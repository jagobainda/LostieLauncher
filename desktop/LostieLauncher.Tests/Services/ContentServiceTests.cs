using LostieLauncher.Models;
using LostieLauncher.Services;

namespace LostieLauncher.Tests.Services;

public class ContentServiceTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("content-service");
    private readonly HttpClientFactoryStub _httpFactory = new();
    private readonly ISettingsService _settings = Substitute.For<ISettingsService>();
    private readonly ContentOptions _options = new(
        ContentEndpoint: "https://content.test/list.json",
        NotificationsEndpoint: "https://content.test/notifications.json",
        FlagEndpoint: "https://content.test/flag.txt");

    public ContentServiceTests()
    {
        _settings.GetGamesRootDirectory().Returns(_temp.Path);
        _settings.Load().Returns(TestData.AppSettings(language: AppLanguage.Esp, downloadDirectory: _temp.Path));
    }

    private ContentService CreateSut() => new(_httpFactory, _options, _settings);

    public void Dispose() => _temp.Dispose();

    [Fact]
    public async Task GetGamesAsync_WhenServerReturnsList_ReturnsDeserialisedGames()
    {
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var json = "[{\"id\":\"11111111-1111-1111-1111-111111111111\",\"nombre\":\"Demo\",\"version\":\"1.0.0\",\"pesoGB\":0.5}]";
        _httpFactory.HandlerFor("Content").RespondWithJson("list.json", json);
        var sut = CreateSut();

        var games = await sut.GetGamesAsync();

        games.ShouldHaveSingleItem();
        games[0].Nombre.ShouldBe("Demo");
    }

    [Fact]
    public async Task GetGamesAsync_WhenMaintenanceFlagIsActive_ReturnsEmptyWithoutCallingContentEndpoint()
    {
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.OK);
        var contentHandler = _httpFactory.HandlerFor("Content");
        var sut = CreateSut();

        var games = await sut.GetGamesAsync();

        games.ShouldBeEmpty();
        contentHandler.Requests.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetGamesAsync_WhenContentRequestFails_ReturnsEmptyList()
    {
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        _httpFactory.HandlerFor("Content").RespondWithStatus("list.json", System.Net.HttpStatusCode.InternalServerError);
        var sut = CreateSut();

        var games = await sut.GetGamesAsync();

        games.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetGamesAsync_WhenAnEntryHasNullNombre_RejectsPayloadInsteadOfReturningACrashingEntry()
    {
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var json = "[{\"id\":\"11111111-1111-1111-1111-111111111111\",\"nombre\":null,\"version\":\"1.0.0\"}]";
        _httpFactory.HandlerFor("Content").RespondWithJson("list.json", json);
        var sut = CreateSut();

        var games = await sut.GetGamesAsync();

        games.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetGamesAsync_WhenAnEntryOmitsNombre_KeepsItWithEmptyName()
    {
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var json = "[{\"id\":\"11111111-1111-1111-1111-111111111111\",\"version\":\"1.0.0\"}]";
        _httpFactory.HandlerFor("Content").RespondWithJson("list.json", json);
        var sut = CreateSut();

        var games = await sut.GetGamesAsync();

        games.ShouldHaveSingleItem();
        games[0].Nombre.ShouldBe(string.Empty);
        games[0].GameId.ShouldBe(string.Empty);
    }

    [Fact]
    public async Task GetGamesAsync_WhenCatalogHasNullArrayElement_SkipsItAndKeepsValidGames()
    {
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var json = "[null,{\"id\":\"11111111-1111-1111-1111-111111111111\",\"nombre\":\"Demo\",\"version\":\"1.0.0\"}]";
        _httpFactory.HandlerFor("Content").RespondWithJson("list.json", json);
        var sut = CreateSut();

        var games = await sut.GetGamesAsync();

        games.ShouldHaveSingleItem();
        games[0].Nombre.ShouldBe("Demo");
    }

    [Fact]
    public async Task IsServerActionBlockedAsync_WhenHeadReturns2xx_ReturnsTrue()
    {
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.OK);
        var sut = CreateSut();

        var blocked = await sut.IsServerActionBlockedAsync(forceRefresh: true);

        blocked.ShouldBeTrue();
    }

    [Fact]
    public async Task IsServerActionBlockedAsync_WhenHeadReturnsNonSuccess_ReturnsFalse()
    {
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var sut = CreateSut();

        var blocked = await sut.IsServerActionBlockedAsync(forceRefresh: true);

        blocked.ShouldBeFalse();
    }

    [Fact]
    public async Task IsServerActionBlockedAsync_WhenHeadIs405_FallsBackToGetForFlagDecision()
    {
        var handler = _httpFactory.HandlerFor("SecurityFlag");
        handler.RespondWithStatus("flag.txt", System.Net.HttpStatusCode.MethodNotAllowed, method: HttpMethod.Head);
        handler.RespondWithStatus("flag.txt", System.Net.HttpStatusCode.OK, method: HttpMethod.Get);
        var sut = CreateSut();

        var blocked = await sut.IsServerActionBlockedAsync(forceRefresh: true);

        blocked.ShouldBeTrue();
        handler.Requests.Count.ShouldBe(2);
    }

    [Fact]
    public async Task IsServerActionBlockedAsync_WhenWithinCacheWindow_DoesNotCallEndpointAgain()
    {
        var handler = _httpFactory.HandlerFor("SecurityFlag")
            .RespondWithStatus("flag.txt", System.Net.HttpStatusCode.OK);
        var sut = CreateSut();
        await sut.IsServerActionBlockedAsync(forceRefresh: true);
        var initialRequests = handler.Requests.Count;

        var second = await sut.IsServerActionBlockedAsync();

        second.ShouldBeTrue();
        handler.Requests.Count.ShouldBe(initialRequests);
    }

    [Fact]
    public async Task IsServerActionBlockedAsync_WhenCalledConcurrently_SharesASingleRequest()
    {
        using var release = new ManualResetEventSlim();
        var handler = _httpFactory.HandlerFor("SecurityFlag").Respond(_ =>
        {
            release.Wait();
            return new HttpResponseMessage(HttpStatusCode.NotFound);
        });
        var sut = CreateSut();

        var first = sut.IsServerActionBlockedAsync(forceRefresh: true);
        var second = sut.IsServerActionBlockedAsync(forceRefresh: true);
        release.Set();
        var results = await Task.WhenAll(first, second);

        results.ShouldAllBe(blocked => !blocked);
        handler.Requests.Count.ShouldBe(1);
    }

    [Theory]
    [InlineData(typeof(HttpRequestException))]
    [InlineData(typeof(TaskCanceledException))]
    [InlineData(typeof(InvalidOperationException))]
    public async Task IsServerActionBlockedAsync_WhenTheCheckFails_FailsOpen(Type exceptionType)
    {
        _httpFactory.HandlerFor("SecurityFlag").Respond(_ => throw (Exception)Activator.CreateInstance(exceptionType)!);
        var sut = CreateSut();

        var blocked = await sut.IsServerActionBlockedAsync(forceRefresh: true);

        blocked.ShouldBeFalse();
    }

    [Fact]
    public void GetGameDirectory_WithSimpleName_ReturnsPathUnderGamesRoot()
    {
        var sut = CreateSut();

        var dir = sut.GetGameDirectory("MyGame");

        dir.ShouldBe(Path.Combine(_temp.Path, "MyGame"));
    }

    [Fact]
    public void GetGameDirectory_WhenNameAttemptsTraversal_ThrowsInvalidOperationException()
    {
        var sut = CreateSut();

        var act = () => sut.GetGameDirectory("..\\..\\Windows");

        Should.Throw<InvalidOperationException>(act);
    }

    [Fact]
    public async Task GetLocalGamesAsync_WhenRegistryFileMissing_ReturnsEmptyList()
    {
        var sut = CreateSut();

        var games = await sut.GetLocalGamesAsync();

        games.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetLocalGamesAsync_WhenRegistryHasDuplicateIds_KeepsOnlyTheFirstOccurrence()
    {
        var json = """
        [
          {"id":"11111111-1111-1111-1111-111111111111","nombre":"Demo","version":"1.0.0"},
          {"id":"11111111-1111-1111-1111-111111111111","nombre":"Demo","version":"9.9.9"}
        ]
        """;
        await File.WriteAllTextAsync(Path.Combine(_temp.Path, "local_games.json"), json);
        var sut = CreateSut();

        var games = await sut.GetLocalGamesAsync();

        games.ShouldHaveSingleItem();
        games[0].Version.ShouldBe("1.0.0");
    }

    [Fact]
    public async Task GetLocalGamesAsync_WhenRegistryHasDuplicateIdlessNames_KeepsOnlyTheFirstOccurrence()
    {
        var json = """
        [
          {"id":"00000000-0000-0000-0000-000000000000","nombre":"Legacy","version":"1.0.0"},
          {"id":"00000000-0000-0000-0000-000000000000","nombre":"legacy","version":"2.0.0"}
        ]
        """;
        await File.WriteAllTextAsync(Path.Combine(_temp.Path, "local_games.json"), json);
        var sut = CreateSut();

        var games = await sut.GetLocalGamesAsync();

        games.ShouldHaveSingleItem();
        games[0].Version.ShouldBe("1.0.0");
    }

    [Fact]
    public async Task GetLocalGamesAsync_WhenIdlessEntryHasNullName_DoesNotThrowAndKeepsValidEntries()
    {
        var json = """
        [
          {"id":"11111111-1111-1111-1111-111111111111","nombre":"Good","version":"1.0.0"},
          {"id":"00000000-0000-0000-0000-000000000000","nombre":null,"version":"2.0.0"}
        ]
        """;
        await File.WriteAllTextAsync(Path.Combine(_temp.Path, "local_games.json"), json);
        var sut = CreateSut();

        var games = await sut.GetLocalGamesAsync();

        games.Count.ShouldBe(2);
        games.ShouldContain(g => g.Nombre == "Good");
    }

    [Fact]
    public async Task GetLocalGamesAsync_WhenEntriesAreDistinct_KeepsThemAll()
    {
        var json = """
        [
          {"id":"11111111-1111-1111-1111-111111111111","nombre":"Alpha","version":"1.0.0"},
          {"id":"22222222-2222-2222-2222-222222222222","nombre":"Bravo","version":"1.0.0"}
        ]
        """;
        await File.WriteAllTextAsync(Path.Combine(_temp.Path, "local_games.json"), json);
        var sut = CreateSut();

        var games = await sut.GetLocalGamesAsync();

        games.Count.ShouldBe(2);
        games.Select(g => g.Nombre).ShouldBe(["Alpha", "Bravo"]);
    }

    [Fact]
    public async Task RegisterGameAsync_WhenCalled_PersistsGameToRegistryFile()
    {
        var sut = CreateSut();
        var id = Guid.NewGuid();

        await sut.RegisterGameAsync(id, "TestGame", "1.0.0");
        var games = await sut.GetLocalGamesAsync();

        games.ShouldHaveSingleItem();
        games[0].Id.ShouldBe(id);
        games[0].Nombre.ShouldBe("TestGame");
        games[0].Version.ShouldBe("1.0.0");
    }

    [Fact]
    public async Task RegisterGameAsync_WhenSameNameAlreadyExists_ReplacesPreviousEntry()
    {
        var sut = CreateSut();
        await sut.RegisterGameAsync(Guid.NewGuid(), "TestGame", "1.0.0");

        await sut.RegisterGameAsync(Guid.NewGuid(), "TestGame", "1.1.0");
        var games = await sut.GetLocalGamesAsync();

        games.ShouldHaveSingleItem();
        games[0].Version.ShouldBe("1.1.0");
    }

    [Fact]
    public async Task RemoveGameRegistryAsync_WhenGameExists_RemovesItFromTheList()
    {
        var sut = CreateSut();
        await sut.RegisterGameAsync(Guid.NewGuid(), "GameA", "1.0.0");
        await sut.RegisterGameAsync(Guid.NewGuid(), "GameB", "1.0.0");

        await sut.RemoveGameRegistryAsync("GameA");
        var games = await sut.GetLocalGamesAsync();

        games.ShouldHaveSingleItem();
        games[0].Nombre.ShouldBe("GameB");
    }

    [Fact]
    public async Task RemoveGameRegistryAsync_WhenRegistryDoesNotExist_DoesNotThrow()
    {
        var sut = CreateSut();

        var act = () => sut.RemoveGameRegistryAsync("Anything");

        await act.ShouldNotThrowAsync();
    }

    [Fact]
    public async Task AddPlaytimeAsync_WhenIdIsEmpty_DoesNothing()
    {
        var sut = CreateSut();

        await sut.AddPlaytimeAsync(Guid.Empty, 30);
        var all = await sut.GetAllPlaytimesAsync();

        all.ShouldBeEmpty();
    }

    [Fact]
    public async Task AddPlaytimeAsync_WhenCalledTwiceForSameId_AccumulatesMinutes()
    {
        var sut = CreateSut();
        var id = Guid.NewGuid();

        await sut.AddPlaytimeAsync(id, 30);
        await sut.AddPlaytimeAsync(id, 15);
        var all = await sut.GetAllPlaytimesAsync();

        all[id].ShouldBe(45);
    }

    [Fact]
    public async Task GetAllPlaytimesAsync_WhenRegistryFileMissing_ReturnsEmptyDictionary()
    {
        var sut = CreateSut();

        var all = await sut.GetAllPlaytimesAsync();

        all.ShouldBeEmpty();
    }

    [Fact]
    public async Task AddPlaytimeAsync_WhenManyConcurrentWritesForSameId_DoesNotLoseUpdates()
    {
        var sut = CreateSut();
        var id = Guid.NewGuid();
        const int writes = 100;

        await Task.WhenAll(Enumerable.Range(0, writes).Select(_ => sut.AddPlaytimeAsync(id, 1)));
        var all = await sut.GetAllPlaytimesAsync();

        all[id].ShouldBe(writes);
    }

    [Fact]
    public async Task RegisterGameAsync_WhenManyConcurrentWritesForDifferentGames_PersistsEveryEntry()
    {
        var sut = CreateSut();
        const int games = 50;

        await Task.WhenAll(Enumerable.Range(0, games)
            .Select(i => sut.RegisterGameAsync(Guid.NewGuid(), $"Game{i}", "1.0.0")));
        var stored = await sut.GetLocalGamesAsync();

        stored.Count.ShouldBe(games);
    }

    [Fact]
    public async Task AddPlaytimeAsync_WhenReadsInterleaveWithWrites_LosesNoWritesAndReadsNeverThrow()
    {
        var sut = CreateSut();
        var id = Guid.NewGuid();
        const int writes = 100;
        var readFailures = 0;

        var writers = Enumerable.Range(0, writes).Select(_ => sut.AddPlaytimeAsync(id, 1));
        var readers = Enumerable.Range(0, writes).Select(async _ =>
        {
            try { await sut.GetAllPlaytimesAsync(); }
            catch { Interlocked.Increment(ref readFailures); }
        });
        await Task.WhenAll(writers.Concat(readers));
        var all = await sut.GetAllPlaytimesAsync();

        all[id].ShouldBe(writes);
        readFailures.ShouldBe(0);
    }

    [Fact]
    public async Task RegisterGameAsync_WritesAtomically_LeavingNoTempFileBehind()
    {
        var sut = CreateSut();

        await sut.RegisterGameAsync(Guid.NewGuid(), "TestGame", "1.0.0");

        File.Exists(Path.Combine(_temp.Path, "local_games.json.tmp")).ShouldBeFalse();
        File.Exists(Path.Combine(_temp.Path, "local_games.json")).ShouldBeTrue();
    }

    [Fact]
    public async Task GetHomeContentAsync_FiltersOutExpiredItems()
    {
        var past = DateTime.Now.AddDays(-1).ToString("yyyy-MM-ddTHH:mm:ss");
        var json = $$"""
        {
          "news": [
            { "id": "11111111-1111-1111-1111-111111111111",
              "title": {"es":"Vieja"}, "description": {"es":"."},
              "tag":"x", "date":"2024-01-01T00:00:00", "expires_at": "{{past}}" },
            { "id": "22222222-2222-2222-2222-222222222222",
              "title": {"es":"Actual"}, "description": {"es":"."},
              "tag":"x", "date":"2024-01-01T00:00:00", "expires_at": null }
          ],
          "notifications": []
        }
        """;
        _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News.ShouldHaveSingleItem();
        content.News[0].Title.ShouldBe("Actual");
    }

    [Fact]
    public void Resolve_WhenRequestedLanguagePresent_ReturnsIt()
    {
        var localized = new Dictionary<string, string> { ["es"] = "Hola", ["en"] = "Hi", ["fr"] = "Salut" };

        var result = ContentService.Resolve(localized, "fr");

        result.ShouldBe("Salut");
    }

    [Fact]
    public void Resolve_WhenRequestedMissingButSpanishPresent_FallsBackToSpanish()
    {
        var localized = new Dictionary<string, string> { ["en"] = "Hi", ["es"] = "Hola" };

        var result = ContentService.Resolve(localized, "ja");

        result.ShouldBe("Hola");
    }

    [Fact]
    public void Resolve_WhenRequestedAndSpanishMissingButEnglishPresent_FallsBackToEnglish()
    {
        var localized = new Dictionary<string, string> { ["fr"] = "Salut", ["en"] = "Hi" };

        var result = ContentService.Resolve(localized, "ja");

        result.ShouldBe("Hi");
    }

    [Fact]
    public void Resolve_WhenNoPreferredLanguage_PicksFirstOrdinalKeyDeterministically()
    {
        var insertionOrderA = new Dictionary<string, string> { ["pt"] = "Olá", ["fr"] = "Salut", ["ca"] = "Hola-ca" };
        var insertionOrderB = new Dictionary<string, string> { ["fr"] = "Salut", ["ca"] = "Hola-ca", ["pt"] = "Olá" };

        var resultA = ContentService.Resolve(insertionOrderA, "ja");
        var resultB = ContentService.Resolve(insertionOrderB, "ja");

        resultA.ShouldBe("Hola-ca");
        resultB.ShouldBe("Hola-ca");
    }

    [Fact]
    public void Resolve_WhenDictionaryEmpty_ReturnsEmptyString()
    {
        var localized = new Dictionary<string, string>();

        var result = ContentService.Resolve(localized, "es");

        result.ShouldBe(string.Empty);
    }

    [Fact]
    public void Resolve_WhenDictionaryIsNull_ReturnsEmptyString()
    {
        var result = ContentService.Resolve(null, "es");

        result.ShouldBe(string.Empty);
    }

    [Fact]
    public void Resolve_WhenRequestedValueIsNull_SkipsItAndFallsBack()
    {
        var localized = new Dictionary<string, string> { ["es"] = null!, ["en"] = "Hi" };

        var result = ContentService.Resolve(localized, "es");

        result.ShouldBe("Hi");
    }

    [Fact]
    public void Resolve_WhenAllValuesNull_ReturnsEmptyString()
    {
        var localized = new Dictionary<string, string> { ["es"] = null!, ["fr"] = null! };

        var result = ContentService.Resolve(localized, "es");

        result.ShouldBe(string.Empty);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenOnlyEnglishAvailable_FallsBackToEnglish()
    {
        _settings.Load().Returns(TestData.AppSettings(language: AppLanguage.Esp, downloadDirectory: _temp.Path));
        var json = """
        {
          "news": [{ "id":"11111111-1111-1111-1111-111111111111",
                     "title": {"fr":"Salut","en":"Hello"}, "description": {"en":"."},
                     "tag":"x", "date":"2024-01-01T00:00:00", "expires_at": null }],
          "notifications": []
        }
        """;
        _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News[0].Title.ShouldBe("Hello");
    }

    [Fact]
    public void NormalizeToCet_WhenUtc_ConvertsToCet()
    {
        var utc = new DateTime(2026, 6, 19, 12, 0, 0, DateTimeKind.Utc);

        var result = ContentService.NormalizeToCet(utc);

        var expected = TimeZoneInfo.ConvertTimeFromUtc(utc, TimeZoneInfo.FindSystemTimeZoneById("Central Europe Standard Time"));
        result.Ticks.ShouldBe(expected.Ticks);
    }

    [Fact]
    public void NormalizeToCet_WhenLocal_ConvertsToCet()
    {
        var local = new DateTime(2026, 1, 15, 12, 0, 0, DateTimeKind.Local);

        var result = ContentService.NormalizeToCet(local);

        var utcEquiv = TimeZoneInfo.ConvertTimeToUtc(local);
        var expected = TimeZoneInfo.ConvertTimeFromUtc(utcEquiv, TimeZoneInfo.FindSystemTimeZoneById("Central Europe Standard Time"));
        result.Ticks.ShouldBe(expected.Ticks);
    }

    [Fact]
    public void NormalizeToCet_WhenUnspecified_ReturnsAsIs()
    {
        var unspecified = new DateTime(2026, 6, 19, 12, 0, 0, DateTimeKind.Unspecified);

        var result = ContentService.NormalizeToCet(unspecified);

        result.Ticks.ShouldBe(unspecified.Ticks);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenExpiresAtIsUtcInPast_FiltersItOut()
    {
        var pastUtc = DateTime.UtcNow.AddDays(-1).ToString("yyyy-MM-ddTHH:mm:ssZ");
        var json = $$"""
        {
          "news": [
            { "id": "11111111-1111-1111-1111-111111111111",
              "title": {"es":"Expired"}, "description": {"es":"."},
              "tag":"x", "date":"2024-01-01T00:00:00", "expires_at": "{{pastUtc}}" }
          ],
          "notifications": []
        }
        """;
        _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenExpiresAtIsUtcInFuture_KeepsIt()
    {
        var futureUtc = DateTime.UtcNow.AddDays(1).ToString("yyyy-MM-ddTHH:mm:ssZ");
        var json = $$"""
        {
          "news": [
            { "id": "11111111-1111-1111-1111-111111111111",
              "title": {"es":"Vigente"}, "description": {"es":"."},
              "tag":"x", "date":"2024-01-01T00:00:00", "expires_at": "{{futureUtc}}" }
          ],
          "notifications": []
        }
        """;
        _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News.ShouldHaveSingleItem();
        content.News[0].Title.ShouldBe("Vigente");
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenLanguageMissing_FallsBackToSpanish()
    {
        _settings.Load().Returns(TestData.AppSettings(language: AppLanguage.Eng, downloadDirectory: _temp.Path));
        var json = """
        {
          "news": [{ "id":"11111111-1111-1111-1111-111111111111",
                     "title": {"es":"Hola"}, "description": {"es":"."},
                     "tag":"x", "date":"2024-01-01T00:00:00", "expires_at": null }],
          "notifications": []
        }
        """;
        _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News[0].Title.ShouldBe("Hola");
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenCalledTwiceWithoutForceRefresh_FetchesOnlyOnce()
    {
        var json = """{ "news": [], "notifications": [] }""";
        var handler = _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        await sut.GetHomeContentAsync();
        await sut.GetHomeContentAsync();

        handler.Requests.Count.ShouldBe(1);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenForceRefresh_BypassesCacheAndRefetches()
    {
        var json = """{ "news": [], "notifications": [] }""";
        var handler = _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();
        await sut.GetHomeContentAsync();

        await sut.GetHomeContentAsync(forceRefresh: true);

        handler.Requests.Count.ShouldBe(2);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenManyConcurrentCallersOnColdCache_FetchesOnlyOnce()
    {
        var json = """{ "news": [], "notifications": [] }""";
        var handler = _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        await Task.WhenAll(Enumerable.Range(0, 50).Select(_ => sut.GetHomeContentAsync()));

        handler.Requests.Count.ShouldBe(1);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenLocalizedValueIsNull_ResolvesToEmptyStringWithoutCrashing()
    {
        var json = """
        {
          "news": [{ "id":"11111111-1111-1111-1111-111111111111",
                     "title": {"es": null}, "description": {"es":"."},
                     "tag":"x", "date":"2024-01-01T00:00:00", "expires_at": null }],
          "notifications": []
        }
        """;
        _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News.ShouldHaveSingleItem();
        content.News[0].Title.ShouldBe(string.Empty);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenNewsHasNullArrayElement_SkipsIt()
    {
        var json = """
        {
          "news": [
            null,
            { "id":"22222222-2222-2222-2222-222222222222",
              "title": {"es":"Vigente"}, "description": {"es":"."},
              "tag":"x", "date":"2024-01-01T00:00:00", "expires_at": null }
          ],
          "notifications": []
        }
        """;
        _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News.ShouldHaveSingleItem();
        content.News[0].Title.ShouldBe("Vigente");
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenTheFirstRequestFailsWithNothingCached_DegradesToAnEmptyStateMarkedStale()
    {
        var sut = CreateSut();

        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News.ShouldBeEmpty();
        content.Notifications.ShouldBeEmpty();
        content.IsStale.ShouldBeTrue();
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenARefreshFails_KeepsThePreviouslyLoadedContent()
    {
        var failing = false;
        RespondWithSingleNewsItem(() => failing, () => "Vigente");
        var sut = CreateSut();
        await sut.GetHomeContentAsync(forceRefresh: true);

        failing = true;
        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News.ShouldHaveSingleItem();
        content.News[0].Title.ShouldBe("Vigente");
        content.IsStale.ShouldBeTrue();
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenAnUnforcedReloadFollowsAFailedRefresh_KeepsTheStaleFlag()
    {
        var failing = false;
        RespondWithSingleNewsItem(() => failing, () => "Vigente");
        var sut = CreateSut();
        await sut.GetHomeContentAsync(forceRefresh: true);
        failing = true;
        await sut.GetHomeContentAsync(forceRefresh: true);

        var content = await sut.GetHomeContentAsync();

        content.News.ShouldHaveSingleItem();
        content.IsStale.ShouldBeTrue();
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenARefreshFailsAndThenSucceeds_ReturnsTheFreshContentUnflagged()
    {
        var failing = false;
        var title = "Vigente";
        RespondWithSingleNewsItem(() => failing, () => title);
        var sut = CreateSut();
        await sut.GetHomeContentAsync(forceRefresh: true);

        failing = true;
        await sut.GetHomeContentAsync(forceRefresh: true);

        failing = false;
        title = "Actualizada";
        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        content.News.ShouldHaveSingleItem();
        content.News[0].Title.ShouldBe("Actualizada");
        content.IsStale.ShouldBeFalse();
    }

    private void RespondWithSingleNewsItem(Func<bool> isFailing, Func<string> title) =>
        _httpFactory.HandlerFor("Content").Respond(req =>
        {
            if (!req.RequestUri!.ToString().Contains("notifications.json", StringComparison.OrdinalIgnoreCase)) return null;
            if (isFailing()) throw new HttpRequestException("Unknown host.");

            var json = $$"""
            {
              "news": [
                { "id": "11111111-1111-1111-1111-111111111111",
                  "title": {"es":"{{title()}}"}, "description": {"es":"."},
                  "tag":"x", "date":"2024-01-01T00:00:00", "expires_at": null }
              ],
              "notifications": []
            }
            """;
            return new HttpResponseMessage(HttpStatusCode.OK)
            {
                Content = new StringContent(json, System.Text.Encoding.UTF8, "application/json")
            };
        });
}
