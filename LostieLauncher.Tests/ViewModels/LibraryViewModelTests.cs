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


    [Fact]
    public async Task ResumingPausedGame_ContinuesItsOwnDownload_NotAnotherPausedGame()
    {
        // Arrange — two games that will both end up Paused. The download service records every
        // (url, destination) it is asked to fetch and reports "paused" so the games stay Paused.
        var calls = new List<(string Url, string Dest)>();
        _downloadService
            .DownloadAsync(Arg.Any<string>(), Arg.Any<string>(), Arg.Any<IProgress<DownloadProgressInfo>>(), Arg.Any<CancellationToken>())
            .Returns(ci =>
            {
                calls.Add((ci.ArgAt<string>(0), ci.ArgAt<string>(1)));
                return Task.FromResult(DownloadResult.Cancelled());
            });
        _settingsService.GetGamesRootDirectory().Returns(Path.Combine(Path.GetTempPath(), "LostieLauncherTests-root"));
        _contentService.GetGameDirectory(Arg.Any<string>())
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()));
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Alpha", version: "1.0.0"),
            TestData.Game(name: "Bravo", version: "1.0.0"),
        ]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;

        var argsA = new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip");
        var argsB = new GameDownloadArgs("bravo", "1.0.0", "/b/bravo.zip");

        // Act — start+pause A, then start+pause B (the update path shows no modal dialog), resume A.
        await vm.StartUpdateCommand.ExecuteAsync(argsA);
        await vm.StartUpdateCommand.ExecuteAsync(argsB);

        vm.Games.Single(g => g.GameId == "alpha").DownloadStatus.ShouldBe(GameDownloadStatus.Paused);
        vm.Games.Single(g => g.GameId == "bravo").DownloadStatus.ShouldBe(GameDownloadStatus.Paused);

        calls.Clear();
        await vm.StartDownloadCommand.ExecuteAsync(argsA);   // Resume A

        // Assert — the resume fetched A's own URL/destination, never B's (the BUG-003 regression
        // reused the last global args, which were B's).
        calls.ShouldHaveSingleItem();
        calls[0].Url.ShouldBe("https://download.test/a/alpha.zip");
        calls[0].Url.ShouldNotContain("bravo");
        calls[0].Dest.ShouldContain("alpha.");
    }

    [Fact]
    public async Task PauseDownloadCommand_PausesActiveDownload_FreesTheGlobalGuard_AndKeepsItResumable()
    {
        // Arrange — the first fetch blocks until its token is cancelled (a real pause); any later
        // fetch (the resume) returns immediately so the test does not hang.
        var fetches = 0;
        _downloadService
            .DownloadAsync(Arg.Any<string>(), Arg.Any<string>(), Arg.Any<IProgress<DownloadProgressInfo>>(), Arg.Any<CancellationToken>())
            .Returns(async ci =>
            {
                fetches++;
                if (fetches == 1)
                {
                    var ct = ci.ArgAt<CancellationToken>(3);
                    try { await Task.Delay(Timeout.Infinite, ct); }
                    catch (OperationCanceledException) { }
                }
                return DownloadResult.Cancelled();
            });
        _settingsService.GetGamesRootDirectory().Returns(Path.Combine(Path.GetTempPath(), "LostieLauncherTests-root"));
        _contentService.GetGameDirectory(Arg.Any<string>())
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()));
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Alpha", version: "1.0.0")]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        var args = new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip");

        // Act — kick off the (blocking) download, then pause it by GameId.
        var downloadTask = vm.StartUpdateCommand.ExecuteAsync(args);
        var game = vm.Games.Single();
        game.DownloadStatus.ShouldBe(GameDownloadStatus.Downloading);   // sanity: in flight
        _globalViewModel.IsDownloading.ShouldBeTrue();

        vm.PauseDownloadCommand.Execute("alpha");
        await downloadTask;

        // Assert — paused, the single-download guard is released, and the session survives so a
        // subsequent resume actually launches another fetch.
        game.DownloadStatus.ShouldBe(GameDownloadStatus.Paused);
        _globalViewModel.IsDownloading.ShouldBeFalse();

        await vm.StartDownloadCommand.ExecuteAsync(args);   // Resume
        fetches.ShouldBe(2);
    }

    [Fact]
    public async Task DownloadFinishing_ReleasesTheGlobalGuard_SoFurtherDownloadsArePossible()
    {
        // Arrange — the fetch reports Success. The per-game session refactor must still release the
        // single-download guard on a terminal outcome (not only on pause): otherwise the first
        // completed download would leave IsDownloading stuck true and block every later download.
        var fetches = 0;
        _downloadService
            .DownloadAsync(Arg.Any<string>(), Arg.Any<string>(), Arg.Any<IProgress<DownloadProgressInfo>>(), Arg.Any<CancellationToken>())
            .Returns(_ =>
            {
                fetches++;
                return Task.FromResult(DownloadResult.Succeeded());
            });
        _settingsService.GetGamesRootDirectory().Returns(Path.Combine(Path.GetTempPath(), "LostieLauncherTests-root"));
        _contentService.GetGameDirectory(Arg.Any<string>())
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()));
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Alpha", version: "1.0.0")]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        var args = new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip");

        // Act — the success handler runs (extraction fails harmlessly because no .zip exists on
        // disk, exercising the terminal RemoveSession path either way).
        await vm.StartUpdateCommand.ExecuteAsync(args);

        // Assert — guard released, and a second download is actually allowed through (not early-out).
        _globalViewModel.IsDownloading.ShouldBeFalse();
        await vm.StartUpdateCommand.ExecuteAsync(args);
        fetches.ShouldBe(2);
    }

    [Fact]
    public async Task ExecuteDownload_WhenSomethingThrowsMidFlight_StillReleasesTheGlobalGuard()
    {
        // Arrange — the download service throws between IsDownloading=true and its reset (the BUG-010
        // vector: any throw in the body — binding setter, modal, cleanup — leaves the guard stuck true
        // without a try/finally). DownloadAsync is the only injectable seam in that body, so it stands
        // in for "anything in the body throws"; the realistic production sources (CustomMessageBox in
        // HandleDownloadFailed, binding setters) are modal/dispatcher-bound and not unit-testable.
        var attempts = 0;
        _downloadService
            .DownloadAsync(Arg.Any<string>(), Arg.Any<string>(), Arg.Any<IProgress<DownloadProgressInfo>>(), Arg.Any<CancellationToken>())
            .Returns<Task<DownloadResult>>(_ => { attempts++; throw new IOException("boom"); });
        _settingsService.GetGamesRootDirectory().Returns(Path.Combine(Path.GetTempPath(), "LostieLauncherTests-root"));
        _contentService.GetGameDirectory(Arg.Any<string>())
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()));
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Alpha", version: "1.0.0")]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        var args = new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip");

        // Act — the throw propagates (BUG-014 silences it in production); the guard must still clear.
        await Should.ThrowAsync<IOException>(() => vm.StartUpdateCommand.ExecuteAsync(args));

        // Assert — guard released despite the exception...
        _globalViewModel.IsDownloading.ShouldBeFalse();

        // ...and a second download is genuinely allowed through (the body ran again, not an early-out),
        // proving the guard did not stay stuck true.
        await Should.ThrowAsync<IOException>(() => vm.StartUpdateCommand.ExecuteAsync(args));
        attempts.ShouldBe(2);
    }
}
