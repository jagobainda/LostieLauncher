using System.IO;
using System.Text.Json;
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
