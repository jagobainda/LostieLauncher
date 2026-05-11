using System.Net;
using System.Text;
using LostieLauncher.Models;
using LostieLauncher.Services;

namespace LostieLauncher.Tests.Services;

public class DownloadServiceTests : IDisposable
{
    private const string ValidGuid = "11111111-2222-3333-4444-555555555555";

    private readonly TempDirectoryFixture _temp = new("download-service");
    private readonly HttpClientFactoryStub _httpFactory = new();
    private readonly DownloadOptions _options = new(BaseUrl: "https://download.test");

    private DownloadService CreateSut() => new(_httpFactory, _options);

    public void Dispose() => _temp.Dispose();

    private static string ValidConfigContent() => string.Join('\n',
        "sha256=abc",
        "tipo=beta",
        $"juego-principal={ValidGuid}",
        "vers=1.2.3",
        "archivo=game.zip");

    // -------------------- FetchSpecialVersionConfigAsync --------------------

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenServerReturnsValidContent_ReturnsParsedConfig()
    {
        // Arrange
        _httpFactory.HandlerFor("Content").Respond(req =>
        {
            if (req.RequestUri!.ToString().EndsWith("game.config", StringComparison.Ordinal))
                return new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new StringContent(ValidConfigContent(), Encoding.UTF8, "text/plain")
                };
            return null;
        });
        var sut = CreateSut();

        // Act
        var config = await sut.FetchSpecialVersionConfigAsync("ABCD-EFGH-IJKL-MNOP-QRST");

        // Assert
        config.ShouldNotBeNull();
        config!.Tipo.ShouldBe("beta");
        config.Version.ShouldBe("1.2.3");
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenServerReturns404_ReturnsNull()
    {
        // Arrange
        _httpFactory.HandlerFor("Content").RespondWithStatus("game.config", HttpStatusCode.NotFound);
        var sut = CreateSut();

        // Act
        var config = await sut.FetchSpecialVersionConfigAsync("ABCD-EFGH-IJKL-MNOP-QRST");

        // Assert
        config.ShouldBeNull();
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenContentIsUnparsable_ReturnsNull()
    {
        // Arrange
        _httpFactory.HandlerFor("Content").Respond(_ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent("not a valid config", Encoding.UTF8, "text/plain")
        });
        var sut = CreateSut();

        // Act
        var config = await sut.FetchSpecialVersionConfigAsync("KEY1");

        // Assert
        config.ShouldBeNull();
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenServerReturnsServerError_ReturnsNull()
    {
        // Arrange
        _httpFactory.HandlerFor("Content").RespondWithStatus("game.config", HttpStatusCode.InternalServerError);
        var sut = CreateSut();

        // Act
        var config = await sut.FetchSpecialVersionConfigAsync("KEY1");

        // Assert
        config.ShouldBeNull();
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenCancellationAlreadyRequested_ReturnsNull()
    {
        // Arrange
        var sut = CreateSut();
        using var cts = new CancellationTokenSource();
        cts.Cancel();

        // Act
        var config = await sut.FetchSpecialVersionConfigAsync("KEY1", cts.Token);

        // Assert
        config.ShouldBeNull();
    }

    // -------------------- DownloadAsync --------------------

    [Fact]
    public async Task DownloadAsync_WhenServerReturnsContent_WritesFileAndReportsSuccess()
    {
        // Arrange
        var payload = "hello world";
        _httpFactory.HandlerFor("Download").Respond(_ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(payload, Encoding.UTF8, "application/octet-stream")
        });
        var dest = Path.Combine(_temp.Path, "out.bin");
        var sut = CreateSut();

        // Act
        var result = await sut.DownloadAsync("https://download.test/file", dest);

        // Assert
        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.Exists(dest).ShouldBeTrue();
        File.ReadAllText(dest).ShouldBe(payload);
        sut.State.ShouldBe(DownloadState.Completed);
    }

    [Fact]
    public async Task DownloadAsync_WhenCancelled_ReturnsCancelledOutcomeAndPausesState()
    {
        // Arrange — return a stalled stream so cancellation can fire mid-read.
        _httpFactory.HandlerFor("Download").Respond(_ =>
        {
            var stream = new BlockingStream();
            return new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StreamContent(stream),
            };
        });
        var dest = Path.Combine(_temp.Path, "cancelled.bin");
        var sut = CreateSut();
        using var cts = new CancellationTokenSource();
        cts.CancelAfter(TimeSpan.FromMilliseconds(50));

        // Act
        var result = await sut.DownloadAsync("https://download.test/file", dest, ct: cts.Token);

        // Assert
        result.Outcome.ShouldBe(DownloadOutcome.Cancelled);
        sut.State.ShouldBe(DownloadState.Paused);
    }

    [Fact]
    public async Task DownloadAsync_WhenServerKeepsFailing_RetriesAndReturnsFailedOutcome()
    {
        // Arrange — every Download attempt returns an HTTP-protocol failure.
        var attempts = 0;
        _httpFactory.HandlerFor("Download").Respond(_ =>
        {
            attempts++;
            // 500 + EnsureSuccessStatusCode triggers HttpRequestException, which is the retry path.
            return new HttpResponseMessage(HttpStatusCode.InternalServerError);
        });
        var dest = Path.Combine(_temp.Path, "fail.bin");
        var sut = CreateSut();

        // Act
        var result = await sut.DownloadAsync("https://download.test/file", dest);

        // Assert
        result.Outcome.ShouldBe(DownloadOutcome.Failed);
        attempts.ShouldBeGreaterThanOrEqualTo(2);
        sut.State.ShouldBe(DownloadState.Failed);
    }

    /// <summary>Stream that blocks indefinitely on read until the cancellation token cancels.</summary>
    private sealed class BlockingStream : Stream
    {
        public override bool CanRead => true;
        public override bool CanSeek => false;
        public override bool CanWrite => false;
        public override long Length => long.MaxValue;
        public override long Position { get => 0; set => throw new NotSupportedException(); }
        public override void Flush() { }
        public override int Read(byte[] buffer, int offset, int count) { Thread.Sleep(Timeout.Infinite); return 0; }
        public override async ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken ct = default)
        {
            await Task.Delay(Timeout.Infinite, ct).ConfigureAwait(false);
            return 0;
        }
        public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();
        public override void SetLength(long value) => throw new NotSupportedException();
        public override void Write(byte[] buffer, int offset, int count) => throw new NotSupportedException();
    }
}
