using LostieLauncher.Models;
using LostieLauncher.Services;
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

    // -------------------- NormalizeVersion --------------------

    [Theory]
    [InlineData("1.2.3", "1.2.3")]   // canonical three-part version is preserved
    [InlineData("1.2", "1.2.0")]     // missing patch defaults to 0
    [InlineData("1.2.3.4", "1.2.3")] // revision is dropped
    [InlineData("v1.2.3", "1.2.3")]  // leading 'v' is stripped (consistent with VersionUtils)
    [InlineData("1.2.3-beta", "1.2.3")] // pre-release suffix is trimmed, base kept
    public void NormalizeVersion_WhenVersionIsNumeric_ReturnsCanonicalThreePartVersion(string input, string expected)
    {
        // Act
        var result = TelemetryService.NormalizeVersion(input);

        // Assert
        result.ShouldBe(expected);
    }

    [Theory]
    [InlineData("alpha.beta.gamma")] // the BUG-043 case: non-numeric parts
    [InlineData("not-a-version")]
    [InlineData("1")]                // single component is not a parseable Version
    [InlineData("")]
    [InlineData("   ")]
    public void NormalizeVersion_WhenVersionIsNotParseable_ReturnsZeroVersion(string input)
    {
        // Act
        var result = TelemetryService.NormalizeVersion(input);

        // Assert
        result.ShouldBe("0.0.0");
    }

    // -------------------- FormatGpuNames --------------------

    [Fact]
    public void FormatGpuNames_WhenMultipleAdapters_JoinsAllNamesInOrder()
    {
        // Arrange — the BUG-065 scenario: an iGPU at "0000" and a dGPU at "0001".
        string?[] descriptions = ["Intel(R) UHD Graphics", "NVIDIA GeForce RTX 4060"];

        // Act
        var result = TelemetryService.FormatGpuNames(descriptions);

        // Assert — both GPUs are reported, not just the first adapter.
        result.ShouldBe("Intel(R) UHD Graphics + NVIDIA GeForce RTX 4060");
    }

    [Fact]
    public void FormatGpuNames_WhenSingleAdapter_ReturnsNameWithoutSeparator()
    {
        // Arrange
        string?[] descriptions = ["NVIDIA GeForce RTX 4060"];

        // Act
        var result = TelemetryService.FormatGpuNames(descriptions);

        // Assert
        result.ShouldBe("NVIDIA GeForce RTX 4060");
    }

    [Fact]
    public void FormatGpuNames_WhenNamesAreDuplicatedCaseInsensitively_Deduplicates()
    {
        // Arrange — mirrored / repeated adapter entries are common in the registry.
        string?[] descriptions = ["NVIDIA GeForce RTX 4060", "nvidia geforce rtx 4060", "Intel(R) UHD Graphics"];

        // Act
        var result = TelemetryService.FormatGpuNames(descriptions);

        // Assert — first occurrence is kept; the case-insensitive duplicate is dropped.
        result.ShouldBe("NVIDIA GeForce RTX 4060 + Intel(R) UHD Graphics");
    }

    [Fact]
    public void FormatGpuNames_WhenSomeEntriesAreNullOrBlank_FiltersThemOut()
    {
        // Arrange — adapter subkeys without a DriverDesc read back as null.
        string?[] descriptions = [null, "  ", "  NVIDIA GeForce RTX 4060  ", ""];

        // Act
        var result = TelemetryService.FormatGpuNames(descriptions);

        // Assert — null/blank entries are dropped and the surviving name is trimmed.
        result.ShouldBe("NVIDIA GeForce RTX 4060");
    }

    [Fact]
    public void FormatGpuNames_WhenNoUsableNames_ReturnsEmptyString()
    {
        // Arrange
        string?[] descriptions = [null, "", "   "];

        // Act
        var result = TelemetryService.FormatGpuNames(descriptions);

        // Assert — empty signals "Unknown" to the caller.
        result.ShouldBeEmpty();
    }

    [Fact]
    public void FormatGpuNames_WhenJoinedNamesExceedLimit_TruncatesToBoundedLength()
    {
        // Arrange — many phantom adapters must not produce an unbounded payload field.
        var descriptions = Enumerable.Range(0, 20).Select(i => $"Adapter Model Number {i:D2}").Cast<string?>().ToArray();

        // Act
        var result = TelemetryService.FormatGpuNames(descriptions);

        // Assert
        result.Length.ShouldBe(128);
    }

    private static async Task WaitForRequestAsync(FakeHttpMessageHandler handler, int expected, int timeoutMs = 2000)
    {
        var deadline = DateTime.UtcNow.AddMilliseconds(timeoutMs);
        while (handler.Requests.Count < expected && DateTime.UtcNow < deadline)
            await Task.Delay(10);
    }
}
