using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.ViewModels;

namespace LostieLauncher.Tests.ViewModels;

[Collection(WpfCollection.Name)]
public class MainViewModelTests
{
    private readonly IContentService _contentService = Substitute.For<IContentService>();
    private readonly ITwitchLiveService _twitchLiveService = Substitute.For<ITwitchLiveService>();
    private readonly GlobalViewModel _global = new();
    private HomeViewModel _home = null!;
    private readonly ISettingsService _settingsService = Substitute.For<ISettingsService>();
    private readonly IWindowsStartupService _startupService = Substitute.For<IWindowsStartupService>();
    private readonly IDownloadService _downloadService = Substitute.For<IDownloadService>();
    private readonly DownloadOptions _downloadOptions = new(BaseUrl: "https://download.test");

    public MainViewModelTests(WpfApplicationFixture _)
    {
        _settingsService.Load().Returns(new AppSettings());
        _contentService.GetGamesAsync().Returns([]);
        _contentService.GetLocalGamesAsync().Returns([]);
        _contentService.GetAllPlaytimesAsync().Returns(new Dictionary<Guid, int>());
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        _contentService.GetGameDirectory(Arg.Any<string>()).Returns(ci =>
            Path.Combine(Path.GetTempPath(), "LostieLauncherTests-nonexistent", ci.Arg<string>()!));
    }

    private async Task<MainViewModel> CreateSutAsync()
    {
        var settings = new SettingsViewModel(_settingsService, _startupService, _global, Substitute.For<IUpdateService>(),
            Substitute.For<IDownloadLocationService>(), Substitute.For<IDownloadLocationNotifier>());
        var library = new LibraryViewModel(_contentService, _settingsService, _downloadService, _global, _downloadOptions,
            Substitute.For<IDownloadLocationService>(), Substitute.For<IDownloadLocationNotifier>());
        await library.LibraryLoadedTask;
        _home = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await _home.RefreshAsync();
        var games = new GamesViewModel(_contentService, library, _global);
        await games.RefreshAsync();
        var faqs = new FaqsViewModel(settings);
        return new MainViewModel(_global, _home, games, library, faqs, settings);
    }

    private async Task EndMaintenanceAsync()
    {
        _contentService.ClearReceivedCalls();
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        await _home.RefreshAsync();
    }

    [Fact]
    public async Task Constructor_DefaultsToHomeViewModelAsCurrent()
    {
        // Arrange & Act
        var vm = await CreateSutAsync();

        // Assert
        vm.IsHomeActive.ShouldBeTrue();
        vm.IsGamesActive.ShouldBeFalse();
        vm.IsLibraryActive.ShouldBeFalse();
        vm.IsSettingsActive.ShouldBeFalse();
    }

    [Fact]
    public async Task NavigateToGamesCommand_SwitchesCurrentViewModelAndUpdatesTitle()
    {
        // Arrange
        var vm = await CreateSutAsync();
        var expectedTitle = SettingsViewModel.Instance.Strings.TitleGames;

        // Act
        vm.NavigateToGamesCommand.Execute(null);

        // Assert
        vm.IsGamesActive.ShouldBeTrue();
        vm.CurrentTitle.ShouldBe(expectedTitle);
    }

    [Fact]
    public async Task NavigateToLibraryCommand_SwitchesCurrentViewModelAndUpdatesTitle()
    {
        // Arrange
        var vm = await CreateSutAsync();
        var expectedTitle = SettingsViewModel.Instance.Strings.TitleLibrary;

        // Act
        vm.NavigateToLibraryCommand.Execute(null);

        // Assert
        vm.IsLibraryActive.ShouldBeTrue();
        vm.CurrentTitle.ShouldBe(expectedTitle);
    }

    [Fact]
    public async Task NavigateToSettingsCommand_SwitchesCurrentViewModelAndUpdatesTitle()
    {
        // Arrange
        var vm = await CreateSutAsync();
        var expectedTitle = SettingsViewModel.Instance.Strings.TitleSettings;

        // Act
        vm.NavigateToSettingsCommand.Execute(null);

        // Assert
        vm.IsSettingsActive.ShouldBeTrue();
        vm.CurrentTitle.ShouldBe(expectedTitle);
    }

    [Fact]
    public async Task NavigateToHomeCommand_FromAnotherView_SwitchesBackToHome()
    {
        // Arrange
        var vm = await CreateSutAsync();
        vm.NavigateToSettingsCommand.Execute(null);

        // Act
        vm.NavigateToHomeCommand.Execute(null);

        // Assert
        vm.IsHomeActive.ShouldBeTrue();
    }

    [Fact]
    public async Task IsRefreshVisible_OnlyInHomeGamesAndLibrary()
    {
        // Arrange
        var vm = await CreateSutAsync();

        // Act & Assert
        vm.IsRefreshVisible.ShouldBeTrue();
        vm.NavigateToGamesCommand.Execute(null);
        vm.IsRefreshVisible.ShouldBeTrue();
        vm.NavigateToLibraryCommand.Execute(null);
        vm.IsRefreshVisible.ShouldBeTrue();
        vm.NavigateToFaqsCommand.Execute(null);
        vm.IsRefreshVisible.ShouldBeFalse();
        vm.NavigateToSettingsCommand.Execute(null);
        vm.IsRefreshVisible.ShouldBeFalse();
    }

    [Fact]
    public async Task CurrentViewModelChange_RaisesPropertyChangedForActiveFlags()
    {
        // Arrange
        var vm = await CreateSutAsync();
        using var recorder = new PropertyChangedRecorder(vm);

        // Act
        vm.NavigateToGamesCommand.Execute(null);

        // Assert
        recorder.WasRaised(nameof(MainViewModel.IsHomeActive)).ShouldBeTrue();
        recorder.WasRaised(nameof(MainViewModel.IsGamesActive)).ShouldBeTrue();
        recorder.WasRaised(nameof(MainViewModel.IsLibraryActive)).ShouldBeTrue();
        recorder.WasRaised(nameof(MainViewModel.IsSettingsActive)).ShouldBeTrue();
    }

    [Fact]
    public async Task RefreshDataCommand_CanExecute_ReturnsFalseWhileGlobalIsDownloading()
    {
        // Arrange
        var global = new GlobalViewModel();
        var settings = new SettingsViewModel(_settingsService, _startupService, global, Substitute.For<IUpdateService>(),
            Substitute.For<IDownloadLocationService>(), Substitute.For<IDownloadLocationNotifier>());
        var library = new LibraryViewModel(_contentService, _settingsService, _downloadService, global, _downloadOptions,
            Substitute.For<IDownloadLocationService>(), Substitute.For<IDownloadLocationNotifier>());
        await library.LibraryLoadedTask;
        var home = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await home.RefreshAsync();
        var games = new GamesViewModel(_contentService, library, global);
        await games.RefreshAsync();
        var faqs = new FaqsViewModel(settings);
        var vm = new MainViewModel(global, home, games, library, faqs, settings);

        // Act
        global.IsDownloading = true;

        // Assert
        vm.RefreshDataCommand.CanExecute(null).ShouldBeFalse();
    }

    [Fact]
    public async Task MaintenanceEnding_ReloadsTheCatalogue()
    {
        // Arrange
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(true);
        var vm = await CreateSutAsync();

        // Act
        await EndMaintenanceAsync();

        // Assert
        vm.IsOfflineMode.ShouldBeFalse();
        await _contentService.Received(1).GetGamesAsync();
    }

    [Fact]
    public async Task MaintenanceEnding_WhileDownloading_KeepsTheCatalogue()
    {
        // Arrange
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(true);
        await CreateSutAsync();
        _global.IsDownloading = true;

        // Act
        await EndMaintenanceAsync();

        // Assert
        await _contentService.DidNotReceive().GetGamesAsync();
    }

    [Fact]
    public async Task HomeRefresh_WithoutMaintenance_DoesNotReloadTheCatalogue()
    {
        // Arrange
        await CreateSutAsync();

        // Act
        await EndMaintenanceAsync();

        // Assert
        await _contentService.DidNotReceive().GetGamesAsync();
    }

    [Fact]
    public async Task RefreshDataCommand_CanExecute_ReturnsTrueWhenIdle()
    {
        // Arrange
        var vm = await CreateSutAsync();

        // Act & Assert
        vm.RefreshDataCommand.CanExecute(null).ShouldBeTrue();
    }
}
