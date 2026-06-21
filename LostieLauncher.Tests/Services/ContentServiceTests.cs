using LostieLauncher.Models;
using LostieLauncher.Services;
using System.IO;
using System.Text.Json;

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
        // Default: route the games-root lookup to a writable temp directory.
        _settings.GetGamesRootDirectory().Returns(_temp.Path);
        _settings.Load().Returns(TestData.AppSettings(language: AppLanguage.Esp, downloadDirectory: _temp.Path));
    }

    private ContentService CreateSut() => new(_httpFactory, _options, _settings);

    public void Dispose() => _temp.Dispose();

    // -------------------- GetGamesAsync --------------------

    [Fact]
    public async Task GetGamesAsync_WhenServerReturnsList_ReturnsDeserialisedGames()
    {
        // Arrange
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var json = "[{\"id\":\"11111111-1111-1111-1111-111111111111\",\"nombre\":\"Demo\",\"version\":\"1.0.0\",\"pesoGB\":0.5}]";
        _httpFactory.HandlerFor("Content").RespondWithJson("list.json", json);
        var sut = CreateSut();

        // Act
        var games = await sut.GetGamesAsync();

        // Assert
        games.ShouldHaveSingleItem();
        games[0].Nombre.ShouldBe("Demo");
    }

    [Fact]
    public async Task GetGamesAsync_WhenMaintenanceFlagIsActive_ReturnsEmptyWithoutCallingContentEndpoint()
    {
        // Arrange — flag endpoint returns 200 (blocked).
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.OK);
        var contentHandler = _httpFactory.HandlerFor("Content");
        var sut = CreateSut();

        // Act
        var games = await sut.GetGamesAsync();

        // Assert
        games.ShouldBeEmpty();
        contentHandler.Requests.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetGamesAsync_WhenContentRequestFails_ReturnsEmptyList()
    {
        // Arrange
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        _httpFactory.HandlerFor("Content").RespondWithStatus("list.json", System.Net.HttpStatusCode.InternalServerError);
        var sut = CreateSut();

        // Act
        var games = await sut.GetGamesAsync();

        // Assert
        games.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetGamesAsync_WhenAnEntryHasNullNombre_RejectsPayloadInsteadOfReturningACrashingEntry()
    {
        // Arrange — a remote catalog with an explicit "nombre": null (BUG-053). Before the fix,
        // System.Text.Json assigned null to the non-nullable Nombre and the resulting GameInfo
        // NRE'd later in the UI on GameId. With RespectNullableAnnotations the strict remote
        // profile throws JsonException, which the service catches and logs → empty (fail-safe),
        // never a crashing entry.
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var json = "[{\"id\":\"11111111-1111-1111-1111-111111111111\",\"nombre\":null,\"version\":\"1.0.0\"}]";
        _httpFactory.HandlerFor("Content").RespondWithJson("list.json", json);
        var sut = CreateSut();

        // Act
        var games = await sut.GetGamesAsync();

        // Assert
        games.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetGamesAsync_WhenAnEntryOmitsNombre_KeepsItWithEmptyName()
    {
        // Arrange — a *missing* property is distinct from an explicit null: strict nullability
        // only rejects explicit nulls, so an omitted "nombre" keeps the model default
        // (string.Empty) and the catalog still loads.
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var json = "[{\"id\":\"11111111-1111-1111-1111-111111111111\",\"version\":\"1.0.0\"}]";
        _httpFactory.HandlerFor("Content").RespondWithJson("list.json", json);
        var sut = CreateSut();

        // Act
        var games = await sut.GetGamesAsync();

        // Assert
        games.ShouldHaveSingleItem();
        games[0].Nombre.ShouldBe(string.Empty);
        games[0].GameId.ShouldBe(string.Empty);
    }

    [Fact]
    public async Task GetGamesAsync_WhenCatalogHasNullArrayElement_SkipsItAndKeepsValidGames()
    {
        // Arrange — strict nullability doesn't reach collection elements, so a stray null entry
        // in the array survives deserialization; the explicit null-element filter drops it before
        // it can NRE in the UI.
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var json = "[null,{\"id\":\"11111111-1111-1111-1111-111111111111\",\"nombre\":\"Demo\",\"version\":\"1.0.0\"}]";
        _httpFactory.HandlerFor("Content").RespondWithJson("list.json", json);
        var sut = CreateSut();

        // Act
        var games = await sut.GetGamesAsync();

        // Assert
        games.ShouldHaveSingleItem();
        games[0].Nombre.ShouldBe("Demo");
    }

    // -------------------- IsServerActionBlockedAsync --------------------

    [Fact]
    public async Task IsServerActionBlockedAsync_WhenHeadReturns2xx_ReturnsTrue()
    {
        // Arrange
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.OK);
        var sut = CreateSut();

        // Act
        var blocked = await sut.IsServerActionBlockedAsync(forceRefresh: true);

        // Assert
        blocked.ShouldBeTrue();
    }

    [Fact]
    public async Task IsServerActionBlockedAsync_WhenHeadReturnsNonSuccess_ReturnsFalse()
    {
        // Arrange
        _httpFactory.HandlerFor("SecurityFlag").RespondWithStatus("flag.txt", System.Net.HttpStatusCode.NotFound);
        var sut = CreateSut();

        // Act
        var blocked = await sut.IsServerActionBlockedAsync(forceRefresh: true);

        // Assert
        blocked.ShouldBeFalse();
    }

    [Fact]
    public async Task IsServerActionBlockedAsync_WhenHeadIs405_FallsBackToGetForFlagDecision()
    {
        // Arrange — HEAD => 405, GET => 200 means blocked.
        var handler = _httpFactory.HandlerFor("SecurityFlag");
        handler.RespondWithStatus("flag.txt", System.Net.HttpStatusCode.MethodNotAllowed, method: HttpMethod.Head);
        handler.RespondWithStatus("flag.txt", System.Net.HttpStatusCode.OK, method: HttpMethod.Get);
        var sut = CreateSut();

        // Act
        var blocked = await sut.IsServerActionBlockedAsync(forceRefresh: true);

        // Assert
        blocked.ShouldBeTrue();
        handler.Requests.Count.ShouldBe(2);
    }

    [Fact]
    public async Task IsServerActionBlockedAsync_WhenWithinCacheWindow_DoesNotCallEndpointAgain()
    {
        // Arrange
        var handler = _httpFactory.HandlerFor("SecurityFlag")
            .RespondWithStatus("flag.txt", System.Net.HttpStatusCode.OK);
        var sut = CreateSut();
        await sut.IsServerActionBlockedAsync(forceRefresh: true);
        var initialRequests = handler.Requests.Count;

        // Act
        var second = await sut.IsServerActionBlockedAsync();

        // Assert
        second.ShouldBeTrue();
        handler.Requests.Count.ShouldBe(initialRequests);
    }

    // -------------------- GetGameDirectory --------------------

    [Fact]
    public void GetGameDirectory_WithSimpleName_ReturnsPathUnderGamesRoot()
    {
        // Arrange
        var sut = CreateSut();

        // Act
        var dir = sut.GetGameDirectory("MyGame");

        // Assert
        dir.ShouldBe(Path.Combine(_temp.Path, "MyGame"));
    }

    [Fact]
    public void GetGameDirectory_WhenNameAttemptsTraversal_ThrowsInvalidOperationException()
    {
        // Arrange
        var sut = CreateSut();

        // Act
        var act = () => sut.GetGameDirectory("..\\..\\Windows");

        // Assert
        Should.Throw<InvalidOperationException>(act);
    }

    // -------------------- Local games registry --------------------

    [Fact]
    public async Task GetLocalGamesAsync_WhenRegistryFileMissing_ReturnsEmptyList()
    {
        // Arrange — fresh temp dir means no local_games.json yet.
        var sut = CreateSut();

        // Act
        var games = await sut.GetLocalGamesAsync();

        // Assert
        games.ShouldBeEmpty();
    }

    [Fact]
    public async Task GetLocalGamesAsync_WhenRegistryHasDuplicateIds_KeepsOnlyTheFirstOccurrence()
    {
        // Arrange — a hand-edited registry with the same id twice. Upstream this made
        // ToDictionary(g => g.Id) throw and emptied the whole library (BUG-009).
        var json = """
        [
          {"id":"11111111-1111-1111-1111-111111111111","nombre":"Demo","version":"1.0.0"},
          {"id":"11111111-1111-1111-1111-111111111111","nombre":"Demo","version":"9.9.9"}
        ]
        """;
        await File.WriteAllTextAsync(Path.Combine(_temp.Path, "local_games.json"), json);
        var sut = CreateSut();

        // Act
        var games = await sut.GetLocalGamesAsync();

        // Assert — the duplicate is dropped (first wins) and the list is safe to index by id.
        games.ShouldHaveSingleItem();
        games[0].Version.ShouldBe("1.0.0");
    }

    [Fact]
    public async Task GetLocalGamesAsync_WhenRegistryHasDuplicateIdlessNames_KeepsOnlyTheFirstOccurrence()
    {
        // Arrange — legacy id-less entries indexed by name (case-insensitive) upstream.
        var json = """
        [
          {"id":"00000000-0000-0000-0000-000000000000","nombre":"Legacy","version":"1.0.0"},
          {"id":"00000000-0000-0000-0000-000000000000","nombre":"legacy","version":"2.0.0"}
        ]
        """;
        await File.WriteAllTextAsync(Path.Combine(_temp.Path, "local_games.json"), json);
        var sut = CreateSut();

        // Act
        var games = await sut.GetLocalGamesAsync();

        // Assert
        games.ShouldHaveSingleItem();
        games[0].Version.ShouldBe("1.0.0");
    }

    [Fact]
    public async Task GetLocalGamesAsync_WhenIdlessEntryHasNullName_DoesNotThrowAndKeepsValidEntries()
    {
        // Arrange — an id-less entry whose name is explicitly null. OrdinalIgnoreCase.GetHashCode(null)
        // would throw during dedup and the outer catch would return [] (total loss of the registry).
        var json = """
        [
          {"id":"11111111-1111-1111-1111-111111111111","nombre":"Good","version":"1.0.0"},
          {"id":"00000000-0000-0000-0000-000000000000","nombre":null,"version":"2.0.0"}
        ]
        """;
        await File.WriteAllTextAsync(Path.Combine(_temp.Path, "local_games.json"), json);
        var sut = CreateSut();

        // Act
        var games = await sut.GetLocalGamesAsync();

        // Assert — no throw, and the valid game is not lost alongside the corrupt one.
        games.Count.ShouldBe(2);
        games.ShouldContain(g => g.Nombre == "Good");
    }

    [Fact]
    public async Task GetLocalGamesAsync_WhenEntriesAreDistinct_KeepsThemAll()
    {
        // Arrange — dedup must not be over-aggressive: distinct ids are all preserved.
        var json = """
        [
          {"id":"11111111-1111-1111-1111-111111111111","nombre":"Alpha","version":"1.0.0"},
          {"id":"22222222-2222-2222-2222-222222222222","nombre":"Bravo","version":"1.0.0"}
        ]
        """;
        await File.WriteAllTextAsync(Path.Combine(_temp.Path, "local_games.json"), json);
        var sut = CreateSut();

        // Act
        var games = await sut.GetLocalGamesAsync();

        // Assert
        games.Count.ShouldBe(2);
        games.Select(g => g.Nombre).ShouldBe(["Alpha", "Bravo"]);
    }

    [Fact]
    public async Task RegisterGameAsync_WhenCalled_PersistsGameToRegistryFile()
    {
        // Arrange
        var sut = CreateSut();
        var id = Guid.NewGuid();

        // Act
        await sut.RegisterGameAsync(id, "TestGame", "1.0.0");
        var games = await sut.GetLocalGamesAsync();

        // Assert
        games.ShouldHaveSingleItem();
        games[0].Id.ShouldBe(id);
        games[0].Nombre.ShouldBe("TestGame");
        games[0].Version.ShouldBe("1.0.0");
    }

    [Fact]
    public async Task RegisterGameAsync_WhenSameNameAlreadyExists_ReplacesPreviousEntry()
    {
        // Arrange
        var sut = CreateSut();
        await sut.RegisterGameAsync(Guid.NewGuid(), "TestGame", "1.0.0");

        // Act
        await sut.RegisterGameAsync(Guid.NewGuid(), "TestGame", "1.1.0");
        var games = await sut.GetLocalGamesAsync();

        // Assert
        games.ShouldHaveSingleItem();
        games[0].Version.ShouldBe("1.1.0");
    }

    [Fact]
    public async Task RemoveGameRegistryAsync_WhenGameExists_RemovesItFromTheList()
    {
        // Arrange
        var sut = CreateSut();
        await sut.RegisterGameAsync(Guid.NewGuid(), "GameA", "1.0.0");
        await sut.RegisterGameAsync(Guid.NewGuid(), "GameB", "1.0.0");

        // Act
        await sut.RemoveGameRegistryAsync("GameA");
        var games = await sut.GetLocalGamesAsync();

        // Assert
        games.ShouldHaveSingleItem();
        games[0].Nombre.ShouldBe("GameB");
    }

    [Fact]
    public async Task RemoveGameRegistryAsync_WhenRegistryDoesNotExist_DoesNotThrow()
    {
        // Arrange
        var sut = CreateSut();

        // Act
        var act = () => sut.RemoveGameRegistryAsync("Anything");

        // Assert
        await act.ShouldNotThrowAsync();
    }

    // -------------------- Playtime --------------------

    [Fact]
    public async Task AddPlaytimeAsync_WhenIdIsEmpty_DoesNothing()
    {
        // Arrange
        var sut = CreateSut();

        // Act
        await sut.AddPlaytimeAsync(Guid.Empty, 30);
        var all = await sut.GetAllPlaytimesAsync();

        // Assert
        all.ShouldBeEmpty();
    }

    [Fact]
    public async Task AddPlaytimeAsync_WhenCalledTwiceForSameId_AccumulatesMinutes()
    {
        // Arrange
        var sut = CreateSut();
        var id = Guid.NewGuid();

        // Act
        await sut.AddPlaytimeAsync(id, 30);
        await sut.AddPlaytimeAsync(id, 15);
        var all = await sut.GetAllPlaytimesAsync();

        // Assert
        all[id].ShouldBe(45);
    }

    [Fact]
    public async Task GetAllPlaytimesAsync_WhenRegistryFileMissing_ReturnsEmptyDictionary()
    {
        // Arrange
        var sut = CreateSut();

        // Act
        var all = await sut.GetAllPlaytimesAsync();

        // Assert
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

    // -------------------- GetHomeContentAsync --------------------

    [Fact]
    public async Task GetHomeContentAsync_FiltersOutExpiredItems()
    {
        // Arrange — one expired news item, one current.
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

        // Act
        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        // Assert
        content.News.ShouldHaveSingleItem();
        content.News[0].Title.ShouldBe("Actual");
    }

    // -------------------- Resolve (helper, internal static) — language fallback --------------------

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
        // Two dictionaries with the SAME keys but DIFFERENT insertion order must resolve identically.
        // Upstream this returned Values.FirstOrDefault() (insertion order) => arbitrary language.
        var insertionOrderA = new Dictionary<string, string> { ["pt"] = "Olá", ["fr"] = "Salut", ["ca"] = "Hola-ca" };
        var insertionOrderB = new Dictionary<string, string> { ["fr"] = "Salut", ["ca"] = "Hola-ca", ["pt"] = "Olá" };

        var resultA = ContentService.Resolve(insertionOrderA, "ja");
        var resultB = ContentService.Resolve(insertionOrderB, "ja");

        // "ca" sorts first ordinally among {ca, fr, pt}, so both must agree on it.
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
        // A null localized map (e.g. a DTO field that slipped through) must not NRE on TryGetValue.
        var result = ContentService.Resolve(null, "es");

        result.ShouldBe(string.Empty);
    }

    [Fact]
    public void Resolve_WhenRequestedValueIsNull_SkipsItAndFallsBack()
    {
        // Strict nullability doesn't enforce dictionary *values*, so {"es": null} survives.
        // Resolve must treat a null value as absent and fall through the chain.
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
        // Settings request Spanish, but the item only ships English — the deterministic chain
        // must surface English rather than an arbitrary dictionary entry.
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

    // -------------------- NormalizeToCet (helper, internal static) --------------------

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

    // -------------------- GetHomeContentAsync — timezone-aware expiration --------------------

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
        // Arrange — settings request English, but only Spanish translation is provided.
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

        // Act
        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        // Assert
        content.News[0].Title.ShouldBe("Hola");
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenCalledTwiceWithoutForceRefresh_FetchesOnlyOnce()
    {
        // Arrange
        var json = """{ "news": [], "notifications": [] }""";
        var handler = _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        // Act
        await sut.GetHomeContentAsync();
        await sut.GetHomeContentAsync();

        // Assert — the second call is served from the in-memory cache, no extra HTTP request.
        handler.Requests.Count.ShouldBe(1);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenForceRefresh_BypassesCacheAndRefetches()
    {
        // Arrange
        var json = """{ "news": [], "notifications": [] }""";
        var handler = _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();
        await sut.GetHomeContentAsync();

        // Act
        await sut.GetHomeContentAsync(forceRefresh: true);

        // Assert — forceRefresh nulls the cache and re-fetches.
        handler.Requests.Count.ShouldBe(2);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenManyConcurrentCallersOnColdCache_FetchesOnlyOnce()
    {
        // Arrange — without the gate, concurrent callers all see a null cache and duplicate the
        // HTTP fetch (the BUG-018 race). The gate serialises check-fetch-assign so only one request fires.
        var json = """{ "news": [], "notifications": [] }""";
        var handler = _httpFactory.HandlerFor("Content").RespondWithJson("notifications.json", json);
        var sut = CreateSut();

        // Act — fan out many concurrent callers against a cold cache.
        await Task.WhenAll(Enumerable.Range(0, 50).Select(_ => sut.GetHomeContentAsync()));

        // Assert
        handler.Requests.Count.ShouldBe(1);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenLocalizedValueIsNull_ResolvesToEmptyStringWithoutCrashing()
    {
        // Arrange — the title dictionary is present but its value is null ({"es": null}).
        // Strict nullability doesn't reach dictionary values, so this reaches Resolve, which must
        // degrade to an empty string instead of NRE'ing (the whole home content is rebuilt inside
        // the service try, so an NRE here would silently blank the entire home).
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

        // Act
        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        // Assert
        content.News.ShouldHaveSingleItem();
        content.News[0].Title.ShouldBe(string.Empty);
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenNewsHasNullArrayElement_SkipsIt()
    {
        // Arrange — strict nullability doesn't reach collection elements, so a null entry in the
        // news array survives deserialization; the explicit filter drops it without blanking the
        // rest of the home content.
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

        // Act
        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        // Assert
        content.News.ShouldHaveSingleItem();
        content.News[0].Title.ShouldBe("Vigente");
    }

    [Fact]
    public async Task GetHomeContentAsync_WhenRequestFails_ReturnsEmptyHomeContent()
    {
        // Arrange — no handler set up; default 404 response.
        var sut = CreateSut();

        // Act
        var content = await sut.GetHomeContentAsync(forceRefresh: true);

        // Assert
        content.News.ShouldBeEmpty();
        content.Notifications.ShouldBeEmpty();
    }
}
