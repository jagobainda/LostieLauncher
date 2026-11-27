using LostieLauncher.Models;
using LostieLauncher.Services;
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

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenServerReturnsValidContent_ReturnsSuccessWithParsedConfig()
    {
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

        var result = await sut.FetchSpecialVersionConfigAsync("ABCD-EFGH-IJKL-MNOP-QRST");

        result.Outcome.ShouldBe(SpecialVersionConfigOutcome.Success);
        result.Config.ShouldNotBeNull();
        result.Config!.Tipo.ShouldBe("beta");
        result.Config.Version.ShouldBe("1.2.3");
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenServerReturns404_ReturnsNotFound()
    {
        _httpFactory.HandlerFor("Content").RespondWithStatus("game.config", HttpStatusCode.NotFound);
        var sut = CreateSut();

        var result = await sut.FetchSpecialVersionConfigAsync("ABCD-EFGH-IJKL-MNOP-QRST");

        result.Outcome.ShouldBe(SpecialVersionConfigOutcome.NotFound);
        result.Config.ShouldBeNull();
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenContentIsUnparsable_ReturnsInvalidResponse()
    {
        _httpFactory.HandlerFor("Content").Respond(_ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent("not a valid config", Encoding.UTF8, "text/plain")
        });
        var sut = CreateSut();

        var result = await sut.FetchSpecialVersionConfigAsync("KEY1");

        result.Outcome.ShouldBe(SpecialVersionConfigOutcome.InvalidResponse);
        result.Config.ShouldBeNull();
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenServerReturnsServerError_ReturnsNetworkError()
    {
        _httpFactory.HandlerFor("Content").RespondWithStatus("game.config", HttpStatusCode.InternalServerError);
        var sut = CreateSut();

        var result = await sut.FetchSpecialVersionConfigAsync("KEY1");

        result.Outcome.ShouldBe(SpecialVersionConfigOutcome.NetworkError);
        result.Config.ShouldBeNull();
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenRequestThrowsNetworkException_ReturnsNetworkError()
    {
        _httpFactory.HandlerFor("Content").Respond(_ => throw new HttpRequestException("network down"));
        var sut = CreateSut();

        var result = await sut.FetchSpecialVersionConfigAsync("KEY1");

        result.Outcome.ShouldBe(SpecialVersionConfigOutcome.NetworkError);
        result.Config.ShouldBeNull();
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenTimeoutFiresWithoutUserCancellation_ReturnsNetworkError()
    {
        _httpFactory.HandlerFor("Content").Respond(_ => throw new TaskCanceledException("timed out"));
        var sut = CreateSut();

        var result = await sut.FetchSpecialVersionConfigAsync("KEY1");

        result.Outcome.ShouldBe(SpecialVersionConfigOutcome.NetworkError);
        result.Config.ShouldBeNull();
    }

    [Fact]
    public async Task FetchSpecialVersionConfigAsync_WhenCancellationAlreadyRequested_ReturnsCancelled()
    {
        var sut = CreateSut();
        using var cts = new CancellationTokenSource();
        cts.Cancel();

        var result = await sut.FetchSpecialVersionConfigAsync("KEY1", cts.Token);

        result.Outcome.ShouldBe(SpecialVersionConfigOutcome.Cancelled);
        result.Config.ShouldBeNull();
    }

    [Fact]
    public async Task DownloadAsync_WhenServerReturnsContent_WritesFileAndReportsSuccess()
    {
        var payload = "hello world";
        _httpFactory.HandlerFor("Download").Respond(_ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent(payload, Encoding.UTF8, "application/octet-stream")
        });
        var dest = Path.Combine(_temp.Path, "out.bin");
        var sut = CreateSut();

        var result = await sut.DownloadAsync("https://download.test/file", dest);

        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.Exists(dest).ShouldBeTrue();
        File.ReadAllText(dest).ShouldBe(payload);
    }

    [Fact]
    public async Task DownloadAsync_WhenCancelled_ReturnsCancelledOutcome()
    {
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

        var result = await sut.DownloadAsync("https://download.test/file", dest, ct: cts.Token);

        result.Outcome.ShouldBe(DownloadOutcome.Cancelled);
    }

    [Fact]
    public async Task DownloadAsync_WhenServerKeepsFailing_RetriesAndReturnsFailedOutcome()
    {
        var attempts = 0;
        _httpFactory.HandlerFor("Download").Respond(_ =>
        {
            attempts++;
            return new HttpResponseMessage(HttpStatusCode.InternalServerError);
        });
        var dest = Path.Combine(_temp.Path, "fail.bin");
        var sut = CreateSut();

        var result = await sut.DownloadAsync("https://download.test/file", dest);

        result.Outcome.ShouldBe(DownloadOutcome.Failed);
        attempts.ShouldBeGreaterThanOrEqualTo(2);
        result.ErrorMessage.ShouldBe("Download failed after maximum retries.");
    }

    [Fact]
    public async Task DownloadAsync_WhenFinalizingFileIsDeniedByPermissions_ReturnsPermissionDeniedOutcome()
    {
        var requests = 0;
        _httpFactory.HandlerFor("Download").Respond(_ =>
        {
            requests++;
            return new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StringContent("payload", Encoding.UTF8, "application/octet-stream")
            };
        });
        var dest = Path.Combine(_temp.Path, "occupied");
        Directory.CreateDirectory(dest);
        var sut = CreateSut();

        var result = await sut.DownloadAsync("https://download.test/file", dest);

        result.Outcome.ShouldBe(DownloadOutcome.PermissionDenied);
        result.ErrorMessage.ShouldBeNull();
        requests.ShouldBe(1);
    }

    [Fact]
    public async Task DownloadAsync_WhenTheDestinationIsBrieflyHeldOpenByAnotherProcess_StillFinalizesTheDownload()
    {
        _httpFactory.HandlerFor("Download").Respond(_ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StringContent("payload", Encoding.UTF8, "application/octet-stream")
        });
        var dest = Path.Combine(_temp.Path, "held-open.bin");
        File.WriteAllText(dest, "previous");
        var holder = new FileStream(dest, FileMode.Open, FileAccess.Read, FileShare.None);
        using var release = new Timer(_ => holder.Dispose(), null, TimeSpan.FromMilliseconds(20), Timeout.InfiniteTimeSpan);
        var sut = new DownloadService(_httpFactory, _options, TimeSpan.FromSeconds(30), TimeSpan.FromMilliseconds(50));

        var result = await sut.DownloadAsync(DownloadUrl, dest);

        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.ReadAllText(dest).ShouldBe("payload");
        File.Exists(dest + ".part").ShouldBeFalse();
    }

    [Fact]
    public async Task DownloadAsync_WhenStreamStallsWithoutUserCancellation_TripsInactivityWatchdogAndFails()
    {
        var attempts = 0;
        _httpFactory.HandlerFor("Download").Respond(_ =>
        {
            attempts++;
            return new HttpResponseMessage(HttpStatusCode.OK) { Content = new StreamContent(new BlockingStream()) };
        });
        var dest = Path.Combine(_temp.Path, "stalled.bin");
        var sut = new DownloadService(_httpFactory, _options, TimeSpan.FromMilliseconds(50));

        var result = await sut.DownloadAsync(DownloadUrl, dest);

        result.Outcome.ShouldBe(DownloadOutcome.Failed);
        attempts.ShouldBeGreaterThanOrEqualTo(2);
    }

    [Fact]
    public async Task DownloadAsync_WhenStreamIsSlowButAlive_RearmsWatchdogPerReadAndCompletes()
    {
        var payload = Repeat('A', 600);
        _httpFactory.HandlerFor("Download").Respond(_ => new HttpResponseMessage(HttpStatusCode.OK)
        {
            Content = new StreamContent(new TrickleStream(payload, chunkSize: 40, delay: TimeSpan.FromMilliseconds(50))),
        });
        var dest = Path.Combine(_temp.Path, "slow.bin");
        var sut = new DownloadService(_httpFactory, _options, TimeSpan.FromMilliseconds(500));

        var result = await sut.DownloadAsync(DownloadUrl, dest);

        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.ReadAllBytes(dest).ShouldBe(payload);
    }

    private const string DownloadUrl = "https://download.test/file";

    private static byte[] Repeat(char c, int count) => Enumerable.Repeat((byte)c, count).ToArray();

    private async Task<string> ArrangePausedDownloadAsync(
        DownloadService sut,
        byte[] chunk,
        Action<HttpResponseMessage>? configureInitial,
        Func<HttpRequestMessage, HttpResponseMessage> onResume)
    {
        var phase = 0;
        var initialStream = new ChunkThenBlockStream(chunk);
        _httpFactory.HandlerFor("Download").Respond(req =>
        {
            phase++;
            if (phase == 1)
            {
                var resp = new HttpResponseMessage(HttpStatusCode.OK)
                {
                    Content = new StreamContent(initialStream),
                };
                configureInitial?.Invoke(resp);
                return resp;
            }

            return onResume(req);
        });

        var dest = Path.Combine(_temp.Path, "game.zip");
        using var cts = new CancellationTokenSource();

        var download = sut.DownloadAsync(DownloadUrl, dest, ct: cts.Token);

        await initialStream.Blocked.Task.WaitAsync(TimeSpan.FromSeconds(30));
        await cts.CancelAsync();

        var paused = await download;

        paused.Outcome.ShouldBe(DownloadOutcome.Cancelled);
        File.Exists(dest + ".part").ShouldBeTrue();
        new FileInfo(dest + ".part").Length.ShouldBe(chunk.Length);

        return dest;
    }

    [Fact]
    public async Task DownloadAsync_WhenResumingUnchangedResource_SendsIfRangeAndAppendsRemainder()
    {
        var chunk = Repeat('A', 100);
        var remainder = Repeat('B', 100);
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

        var result = await sut.DownloadAsync(DownloadUrl, dest);

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

        var result = await sut.DownloadAsync(DownloadUrl, dest);

        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.ReadAllBytes(dest).ShouldBe(newContent);
    }

    [Fact]
    public async Task DownloadAsync_WhenResuming416AndSizeMatches_TreatsPartialAsComplete()
    {
        var chunk = Repeat('A', 100);
        var sut = CreateSut();

        var dest = await ArrangePausedDownloadAsync(
            sut,
            chunk,
            initial => { initial.Headers.ETag = new EntityTagHeaderValue("\"v1\""); initial.Content.Headers.ContentLength = 100; },
            _ => new HttpResponseMessage(HttpStatusCode.RequestedRangeNotSatisfiable));

        var result = await sut.DownloadAsync(DownloadUrl, dest);

        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.ReadAllBytes(dest).ShouldBe(chunk);
        File.Exists(dest + ".part").ShouldBeFalse();
    }

    [Fact]
    public async Task DownloadAsync_WhenResuming416AndSizeMismatch_DiscardsPartialAndRestarts()
    {
        var chunk = Repeat('A', 100);
        var freshContent = Repeat('D', 200);
        var sut = CreateSut();

        var dest = await ArrangePausedDownloadAsync(
            sut,
            chunk,
            initial => { initial.Headers.ETag = new EntityTagHeaderValue("\"v1\""); initial.Content.Headers.ContentLength = 200; },
            req =>
            {
                if (req.Headers.Range is not null)
                    return new HttpResponseMessage(HttpStatusCode.RequestedRangeNotSatisfiable);

                return new HttpResponseMessage(HttpStatusCode.OK) { Content = new ByteArrayContent(freshContent) };
            });

        var result = await sut.DownloadAsync(DownloadUrl, dest);

        result.Outcome.ShouldBe(DownloadOutcome.Success);
        File.ReadAllBytes(dest).ShouldBe(freshContent);
    }

    [Fact]
    public async Task DownloadAsync_WhenResumingWithoutValidator_DoesNotSendRangeAndRestarts()
    {
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

        var result = await sut.DownloadAsync(DownloadUrl, dest);

        result.Outcome.ShouldBe(DownloadOutcome.Success);
        resumeRequest.ShouldNotBeNull();
        resumeRequest!.Headers.Range.ShouldBeNull();
        File.ReadAllBytes(dest).ShouldBe(freshContent);
    }

    private sealed class ChunkThenBlockStream(byte[] chunk) : Stream
    {
        private readonly byte[] _chunk = chunk;
        private bool _sent;

        public TaskCompletionSource Blocked { get; } = new(TaskCreationOptions.RunContinuationsAsynchronously);

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

            Blocked.TrySetResult();
            await Task.Delay(Timeout.Infinite, ct).ConfigureAwait(false);
            return 0;
        }
        public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();

        public override void SetLength(long value) => throw new NotSupportedException();

        public override void Write(byte[] buffer, int offset, int count) => throw new NotSupportedException();
    }

    private sealed class TrickleStream(byte[] data, int chunkSize, TimeSpan delay) : Stream
    {
        private int _pos;
        public override bool CanRead => true;
        public override bool CanSeek => false;
        public override bool CanWrite => false;
        public override long Length => data.Length;
        public override long Position { get => _pos; set => throw new NotSupportedException(); }
        public override void Flush() { }
        public override int Read(byte[] buffer, int offset, int count) => throw new NotSupportedException();

        public override async ValueTask<int> ReadAsync(Memory<byte> buffer, CancellationToken ct = default)
        {
            if (_pos >= data.Length) return 0;
            await Task.Delay(delay, ct).ConfigureAwait(false);
            var n = Math.Min(chunkSize, Math.Min(data.Length - _pos, buffer.Length));
            data.AsMemory(_pos, n).CopyTo(buffer);
            _pos += n;
            return n;
        }
        public override long Seek(long offset, SeekOrigin origin) => throw new NotSupportedException();

        public override void SetLength(long value) => throw new NotSupportedException();

        public override void Write(byte[] buffer, int offset, int count) => throw new NotSupportedException();
    }

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
