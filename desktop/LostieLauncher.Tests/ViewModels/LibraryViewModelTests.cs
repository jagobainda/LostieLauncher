using LostieLauncher.Content;
using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.ViewModels;
using System.Security.Cryptography;

namespace LostieLauncher.Tests.ViewModels;

[Collection(WpfCollection.Name)]
public class LibraryViewModelTests
{
    private readonly IContentService _contentService = Substitute.For<IContentService>();
    private readonly ISettingsService _settingsService = Substitute.For<ISettingsService>();
    private readonly IDownloadService _downloadService = Substitute.For<IDownloadService>();
    private readonly DownloadOptions _downloadOptions = new(BaseUrl: "https://download.test");
    private readonly GlobalViewModel _globalViewModel = new();
    private readonly IDownloadLocationService _downloadLocation = Substitute.For<IDownloadLocationService>();
    private readonly IDownloadLocationNotifier _downloadLocationNotifier = Substitute.For<IDownloadLocationNotifier>();

    public LibraryViewModelTests(WpfApplicationFixture _)
    {
        _contentService.GetGamesAsync().Returns([]);
        _contentService.GetLocalGamesAsync().Returns([]);
        _contentService.GetAllPlaytimesAsync().Returns(new Dictionary<Guid, int>());

        _downloadLocation.Probe(Arg.Any<string>())
            .Returns(ci => DirectoryProbeResult.Usable(ci.Arg<string>()));
    }

    private LibraryViewModel CreateSut() => new(
        _contentService, _settingsService,
        _downloadService, _globalViewModel, _downloadOptions,
        _downloadLocation, _downloadLocationNotifier);

    [Theory]
    [InlineData(GameDownloadStatus.Downloading, false)]
    [InlineData(GameDownloadStatus.Paused, false)]
    [InlineData(GameDownloadStatus.VerifyingIntegrity, true)]
    [InlineData(GameDownloadStatus.Extracting, true)]
    [InlineData(GameDownloadStatus.Downloaded, true)]
    [InlineData(GameDownloadStatus.Available, true)]
    [InlineData(GameDownloadStatus.UpdateAvailable, true)]
    public void ConfirmDownloadCancellation_PhaseChanges_DismissesOnlyOutsideTransfer(GameDownloadStatus status, bool dismissed)
    {
        var game = new GameInfo { DownloadStatus = GameDownloadStatus.Downloading };

        var confirmed = LibraryViewModel.ConfirmDownloadCancellation(game, token =>
        {
            game.DownloadStatus = status;
            token.IsCancellationRequested.ShouldBe(dismissed);
            return true;
        });

        confirmed.ShouldBe(!dismissed);
        game.DownloadStatus = GameDownloadStatus.Downloading;
        game.DownloadStatus = GameDownloadStatus.Extracting;
    }

    [Fact]
    public void ConfirmDownloadCancellation_AlreadyExtracting_DoesNotOpenDialog()
    {
        var game = new GameInfo { DownloadStatus = GameDownloadStatus.Extracting };

        var confirmed = LibraryViewModel.ConfirmDownloadCancellation(game, _ => throw new InvalidOperationException());

        confirmed.ShouldBeFalse();
    }

    [Fact]
    public void ConfirmDownloadCancellation_UserDeclines_DoesNotConfirm()
    {
        var game = new GameInfo { DownloadStatus = GameDownloadStatus.Paused };

        var confirmed = LibraryViewModel.ConfirmDownloadCancellation(game, _ => false);

        confirmed.ShouldBeFalse();
        game.DownloadStatus = GameDownloadStatus.Extracting;
    }

    [Fact]
    public async Task Constructor_TriggersInitialLoad_ResolvingLibraryLoadedTask()
    {
        var vm = CreateSut();
        await vm.LibraryLoadedTask;

        vm.IsLoading.ShouldBeFalse();
    }

    [Fact]
    public async Task LoadGames_WhenRemoteHasGameAndLocalIsOlder_FlagsItAsUpdateAvailable()
    {
        var id = Guid.NewGuid();
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Demo", version: "2.0.0", id: id)
        ]);
        _contentService.GetLocalGamesAsync().Returns([
            TestData.LocalGame(name: "Demo", version: "1.0.0", id: id)
        ]);
        var vm = CreateSut();

        await vm.LibraryLoadedTask;

        var game = vm.Games.Single();
        game.DownloadStatus.ShouldBe(GameDownloadStatus.UpdateAvailable);
    }

    [Fact]
    public async Task LoadGames_WhenLocalVersionMatchesRemote_FlagsItAsDownloaded()
    {
        var id = Guid.NewGuid();
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Demo", version: "1.0.0", id: id)
        ]);
        _contentService.GetLocalGamesAsync().Returns([
            TestData.LocalGame(name: "Demo", version: "1.0.0", id: id)
        ]);
        var vm = CreateSut();

        await vm.LibraryLoadedTask;

        vm.Games.Single().DownloadStatus.ShouldBe(GameDownloadStatus.Downloaded);
    }

    [Fact]
    public async Task LoadGames_WhenGameNotInstalledLocally_LeavesStatusAsAvailable()
    {
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Fresh", version: "1.0.0")
        ]);
        var vm = CreateSut();

        await vm.LibraryLoadedTask;

        vm.Games.Single().DownloadStatus.ShouldBe(GameDownloadStatus.Available);
    }

    [Fact]
    public async Task LoadGames_AppliesPlaytimesFromContentService()
    {
        var id = Guid.NewGuid();
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Demo", id: id)
        ]);
        _contentService.GetAllPlaytimesAsync().Returns(new Dictionary<Guid, int> { [id] = 42 });
        var vm = CreateSut();

        await vm.LibraryLoadedTask;

        vm.Games.Single().PlaytimeMinutes.ShouldBe(42);
    }

    [Fact]
    public async Task IsEmpty_WhenServerReturnsNoGames_ReturnsTrueAfterLoad()
    {
        var vm = CreateSut();

        await vm.LibraryLoadedTask;

        vm.IsEmpty.ShouldBeTrue();
        vm.IsListVisible.ShouldBeFalse();
    }

    [Fact]
    public async Task IsListVisible_WhenServerReturnsGames_ReturnsTrueAndIsEmptyIsFalse()
    {
        _contentService.GetGamesAsync().Returns([TestData.Game()]);
        var vm = CreateSut();

        await vm.LibraryLoadedTask;

        vm.IsListVisible.ShouldBeTrue();
        vm.IsEmpty.ShouldBeFalse();
    }

    [Fact]
    public async Task RefreshAsync_RequestsGamesAgain_FromContentService()
    {
        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        _contentService.ClearReceivedCalls();

        await vm.RefreshAsync();

        await _contentService.Received(1).GetGamesAsync();
    }

    [Fact]
    public void StartDownloadCommand_WhenAlreadyDownloading_ReturnsImmediatelyAndDoesNothing()
    {
        var vm = CreateSut();
        _globalViewModel.IsDownloading = true;
        var args = new GameDownloadArgs("any-game", "1.0.0", "/x");

        vm.StartDownloadCommand.Execute(args);

        _contentService.DidNotReceive().IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>());
    }

    [Fact]
    public void PauseDownloadCommand_WhenNoActiveDownload_DoesNotThrow()
    {
        var vm = CreateSut();

        var act = () => vm.PauseDownloadCommand.Execute(null);

        Should.NotThrow(act);
    }


    [Fact]
    public async Task ResumingPausedGame_ContinuesItsOwnDownload_NotAnotherPausedGame()
    {
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
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()!));
        _contentService.GetGamesAsync().Returns([
            TestData.Game(name: "Alpha", version: "1.0.0"),
            TestData.Game(name: "Bravo", version: "1.0.0"),
        ]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;

        var argsA = new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip");
        var argsB = new GameDownloadArgs("bravo", "1.0.0", "/b/bravo.zip");

        await vm.StartUpdateCommand.ExecuteAsync(argsA);
        await vm.StartUpdateCommand.ExecuteAsync(argsB);

        vm.Games.Single(g => g.GameId == "alpha").DownloadStatus.ShouldBe(GameDownloadStatus.Paused);
        vm.Games.Single(g => g.GameId == "bravo").DownloadStatus.ShouldBe(GameDownloadStatus.Paused);

        calls.Clear();
        await vm.StartDownloadCommand.ExecuteAsync(argsA);

        calls.ShouldHaveSingleItem();
        calls[0].Url.ShouldBe("https://download.test/a/alpha.zip");
        calls[0].Url.ShouldNotContain("bravo");
        calls[0].Dest.ShouldContain("alpha.");
    }

    [Fact]
    public async Task PauseDownloadCommand_PausesActiveDownload_FreesTheGlobalGuard_AndKeepsItResumable()
    {
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
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()!));
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Alpha", version: "1.0.0")]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        var args = new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip");

        var downloadTask = vm.StartUpdateCommand.ExecuteAsync(args);
        var game = vm.Games.Single();
        game.DownloadStatus.ShouldBe(GameDownloadStatus.Downloading);
        _globalViewModel.IsDownloading.ShouldBeTrue();

        vm.PauseDownloadCommand.Execute("alpha");
        await downloadTask;

        game.DownloadStatus.ShouldBe(GameDownloadStatus.Paused);
        _globalViewModel.IsDownloading.ShouldBeFalse();

        await vm.StartDownloadCommand.ExecuteAsync(args);
        fetches.ShouldBe(2);
    }

    [Fact]
    public async Task DownloadFinishing_ReleasesTheGlobalGuard_SoFurtherDownloadsArePossible()
    {
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
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()!));
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Alpha", version: "1.0.0")]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        var args = new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip");

        await vm.StartUpdateCommand.ExecuteAsync(args);

        _globalViewModel.IsDownloading.ShouldBeFalse();
        await vm.StartUpdateCommand.ExecuteAsync(args);
        fetches.ShouldBe(2);
    }

    [Fact]
    public async Task StartUpdate_WhenTheDownloadDirectoryCannotFinalizeFiles_NeverStartsTheTransfer()
    {
        var root = Path.Combine(Path.GetTempPath(), "LostieLauncherTests-root");
        _settingsService.GetGamesRootDirectory().Returns(root);
        _contentService.GetGameDirectory(Arg.Any<string>())
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()!));
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Alpha", version: "1.0.0")]);
        _downloadLocation.Probe(Arg.Any<string>())
            .Returns(ci => new DirectoryProbeResult(DirectoryProbeOutcome.CannotRename, ci.Arg<string>(), "UnauthorizedAccessException: Access to the path is denied."));

        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        var game = vm.Games.Single();
        var statusBefore = game.DownloadStatus;

        await vm.StartUpdateCommand.ExecuteAsync(new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip"));

        await _downloadService.DidNotReceive().DownloadAsync(
            Arg.Any<string>(), Arg.Any<string>(), Arg.Any<IProgress<DownloadProgressInfo>>(), Arg.Any<CancellationToken>());
        _downloadLocationNotifier.Received(1).NotifyDirectoryNotUsable(
            Arg.Is<DirectoryProbeResult>(r => r.Outcome == DirectoryProbeOutcome.CannotRename));
        game.DownloadStatus.ShouldBe(statusBefore);
        game.DownloadProgressValue.ShouldBe(0);
        _globalViewModel.IsDownloading.ShouldBeFalse();
    }

    [Fact]
    public async Task StartUpdate_ProbesTheFolderTheArchiveIsFinalizedIn()
    {
        var root = Path.Combine(Path.GetTempPath(), "LostieLauncherTests-root");
        _settingsService.GetGamesRootDirectory().Returns(root);
        _contentService.GetGameDirectory(Arg.Any<string>())
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()!));
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Alpha", version: "1.0.0")]);
        _downloadService
            .DownloadAsync(Arg.Any<string>(), Arg.Any<string>(), Arg.Any<IProgress<DownloadProgressInfo>>(), Arg.Any<CancellationToken>())
            .Returns(Task.FromResult(DownloadResult.Cancelled()));

        var vm = CreateSut();
        await vm.LibraryLoadedTask;

        await vm.StartUpdateCommand.ExecuteAsync(new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip"));

        _downloadLocation.Received(1).Probe(Path.Combine(root, ".downloads"));
    }

    [Fact]
    public async Task ResumingPausedGame_RunsThePreFlightAgain()
    {
        var root = Path.Combine(Path.GetTempPath(), "LostieLauncherTests-root");
        _settingsService.GetGamesRootDirectory().Returns(root);
        _contentService.GetGameDirectory(Arg.Any<string>())
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()!));
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Alpha", version: "1.0.0")]);
        _downloadService
            .DownloadAsync(Arg.Any<string>(), Arg.Any<string>(), Arg.Any<IProgress<DownloadProgressInfo>>(), Arg.Any<CancellationToken>())
            .Returns(Task.FromResult(DownloadResult.Cancelled()));

        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        var args = new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip");
        await vm.StartUpdateCommand.ExecuteAsync(args);
        vm.Games.Single().DownloadStatus.ShouldBe(GameDownloadStatus.Paused);

        _downloadLocation.ClearReceivedCalls();
        _downloadService.ClearReceivedCalls();
        _downloadLocation.Probe(Arg.Any<string>())
            .Returns(ci => new DirectoryProbeResult(DirectoryProbeOutcome.CannotRename, ci.Arg<string>(), "denied"));

        await vm.StartDownloadCommand.ExecuteAsync(args);

        _downloadLocation.Received(1).Probe(Arg.Any<string>());
        await _downloadService.DidNotReceive().DownloadAsync(
            Arg.Any<string>(), Arg.Any<string>(), Arg.Any<IProgress<DownloadProgressInfo>>(), Arg.Any<CancellationToken>());
        vm.Games.Single().DownloadStatus.ShouldBe(GameDownloadStatus.Paused);
    }

    [Fact]
    public async Task ExecuteDownload_WhenSomethingThrowsMidFlight_StillReleasesTheGlobalGuard()
    {
        var attempts = 0;
        _downloadService
            .DownloadAsync(Arg.Any<string>(), Arg.Any<string>(), Arg.Any<IProgress<DownloadProgressInfo>>(), Arg.Any<CancellationToken>())
            .Returns<Task<DownloadResult>>(_ => { attempts++; throw new IOException("boom"); });
        _settingsService.GetGamesRootDirectory().Returns(Path.Combine(Path.GetTempPath(), "LostieLauncherTests-root"));
        _contentService.GetGameDirectory(Arg.Any<string>())
            .Returns(ci => Path.Combine(Path.GetTempPath(), "LostieLauncherTests-extract", ci.Arg<string>()!));
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Alpha", version: "1.0.0")]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;
        var args = new GameDownloadArgs("alpha", "1.0.0", "/a/alpha.zip");

        await Should.ThrowAsync<IOException>(() => vm.StartUpdateCommand.ExecuteAsync(args));

        _globalViewModel.IsDownloading.ShouldBeFalse();

        await Should.ThrowAsync<IOException>(() => vm.StartUpdateCommand.ExecuteAsync(args));
        attempts.ShouldBe(2);
    }

    [Fact]
    public async Task VerifyIntegrity_WhenFileMatchesExpectedHash_ReturnsTrue()
    {
        using var root = new TempDirectoryFixture("verify-match");
        var zip = root.Combine("game.zip");
        var bytes = new byte[] { 1, 2, 3, 4, 5 };
        await File.WriteAllBytesAsync(zip, bytes);
        var expected = Convert.ToHexString(SHA256.HashData(bytes));

        var ok = await LibraryViewModel.VerifyIntegrityAsync(TestData.Game(), zip, expected);

        ok.ShouldBeTrue();
    }

    [Fact]
    public async Task VerifyIntegrity_IsCaseInsensitive_AcceptsLowercaseHash()
    {
        using var root = new TempDirectoryFixture("verify-case");
        var zip = root.Combine("game.zip");
        var bytes = new byte[] { 9, 8, 7 };
        await File.WriteAllBytesAsync(zip, bytes);
        var expected = Convert.ToHexString(SHA256.HashData(bytes)).ToLowerInvariant();

        var ok = await LibraryViewModel.VerifyIntegrityAsync(TestData.Game(), zip, expected);

        ok.ShouldBeTrue();
    }

    [Fact]
    public async Task VerifyIntegrity_WhenHashDiffers_ReturnsFalse()
    {
        using var root = new TempDirectoryFixture("verify-mismatch");
        var zip = root.Combine("game.zip");
        await File.WriteAllBytesAsync(zip, new byte[] { 1, 2, 3 });
        var wrong = new string('a', 64);

        var ok = await LibraryViewModel.VerifyIntegrityAsync(TestData.Game(), zip, wrong);

        ok.ShouldBeFalse();
    }

    [Fact]
    public async Task VerifyIntegrity_WhenHashIsEmpty_FailsClosed_WithoutInstalling()
    {
        using var root = new TempDirectoryFixture("verify-nohash");
        var missingZip = root.Combine("does-not-exist.zip");

        var ok = await LibraryViewModel.VerifyIntegrityAsync(TestData.Game(), missingZip, string.Empty);

        ok.ShouldBeFalse();
    }

    [Fact]
    public async Task VerifyIntegrity_WhenHashIsNull_FailsClosed_WithoutInstalling()
    {
        using var root = new TempDirectoryFixture("verify-nullhash");
        var missingZip = root.Combine("does-not-exist.zip");

        var ok = await LibraryViewModel.VerifyIntegrityAsync(TestData.Game(), missingZip, null);

        ok.ShouldBeFalse();
    }

    [Fact]
    public async Task VerifyIntegrity_WhenHashIsMalformed_FailsClosed()
    {
        using var root = new TempDirectoryFixture("verify-malformed");
        var zip = root.Combine("game.zip");
        await File.WriteAllBytesAsync(zip, new byte[] { 1 });

        var ok = await LibraryViewModel.VerifyIntegrityAsync(TestData.Game(), zip, "not-a-sha256");

        ok.ShouldBeFalse();
    }

    [Fact]
    public void IsValidSpecialVersionConfig_WithValidArchivoHashAndVersion_ReturnsTrue()
    {
        var config = new SpecialVersionConfig
        {
            Archivo = "game.zip",
            Sha256 = new string('A', 64),
            Version = "1.0.0",
        };

        LibraryViewModel.IsValidSpecialVersionConfig(config).ShouldBeTrue();
    }

    [Fact]
    public void IsValidSpecialVersionConfig_WithEmptySha256_ReturnsFalse()
    {
        var config = new SpecialVersionConfig
        {
            Archivo = "game.zip",
            Sha256 = string.Empty,
            Version = "1.0.0",
        };

        LibraryViewModel.IsValidSpecialVersionConfig(config).ShouldBeFalse();
    }

    [Fact]
    public void IsValidSpecialVersionConfig_WithMalformedSha256_ReturnsFalse()
    {
        var config = new SpecialVersionConfig
        {
            Archivo = "game.zip",
            Sha256 = "abc",
            Version = "1.0.0",
        };

        LibraryViewModel.IsValidSpecialVersionConfig(config).ShouldBeFalse();
    }

    [Fact]
    public void GetSpecialVersionConfigErrorMessage_WhenNotFound_ReturnsKeyNotFoundMessage()
    {
        var strings = new Eng();

        var error = LibraryViewModel.GetSpecialVersionConfigErrorMessage(SpecialVersionConfigOutcome.NotFound, strings);

        error.ShouldNotBeNull();
        error!.Value.Title.ShouldBe(strings.DownloadKeyNotFoundTitle);
        error.Value.Message.ShouldBe(strings.DownloadKeyNotFoundMessage);
    }

    [Fact]
    public void GetSpecialVersionConfigErrorMessage_WhenNetworkError_ReturnsDownloadErrorNotKeyNotFound()
    {
        var strings = new Eng();

        var error = LibraryViewModel.GetSpecialVersionConfigErrorMessage(SpecialVersionConfigOutcome.NetworkError, strings);

        error.ShouldNotBeNull();
        error!.Value.Title.ShouldBe(strings.DownloadErrorTitle);
        error.Value.Message.ShouldBe(strings.DownloadErrorMessage);
        error.Value.Title.ShouldNotBe(strings.DownloadKeyNotFoundTitle);
    }

    [Fact]
    public void GetSpecialVersionConfigErrorMessage_WhenInvalidResponse_ReturnsDownloadError()
    {
        var strings = new Eng();

        var error = LibraryViewModel.GetSpecialVersionConfigErrorMessage(SpecialVersionConfigOutcome.InvalidResponse, strings);

        error.ShouldNotBeNull();
        error!.Value.Title.ShouldBe(strings.DownloadErrorTitle);
        error.Value.Message.ShouldBe(strings.DownloadErrorMessage);
    }

    [Fact]
    public void GetSpecialVersionConfigErrorMessage_WhenCancelled_ReturnsNull()
    {
        var strings = new Eng();

        LibraryViewModel.GetSpecialVersionConfigErrorMessage(SpecialVersionConfigOutcome.Cancelled, strings).ShouldBeNull();
    }

    [Fact]
    public void GetSpecialVersionConfigErrorMessage_WhenSuccess_ReturnsNull()
    {
        var strings = new Eng();

        LibraryViewModel.GetSpecialVersionConfigErrorMessage(SpecialVersionConfigOutcome.Success, strings).ShouldBeNull();
    }

    [Fact]
    public async Task LoadGames_PurgesTheDownloadsFolderOfPartialsNothingWillEverResume()
    {
        using var root = new TempDirectoryFixture("downloads-purge");
        var downloads = Directory.CreateDirectory(root.Combine(".downloads")).FullName;
        var orphan = Path.Combine(downloads, "removed-game.abc123.zip.part");
        var resumable = Path.Combine(downloads, "demo.abc123.zip.part");
        File.WriteAllText(orphan, "4 GiB of nobody's business");
        File.WriteAllText(resumable, "still resumable");

        _settingsService.GetGamesRootDirectory().Returns(root.Path);
        _contentService.GetGamesAsync().Returns([TestData.Game(name: "Demo", version: "1.0.0")]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;

        File.Exists(orphan).ShouldBeFalse();
        File.Exists(resumable).ShouldBeTrue();
    }

    [Fact]
    public async Task LoadGames_WhenTheCatalogCameBackEmpty_LeavesTheDownloadsFolderAlone()
    {
        using var root = new TempDirectoryFixture("downloads-purge-offline");
        var downloads = Directory.CreateDirectory(root.Combine(".downloads")).FullName;
        var resumable = Path.Combine(downloads, "demo.abc123.zip.part");
        File.WriteAllText(resumable, "still resumable");

        _settingsService.GetGamesRootDirectory().Returns(root.Path);
        _contentService.GetGamesAsync().Returns([]);

        var vm = CreateSut();
        await vm.LibraryLoadedTask;

        File.Exists(resumable).ShouldBeTrue();
    }
}
