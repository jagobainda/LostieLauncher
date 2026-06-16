using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.ViewModels;
using System.Reflection;

namespace LostieLauncher.Tests.ViewModels;

[Collection(WpfCollection.Name)]
public class GamesViewModelTests
{
    private readonly IContentService _contentService = Substitute.For<IContentService>();
    private readonly ITelemetryService _telemetryService = Substitute.For<ITelemetryService>();
    private readonly ISettingsService _settingsService = Substitute.For<ISettingsService>();
    private readonly IDownloadService _downloadService = Substitute.For<IDownloadService>();
    private readonly DownloadOptions _downloadOptions = new(BaseUrl: "https://download.test");
    private readonly GlobalViewModel _globalViewModel = new();

    public GamesViewModelTests(WpfApplicationFixture _)
    {
        _contentService.GetGamesAsync().Returns([]);
        _contentService.GetLocalGamesAsync().Returns([]);
        _contentService.GetAllPlaytimesAsync().Returns(new Dictionary<Guid, int>());
        _telemetryService.GetDownloadCountsAsync().Returns(new Dictionary<string, int>());
        // GetGameDirectory is invoked for HasHelpSubfolder; return a path that does not exist
        // so that branch returns false without touching real disk.
        _contentService.GetGameDirectory(Arg.Any<string>()).Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-nonexistent", ci.Arg<string>()));
    }

    private LibraryViewModel CreateLibrary() => new(_telemetryService, _contentService, _settingsService, _downloadService, _globalViewModel, _downloadOptions);

    private async Task<GamesViewModel> CreateSutAsync()
    {
        var library = CreateLibrary();
        await library.LibraryLoadedTask;
        var sut = new GamesViewModel(_contentService, library, _telemetryService);
        // Wait for the constructor's fire-and-forget LoadInstalledGamesAsync to finish.
        await sut.RefreshAsync();
        return sut;
    }

    [Fact]
    public async Task Constructor_WithNoLocalGames_LeavesInstalledListEmpty()
    {
        // Arrange & Act
        var vm = await CreateSutAsync();

        // Assert
        vm.InstalledGames.ShouldBeEmpty();
        vm.IsLoading.ShouldBeFalse();
        vm.IsEmpty.ShouldBeTrue();
        vm.IsListVisible.ShouldBeFalse();
    }

    [Fact]
    public async Task LoadInstalledGames_PopulatesEntriesFromLocalRegistry()
    {
        // Arrange
        var id = Guid.NewGuid();
        _contentService.GetLocalGamesAsync().Returns([
            TestData.LocalGame(name: "Demo", version: "1.0.0", id: id)
        ]);
        _contentService.GetAllPlaytimesAsync().Returns(new Dictionary<Guid, int> { [id] = 15 });

        // Act
        var vm = await CreateSutAsync();

        // Assert
        var installed = vm.InstalledGames.Single();
        installed.Nombre.ShouldBe("Demo");
        installed.InstalledVersion.ShouldBe("1.0.0");
        installed.PlaytimeMinutes.ShouldBe(15);
    }

    [Fact]
    public async Task LoadInstalledGames_WhenRemoteVersionIsNewer_FlagsHasUpdate()
    {
        // Arrange
        var id = Guid.NewGuid();
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Demo", version: "2.0.0", id: id)
        ]);
        _contentService.GetLocalGamesAsync().Returns([
            TestData.LocalGame(name: "Demo", version: "1.0.0", id: id)
        ]);

        // Act
        var vm = await CreateSutAsync();

        // Assert
        var installed = vm.InstalledGames.Single();
        installed.HasUpdate.ShouldBeTrue();
        installed.UpdateVersion.ShouldBe("2.0.0");
    }

    [Fact]
    public async Task LoadInstalledGames_WhenRemoteAndLocalVersionsMatch_DoesNotFlagUpdate()
    {
        // Arrange
        var id = Guid.NewGuid();
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Demo", version: "1.0.0", id: id)
        ]);
        _contentService.GetLocalGamesAsync().Returns([
            TestData.LocalGame(name: "Demo", version: "1.0.0", id: id)
        ]);

        // Act
        var vm = await CreateSutAsync();

        // Assert
        var installed = vm.InstalledGames.Single();
        installed.HasUpdate.ShouldBeFalse();
        installed.UpdateVersion.ShouldBeEmpty();
    }

    [Fact]
    public async Task NavigateToLibraryCommand_WhenExecuted_RaisesNavigateToLibraryRequested()
    {
        // Arrange
        var vm = await CreateSutAsync();
        var raised = false;
        vm.NavigateToLibraryRequested += () => raised = true;

        // Act
        vm.NavigateToLibraryCommand.Execute(null);

        // Assert
        raised.ShouldBeTrue();
    }

    [Fact]
    public async Task RefreshAsync_RecomputesInstalledList_FromContentService()
    {
        // Arrange
        var vm = await CreateSutAsync();
        _contentService.ClearReceivedCalls();

        // Act
        await vm.RefreshAsync();

        // Assert
        await _contentService.Received(1).GetLocalGamesAsync();
    }

    // -------------------- Play-session accounting (BUG-004) --------------------

    [Fact]
    public async Task RecordPlaySessionAsync_WhenMinutesPositive_PersistsPlaytimeForTheGame()
    {
        // Arrange
        var vm = await CreateSutAsync();
        _contentService.ClearReceivedCalls();
        var id = Guid.NewGuid();

        // Act — the extracted, dispatcher-free accounting path the Exited handler runs.
        await vm.RecordPlaySessionAsync(id, minutes: 25);

        // Assert
        await _contentService.Received(1).AddPlaytimeAsync(id, 25);
    }

    [Fact]
    public async Task RecordPlaySessionAsync_WhenMinutesIsZero_DoesNotPersistAnything()
    {
        // Arrange
        var vm = await CreateSutAsync();
        _contentService.ClearReceivedCalls();

        // Act
        await vm.RecordPlaySessionAsync(Guid.NewGuid(), minutes: 0);

        // Assert
        await _contentService.DidNotReceive().AddPlaytimeAsync(Arg.Any<Guid>(), Arg.Any<int>());
    }

    [Fact]
    public async Task RecordPlaySessionAsync_WhenGameIdIsEmpty_DoesNotPersistAnything()
    {
        // Arrange
        var vm = await CreateSutAsync();
        _contentService.ClearReceivedCalls();

        // Act
        await vm.RecordPlaySessionAsync(Guid.Empty, minutes: 30);

        // Assert
        await _contentService.DidNotReceive().AddPlaytimeAsync(Arg.Any<Guid>(), Arg.Any<int>());
    }

    // -------------------- Disposal / unsubscription (BUG-020) --------------------

    [Fact]
    public async Task Dispose_UnsubscribesFromLibraryGameInstalled()
    {
        // Arrange
        var library = CreateLibrary();
        await library.LibraryLoadedTask;
        var sut = new GamesViewModel(_contentService, library, _telemetryService);
        await sut.RefreshAsync();
        GetGameInstalledSubscriberCount(library).ShouldBe(1);

        // Act
        sut.Dispose();

        // Assert
        GetGameInstalledSubscriberCount(library).ShouldBe(0);
    }

    [Fact]
    public async Task Dispose_WhenCalledTwice_DoesNotThrow()
    {
        // Arrange
        var sut = await CreateSutAsync();

        // Act
        sut.Dispose();
        var act = sut.Dispose;

        // Assert
        Should.NotThrow(act);
    }

    private static int GetGameInstalledSubscriberCount(LibraryViewModel library)
    {
        // GameInstalled is a field-like event; its compiler-generated backing field lets us
        // assert the subscription was actually removed without pumping a dispatcher.
        var field = typeof(LibraryViewModel).GetField("GameInstalled", BindingFlags.Instance | BindingFlags.NonPublic);
        var subscribers = field?.GetValue(library) as Delegate;
        return subscribers?.GetInvocationList().Length ?? 0;
    }
}
