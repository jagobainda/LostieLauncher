using LostieLauncher.Models;
using LostieLauncher.Services;
using System.Net;
using System.Text;

namespace LostieLauncher.Tests.Services;

public class TelemetryServiceTests
{
    private readonly HttpClientFactoryStub _httpFactory = new();
    private readonly TelemetryOptions _options = new(ApiKey: "test-key", Endpoint: "https://telemetry.test/");

    private TelemetryService CreateSut() => new(_httpFactory, _options);

    // -------------------- GetDownloadCountsAsync --------------------

    [Fact]
    public async Task GetDownloadCountsAsync_WhenServerReturnsStats_ProjectsToFlatDictionary()
    {
        // Arrange
        var json = """
        { "byGame": { "demo": { "totalEvents": 7 }, "other": { "totalEvents": 3 } } }
        """;
        _httpFactory.HandlerFor("Telemetry").RespondWithJson("stats", json);
        var sut = CreateSut();

        // Act
        var counts = await sut.GetDownloadCountsAsync();

        // Assert
        counts.Count.ShouldBe(2);
        counts["demo"].ShouldBe(7);
        counts["other"].ShouldBe(3);
    }

    [Fact]
    public async Task GetDownloadCountsAsync_WhenByGameIsNull_ReturnsEmptyDictionary()
    {
        // Arrange
        _httpFactory.HandlerFor("Telemetry").RespondWithJson("stats", "{}");
        var sut = CreateSut();

        // Act
        var counts = await sut.GetDownloadCountsAsync();

        // Assert
        counts.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetDownloadCountsAsync_WhenServerReturnsNonSuccess_ReturnsEmptyDictionary()
    {
        // Arrange
        _httpFactory.HandlerFor("Telemetry").RespondWithStatus("stats", HttpStatusCode.InternalServerError);
        var sut = CreateSut();

        // Act
        var counts = await sut.GetDownloadCountsAsync();

        // Assert
        counts.ShouldBeEmpty();
    }

    // -------------------- TrackGameLaunched --------------------

    [Fact]
    public async Task TrackGameLaunched_WhenApiKeyIsBlank_DoesNotCallTelemetryEndpoint()
    {
        // Arrange
        var blankKeyOptions = new TelemetryOptions(ApiKey: " ", Endpoint: "https://telemetry.test/");
        var sut = new TelemetryService(_httpFactory, blankKeyOptions);
        var handler = _httpFactory.HandlerFor("Telemetry");

        // Act
        sut.TrackGameLaunched("demo", "1.0.0");
        await Task.Delay(50);

        // Assert
        handler.Requests.ShouldBeEmpty();
    }

    [Fact]
    public async Task TrackGameLaunched_WhenCalledForFirstTime_PostsToTelemetryEndpoint()
    {
        // Arrange
        _httpFactory.HandlerFor("Telemetry").Respond(_ => new HttpResponseMessage(HttpStatusCode.Accepted));
        var handler = _httpFactory.HandlerFor("Telemetry");
        var sut = CreateSut();

        // Act
        sut.TrackGameLaunched("demo", "1.2.3");
        await WaitForRequestAsync(handler, expected: 1);

        // Assert
        handler.Requests.ShouldHaveSingleItem();
        handler.Requests[0].Method.ShouldBe(HttpMethod.Post);
        handler.Requests[0].Headers.GetValues("x-launcher-key").ShouldContain("test-key");
    }

    [Fact]
    public async Task TrackGameLaunched_WhenCalledTwiceForSameGameWithinCooldown_OnlyPostsOnce()
    {
        // Arrange
        _httpFactory.HandlerFor("Telemetry").Respond(_ => new HttpResponseMessage(HttpStatusCode.Accepted));
        var handler = _httpFactory.HandlerFor("Telemetry");
        var sut = CreateSut();

        // Act
        sut.TrackGameLaunched("demo", "1.2.3");
        await WaitForRequestAsync(handler, expected: 1);
        sut.TrackGameLaunched("demo", "1.2.3");
        await Task.Delay(50);

        // Assert
        handler.Requests.Count.ShouldBe(1);
    }

    [Fact]
    public async Task TrackGameLaunched_WhenServerReturnsError_DoesNotThrow()
    {
        // Arrange — even non-success responses must be swallowed.
        _httpFactory.HandlerFor("Telemetry").Respond(_ => new HttpResponseMessage(HttpStatusCode.BadRequest)
        {
            Content = new StringContent("bad", Encoding.UTF8, "text/plain")
        });
        var handler = _httpFactory.HandlerFor("Telemetry");
        var sut = CreateSut();

        // Act
        var act = () => { sut.TrackGameLaunched("demo", "1.2.3"); return Task.CompletedTask; };

        // Assert
        await act.ShouldNotThrowAsync();
        await WaitForRequestAsync(handler, expected: 1);
    }

    private static async Task WaitForRequestAsync(FakeHttpMessageHandler handler, int expected, int timeoutMs = 2000)
    {
        var deadline = DateTime.UtcNow.AddMilliseconds(timeoutMs);
        while (handler.Requests.Count < expected && DateTime.UtcNow < deadline)
            await Task.Delay(10);
    }
}
