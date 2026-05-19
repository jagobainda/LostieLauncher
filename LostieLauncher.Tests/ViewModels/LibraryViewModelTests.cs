using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.ViewModels;
using System.Collections.ObjectModel;

namespace LostieLauncher.Tests.ViewModels;

[Collection(WpfCollection.Name)]
public class LibraryViewModelTests
{
    private readonly ITelemetryService _telemetryService = Substitute.For<ITelemetryService>();
    private readonly IContentService _contentService = Substitute.For<IContentService>();
    private readonly ISettingsService _settingsService = Substitute.For<ISettingsService>();
    private readonly IDownloadService _downloadService = Substitute.For<IDownloadService>();
    private readonly DownloadOptions _downloadOptions = new(BaseUrl: "https://download.test");
    private readonly GlobalViewModel _globalViewModel = new();

    public LibraryViewModelTests(WpfApplicationFixture _)
    {
        // Default mocks: empty everything so initial LoadGamesAsync completes happily.
        _contentService.GetGamesAsync().Returns([]);
        _contentService.GetLocalGamesAsync().Returns([]);
        _contentService.GetAllPlaytimesAsync().Returns(new Dictionary<Guid, int>());
        _telemetryService.GetDownloadCountsAsync().Returns(new Dictionary<string, int>());
    }

    private LibraryViewModel CreateSut() => new(
        _telemetryService, _contentService, _settingsService,
        _downloadService, _globalViewModel, _downloadOptions);

    [Fact]
    public async Task Constructor_TriggersInitialLoad_ResolvingLibraryLoadedTask()
    {
        // Arrange & Act
        var vm = CreateSut();
        await vm.LibraryLoadedTask;

        // Assert
        vm.IsLoading.ShouldBeFalse();
    }

    [Fact]
    public async Task LoadGames_WhenRemoteHasGameAndLocalIsOlder_FlagsItAsUpdateAvailable()
    {
        // Arrange
        var id = Guid.NewGuid();
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Demo", version: "2.0.0", id: id)
        ]);
        _contentService.GetLocalGamesAsync().Returns([
            TestData.LocalGame(name: "Demo", version: "1.0.0", id: id)
        ]);
        var vm = CreateSut();

        // Act
        await vm.LibraryLoadedTask;

        // Assert
        var game = vm.Games.Single();
        game.DownloadStatus.ShouldBe(GameDownloadStatus.UpdateAvailable);
    }

    [Fact]
    public async Task LoadGames_WhenLocalVersionMatchesRemote_FlagsItAsDownloaded()
    {
        // Arrange
        var id = Guid.NewGuid();
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Demo", version: "1.0.0", id: id)
        ]);
        _contentService.GetLocalGamesAsync().Returns([
            TestData.LocalGame(name: "Demo", version: "1.0.0", id: id)
        ]);
        var vm = CreateSut();

        // Act
        await vm.LibraryLoadedTask;

        // Assert
        vm.Games.Single().DownloadStatus.ShouldBe(GameDownloadStatus.Downloaded);
    }

    [Fact]
    public async Task LoadGames_WhenGameNotInstalledLocally_LeavesStatusAsAvailable()
    {
        // Arrange
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Fresh", version: "1.0.0")
        ]);
        var vm = CreateSut();

        // Act
        await vm.LibraryLoadedTask;

        // Assert — default status from the model is Available.
        vm.Games.Single().DownloadStatus.ShouldBe(GameDownloadStatus.Available);
    }

    [Fact]
    public async Task LoadGames_AppliesPlaytimesFromContentService()
    {
        // Arrange
        var id = Guid.NewGuid();
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Demo", id: id)
        ]);
        _contentService.GetAllPlaytimesAsync().Returns(new Dictionary<Guid, int> { [id] = 42 });
        var vm = CreateSut();

        // Act
        await vm.LibraryLoadedTask;

        // Assert
        vm.Games.Single().PlaytimeMinutes.ShouldBe(42);
    }

    [Fact]
    public async Task LoadGames_AppliesDownloadCountsFromTelemetry_KeyedByGameSlug()
    {
        // Arrange — slug for "Cool Game" is "cool-game".
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Cool Game")
        ]);
        _telemetryService.GetDownloadCountsAsync()
            .Returns(new Dictionary<string, int> { ["cool-game"] = 99 });
        var vm = CreateSut();

        // Act
        await vm.LibraryLoadedTask;

        // Assert
        vm.Games.Single().TotalDownloads.ShouldBe(99);
    }

    [Fact]
    public async Task IsEmpty_WhenServerReturnsNoGames_ReturnsTrueAfterLoad()
    {
        // Arrange — default mocks already return empty lists.
        var vm = CreateSut();

        // Act
        await vm.LibraryLoadedTask;

        // Assert
        vm.IsEmpty.ShouldBeTrue();
        vm.IsListVisible.ShouldBeFalse();
    }

    [Fact]
    public async Task IsListVisible_WhenServerReturnsGames_ReturnsTrueAndIsEmptyIsFalse()
    {
        // Arrange
        _contentService.GetGamesAsync().Returns([TestData.Game()]);
        var vm = CreateSut();

        // Act
        await vm.LibraryLoadedTask;

        // Assert
        vm.IsListVisible.ShouldBeTrue();
        vm.IsEmpty.ShouldBeFalse();
    }

    [Fact]
    public async Task RefreshAsync_RequestsGamesAgain_FromContentService()
    {
        // Arrange
        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        _contentService.ClearReceivedCalls();

        // Act
        await vm.RefreshAsync();

        // Assert
        await _contentService.Received(1).GetGamesAsync();
    }

    [Fact]
    public void StartDownloadCommand_WhenAlreadyDownloading_ReturnsImmediatelyAndDoesNothing()
    {
        // Arrange
        var vm = CreateSut();
        _globalViewModel.IsDownloading = true;
        var args = new GameDownloadArgs("any-game", "1.0.0", "/x");

        // Act — command should early-out without consulting the content service.
        vm.StartDownloadCommand.Execute(args);

        // Assert — IsServerActionBlockedAsync must NOT have been called.
        _contentService.DidNotReceive().IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>());
    }

    [Fact]
    public void PauseDownloadCommand_WhenNoActiveDownload_DoesNotThrow()
    {
        // Arrange
        var vm = CreateSut();

        // Act
        var act = () => vm.PauseDownloadCommand.Execute(null);

        // Assert
        Should.NotThrow(act);
    }
}
