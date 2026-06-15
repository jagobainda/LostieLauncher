using LostieLauncher.Models;
using LostieLauncher.Services;
using System.Net;
using System.Net.Http.Headers;
using System.Text;

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

    // -------------------- DownloadAsync: resume identity validation (BUG-002) --------------------

    private const string DownloadUrl = "https://download.test/file";

    private static byte[] Repeat(char c, int count) => Enumerable.Repeat((byte)c, count).ToArray();

    /// <summary>
    /// Drives a first download that writes <paramref name="chunk"/> and then blocks, cancelling it
    /// so a partial <c>.part</c> (and its <c>.part.meta</c> sidecar) is left on disk — the precondition
    /// for every resume test. The handler is left registered with a phase counter so the resume request
    /// is served by <paramref name="onResume"/>.
    /// </summary>
    private async Task<string> ArrangePausedDownloadAsync(
        DownloadService sut,
        byte[] chunk,
        Action<HttpResponseMessage>? configureInitial,
        Func<HttpRequestMessage, HttpResponseMessage> onResume)
    {
        var phase = 0;
        _httpFactory.HandlerFor("Download").Respond(req =>
        {
            phase++;
            if (phase == 1)
            {
                var resp = new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new StreamContent(new ChunkThenBlockStream(chunk)),
                };
                configureInitial?.Invoke(resp);
                return resp;
            }

            return onResume(req);
        });

        var dest = Path.Combine(_temp.Path, "game.zip");
        using var cts = new CancellationTokenSource();
        cts.CancelAfter(TimeSpan.FromMilliseconds(150));

        var paused = await sut.DownloadAsync(DownloadUrl, dest, ct: cts.Token);

        // Sanity: the first phase must have paused, leaving a partial file behind.
        paused.Outcome.ShouldBe(DownloadOutcome.Cancelled);
        File.Exists(dest + ".part").ShouldBeTrue();
        new FileInfo(dest + ".part").Length.ShouldBe(chunk.Length);

        return dest;
    }

    [Fact]
    public async Task DownloadAsync_WhenResumingUnchangedResource_SendsIfRangeAndAppendsRemainder()
    {
        // Arrange
        var chunk = Repeat('A', 100);          // bytes already on disk
        var remainder = Repeat('B', 100);      // bytes the server still owes us
        var sut = CreateSut();
        HttpRequestMessage? resumeRequest = null;

        var dest = await ArrangePausedDownloadAsync(
            sut,
            chunk,
            initial => { initial.Headers.ETag = new EntityTagHeaderValue("\"v1\""); initial.Content.Headers.ContentLength = 200; },
            req =>
            {
                resumeRequest = req;
                var resp = new HttpResponseMessage(HttpStatusCode.PartialContent)
                {
                    Content = new ByteArrayContent(remainder),
                };
                resp.Content.Headers.ContentRange = new ContentRangeHeaderValue(100, 199, 200);
                return resp;
            });

        // Act
        var result = await sut.DownloadAsync(DownloadUrl, dest);

        // Assert — resume must carry both Range and a validating If-Range, and the file is the exact concatenation.
        result.Outcome.ShouldBe(DownloadOutcome.Success);
        resumeRequest.ShouldNotBeNull();
        resumeRequest!.Headers.Range.ShouldNotBeNull();
        resumeRequest.Headers.IfRange.ShouldNotBeNull();
        File.ReadAllBytes(dest).ShouldBe([.. chunk, .. remainder]);
        File.Exists(dest + ".part").ShouldBeFalse();
        File.Exists(dest + ".part.meta").ShouldBeFalse();
    }

    [Fact]
    public async Task DownloadAsync_WhenResumingChangedResource_ServerReturns200_RewritesWithoutCorruption()
    {
        // Arrange — server now serves a *different* file and ignores the stale validator (responds 200, not 206).
        var chunk = Repeat('A', 100);
        var newContent = Repeat('C', 150);
        var sut = CreateSut();

        var dest = await ArrangePausedDownloadAsync(
            sut,
            chunk,
            initial => { initial.Headers.ETag = new EntityTagHeaderValue("\"v1\""); initial.Content.Headers.ContentLength = 200; },
            _ =>
            {
                var resp = new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new ByteArrayContent(newContent),
                };
                resp.Headers.ETag = new EntityTagHeaderValue("\"v2\"");
                return resp;
            });

        // Act
        var result = await sut.DownloadAsync(DownloadUrl, dest);

        // Assert — the stale 100 'A' bytes must be discarded, NOT prepended to the new content.
        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.ReadAllBytes(dest).ShouldBe(newContent);
    }

    [Fact]
    public async Task DownloadAsync_WhenResuming416AndSizeMatches_TreatsPartialAsComplete()
    {
        // Arrange — the partial already holds the full resource (ContentLength == chunk length).
        var chunk = Repeat('A', 100);
        var sut = CreateSut();

        var dest = await ArrangePausedDownloadAsync(
            sut,
            chunk,
            initial => { initial.Headers.ETag = new EntityTagHeaderValue("\"v1\""); initial.Content.Headers.ContentLength = 100; },
            _ => new HttpResponseMessage(HttpStatusCode.RequestedRangeNotSatisfiable));

        // Act
        var result = await sut.DownloadAsync(DownloadUrl, dest);

        // Assert
        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.ReadAllBytes(dest).ShouldBe(chunk);
        File.Exists(dest + ".part").ShouldBeFalse();
    }

    [Fact]
    public async Task DownloadAsync_WhenResuming416AndSizeMismatch_DiscardsPartialAndRestarts()
    {
        // Arrange — the partial (100 B) is smaller than the expected total (200 B), so a 416 means it is corrupt.
        var chunk = Repeat('A', 100);
        var freshContent = Repeat('D', 200);
        var sut = CreateSut();

        var dest = await ArrangePausedDownloadAsync(
            sut,
            chunk,
            initial => { initial.Headers.ETag = new EntityTagHeaderValue("\"v1\""); initial.Content.Headers.ContentLength = 200; },
            req =>
            {
                // First resume attempt: 416 (size mismatch → discard + retry). Retry (no Range): serve full fresh content.
                if (req.Headers.Range is not null)
                    return new HttpResponseMessage(HttpStatusCode.RequestedRangeNotSatisfiable);

                return new HttpResponseMessage(HttpStatusCode.OK) { Content = new ByteArrayContent(freshContent) };
            });

        // Act
        var result = await sut.DownloadAsync(DownloadUrl, dest);

        // Assert — the corrupt partial is gone and the file is the clean re-download.
        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.ReadAllBytes(dest).ShouldBe(freshContent);
    }

    [Fact]
    public async Task DownloadAsync_WhenResumingWithoutValidator_DoesNotSendRangeAndRestarts()
    {
        // Arrange — initial response carries neither ETag nor Last-Modified, so the partial cannot be validated.
        var chunk = Repeat('A', 100);
        var freshContent = Repeat('E', 120);
        var sut = CreateSut();
        HttpRequestMessage? resumeRequest = null;

        var dest = await ArrangePausedDownloadAsync(
            sut,
            chunk,
            configureInitial: null,
            req =>
            {
                resumeRequest = req;
                return new HttpResponseMessage(HttpStatusCode.OK) { Content = new ByteArrayContent(freshContent) };
            });

        // Act
        var result = await sut.DownloadAsync(DownloadUrl, dest);

        // Assert — without a validator we must NOT blindly resume: no Range header, full re-download.
        result.Outcome.ShouldBe(DownloadOutcome.Success);
        resumeRequest.ShouldNotBeNull();
        resumeRequest!.Headers.Range.ShouldBeNull();
        File.ReadAllBytes(dest).ShouldBe(freshContent);
    }

    /// <summary>Stream that yields a single chunk on the first read, then blocks until cancelled.</summary>
    private sealed class ChunkThenBlockStream(byte[] chunk) : Stream
    {
        private readonly byte[] _chunk = chunk;
        private bool _sent;
        public override bool CanRead => true;
        public override bool CanSeek => false;
        public override bool CanWrite => false;
        public override long Length => throw new NotSupportedException();
        public override long Position { get => 0; set => throw new NotSupportedException(); }
        public override void Flush() { }
        public override int Read(byte[] buffer, int offset, int count) => throw new NotSupportedException();

        public override async ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken ct = default)
        {
            if (!_sent)
            {
                _sent = true;
                var n = Math.Min(_chunk.Length, buffer.Length);
                _chunk.AsMemory(0, n).CopyTo(buffer);
                return n;
            }
            await Task.Delay(Timeout.Infinite, ct).ConfigureAwait(false);
            return 0;
        }
        public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();

        public override void SetLength(long value) => throw new NotSupportedException();

        public override void Write(byte[] buffer, int offset, int count) => throw new NotSupportedException();
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
