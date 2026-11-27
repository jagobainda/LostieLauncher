using LostieLauncher.Content;
using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.ViewModels;

namespace LostieLauncher.Tests.ViewModels;

[Collection(WpfCollection.Name)]
public class SettingsViewModelTests
{
    private readonly ISettingsService _settingsService = Substitute.For<ISettingsService>();
    private readonly IWindowsStartupService _startupService = Substitute.For<IWindowsStartupService>();
    private readonly GlobalViewModel _globalViewModel = new();
    private readonly IUpdateService _updateService = Substitute.For<IUpdateService>();
    private readonly IDownloadLocationService _downloadLocation = Substitute.For<IDownloadLocationService>();
    private readonly IDownloadLocationNotifier _downloadLocationNotifier = Substitute.For<IDownloadLocationNotifier>();

    public SettingsViewModelTests(WpfApplicationFixture _) { }

    private SettingsViewModel CreateSut(AppSettings? initial = null, bool startupEnabled = false)
    {
        var settings = initial ?? new AppSettings();
        _settingsService.Load().Returns(settings);
        _startupService.IsEnabled().Returns(startupEnabled);
        _startupService.Enable().Returns(true);
        _startupService.Disable().Returns(true);
        return new SettingsViewModel(_settingsService, _startupService, _globalViewModel, _updateService,
            _downloadLocation, _downloadLocationNotifier);
    }

    [Fact]
    public void Constructor_LoadsValuesFromSettingsService()
    {
        var stored = new AppSettings
        {
            Language = AppLanguage.Eng,
            Theme = AppTheme.Empoleon,
            StartMinimized = true,
            AutoUpdate = true,
            DownloadDirectory = @"C:\games"
        };

        var vm = CreateSut(stored, startupEnabled: true);

        vm.Language.ShouldBe(AppLanguage.Eng);
        vm.Theme.ShouldBe(AppTheme.Empoleon);
        vm.StartMinimized.ShouldBeTrue();
        vm.AutoUpdate.ShouldBeTrue();
        vm.DownloadDirectory.ShouldBe(@"C:\games");
        vm.StartWithWindows.ShouldBeTrue();
    }

    [Fact]
    public void Constructor_EnsuresGamesRootDirectoryExists()
    {
        _ = CreateSut();

        _settingsService.Received(1).EnsureGamesRootDirectoryExists();
    }

    [Fact]
    public void Constructor_DoesNotPersistDuringInitialLoad()
    {
        _ = CreateSut(new AppSettings { Language = AppLanguage.Cat });

        _settingsService.DidNotReceive().Save(Arg.Any<AppSettings>());
    }

    [Fact]
    public void Constructor_AssignsItselfToInstanceSingleton()
    {
        var vm = CreateSut();

        SettingsViewModel.Instance.ShouldBeSameAs(vm);
    }

    [Fact]
    public void ResolveInstance_BeforeConstruction_ThrowsDiagnosticInsteadOfReturningNull()
    {
        var ex = Should.Throw<InvalidOperationException>(() => SettingsViewModel.ResolveInstance(null));
        ex.Message.ShouldContain(nameof(SettingsViewModel.Instance));
    }

    [Fact]
    public void ResolveInstance_AfterConstruction_ReturnsTheSameInstance()
    {
        var vm = CreateSut();

        SettingsViewModel.ResolveInstance(vm).ShouldBeSameAs(vm);
    }

    [Theory]
    [InlineData(AppLanguage.Eng, typeof(Eng))]
    [InlineData(AppLanguage.Cat, typeof(Cat))]
    [InlineData(AppLanguage.Eus, typeof(Eus))]
    [InlineData(AppLanguage.Gal, typeof(Gal))]
    [InlineData(AppLanguage.Por, typeof(Por))]
    [InlineData(AppLanguage.Val, typeof(Val))]
    [InlineData(AppLanguage.Fra, typeof(Fra))]
    [InlineData(AppLanguage.Esp, typeof(Esp))]
    public void Language_WhenChanged_ReplacesStringsWithMatchingResource(AppLanguage lang, Type expectedStringsType)
    {
        var vm = CreateSut();

        vm.Language = lang;

        vm.Strings.ShouldBeOfType(expectedStringsType);
    }

    [Fact]
    public void Language_WhenChanged_PersistsSettings()
    {
        var vm = CreateSut();
        _settingsService.ClearReceivedCalls();

        vm.Language = AppLanguage.Eng;

        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s!.Language == AppLanguage.Eng));
    }

    [Fact]
    public void Language_WhenChanged_RaisesPropertyChangedForBothLanguageAndStrings()
    {
        var vm = CreateSut();
        using var recorder = new PropertyChangedRecorder(vm);

        vm.Language = AppLanguage.Eng;

        recorder.WasRaised(nameof(SettingsViewModel.Language)).ShouldBeTrue();
        recorder.WasRaised(nameof(SettingsViewModel.Strings)).ShouldBeTrue();
    }

    [Fact]
    public void StartWithWindows_WhenSetToTrueAfterLoad_EnablesStartupService()
    {
        var vm = CreateSut(startupEnabled: false);

        vm.StartWithWindows = true;

        _startupService.Received(1).Enable();
    }

    [Fact]
    public void StartWithWindows_WhenSetToFalseAfterLoad_DisablesStartupService()
    {
        var vm = CreateSut(startupEnabled: true);

        vm.StartWithWindows = false;

        _startupService.Received(1).Disable();
    }

    [Fact]
    public void StartWithWindows_WhenEnableFails_RevertsToggleToRealState()
    {
        var vm = CreateSut(startupEnabled: false);
        _startupService.Enable().Returns(false);
        _startupService.IsEnabled().Returns(false);

        vm.StartWithWindows = true;

        _startupService.Received(1).Enable();
        vm.StartWithWindows.ShouldBeFalse();
    }

    [Fact]
    public void StartWithWindows_WhenDisableFails_RevertsToggleToRealState()
    {
        var vm = CreateSut(startupEnabled: true);
        _startupService.Disable().Returns(false);
        _startupService.IsEnabled().Returns(true);

        vm.StartWithWindows = false;

        _startupService.Received(1).Disable();
        vm.StartWithWindows.ShouldBeTrue();
    }

    [Fact]
    public void StartWithWindows_WhenEnableSucceeds_KeepsToggleOnAndPersists()
    {
        var vm = CreateSut(startupEnabled: false);
        _startupService.Enable().Returns(true);
        _settingsService.ClearReceivedCalls();

        vm.StartWithWindows = true;

        vm.StartWithWindows.ShouldBeTrue();
        _settingsService.Received(1).Save(Arg.Any<AppSettings>());
    }

    [Fact]
    public void StartWithWindows_DuringInitialLoad_DoesNotInvokeStartupService()
    {
        _ = CreateSut(startupEnabled: true);

        _startupService.DidNotReceive().Enable();
        _startupService.DidNotReceive().Disable();
    }

    [Fact]
    public void StartMinimized_WhenChanged_PersistsSettings()
    {
        var vm = CreateSut();
        _settingsService.ClearReceivedCalls();

        vm.StartMinimized = true;

        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s!.StartMinimized));
    }

    [Fact]
    public void AutoUpdate_WhenChanged_PersistsSettings()
    {
        var vm = CreateSut();
        _settingsService.ClearReceivedCalls();

        vm.AutoUpdate = true;

        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s!.AutoUpdate));
    }

    [Fact]
    public void DownloadDirectory_WhenChanged_PersistsAndEnsuresDirectoryExists()
    {
        var vm = CreateSut();
        _settingsService.ClearReceivedCalls();

        vm.DownloadDirectory = @"D:\NewFolder";

        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s!.DownloadDirectory == @"D:\NewFolder"));
        _settingsService.Received(1).EnsureGamesRootDirectoryExists();
    }

    [Fact]
    public void Constructor_PublishesTheGamesRootDirectory()
    {
        _settingsService.GetGamesRootDirectory().Returns(@"D:\Games\LostieLauncher");

        var vm = CreateSut(new AppSettings { DownloadDirectory = @"D:\Games" });

        vm.GamesRootDirectory.ShouldBe(@"D:\Games\LostieLauncher");
    }

    [Fact]
    public void DownloadDirectory_WhenChanged_RepublishesTheGamesRoot()
    {
        _settingsService.GetGamesRootDirectory().Returns(@"C:\Old\LostieLauncher");
        var vm = CreateSut();
        _settingsService.GetGamesRootDirectory().Returns(@"D:\New\LostieLauncher");

        vm.DownloadDirectory = @"D:\New";

        vm.GamesRootDirectory.ShouldBe(@"D:\New\LostieLauncher");
    }

    [Fact]
    public void Constructor_WhenTheStoredLibrarySitsUnderOneDrive_RaisesTheWarningFlag()
    {
        var root = @"C:\Users\someone\OneDrive\Documentos\LostieLauncher";
        _settingsService.GetGamesRootDirectory().Returns(root);
        _downloadLocation.IsOneDriveSynced(root).Returns(true);

        var vm = CreateSut(new AppSettings { DownloadDirectory = @"C:\Users\someone\OneDrive\Documentos" });

        vm.IsDownloadDirectoryOneDriveSynced.ShouldBeTrue();
        vm.DownloadDirectory.ShouldBe(@"C:\Users\someone\OneDrive\Documentos");
        _settingsService.DidNotReceive().Save(Arg.Any<AppSettings>());
    }

    [Fact]
    public void Constructor_WhenTheStoredLibraryIsNotSynced_LeavesTheWarningFlagDown()
    {
        _settingsService.GetGamesRootDirectory().Returns(@"C:\Users\someone\AppData\Local\LostieLauncher");
        _downloadLocation.IsOneDriveSynced(Arg.Any<string>()).Returns(false);

        var vm = CreateSut();

        vm.IsDownloadDirectoryOneDriveSynced.ShouldBeFalse();
    }

    [Fact]
    public void MarkWelcomeSeen_PersistsHasSeenWelcomeAsTrue()
    {
        var vm = CreateSut(new AppSettings { HasSeenWelcome = false });
        _settingsService.ClearReceivedCalls();

        vm.MarkWelcomeSeen();

        vm.HasSeenWelcome.ShouldBeTrue();
        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s!.HasSeenWelcome));
    }

    [Fact]
    public void HasSeenWelcome_WhenInitialSettingsHadFlagSet_ReflectsStoredValue()
    {
        var vm = CreateSut(new AppSettings { HasSeenWelcome = true });

        var seen = vm.HasSeenWelcome;

        seen.ShouldBeTrue();
    }

    [Fact]
    public async Task CheckForUpdates_WhenDownloadInProgress_NotifiesUserAndDoesNotCheck()
    {
        var vm = CreateSut();
        _globalViewModel.IsDownloading = true;

        await vm.CheckForUpdatesCommand.ExecuteAsync(null);

        _updateService.Received(1).NotifyDownloadInProgress();
        await _updateService.DidNotReceive().CheckForUpdatesAsync(Arg.Any<bool>());
    }

    [Fact]
    public async Task CheckForUpdates_WhenNotDownloading_DelegatesToUpdateServiceNotifyingWhenUpToDate()
    {
        var vm = CreateSut();
        _globalViewModel.IsDownloading = false;

        await vm.CheckForUpdatesCommand.ExecuteAsync(null);

        await _updateService.Received(1).CheckForUpdatesAsync(true);
        _updateService.DidNotReceive().NotifyDownloadInProgress();
    }

    [Theory]
    [InlineData("0.9.3", "v0.9.3")]
    [InlineData("0.9.3-pre.2", "v0.9.3-pre.2")]
    public void FormatVersion_PackedVersion_PrefixesV(string version, string expected)
    {
        var formatted = SettingsViewModel.FormatVersion(version);

        formatted.ShouldBe(expected);
    }

    [Fact]
    public void FormatVersion_NullVersion_ReturnsUnknownFallback()
    {
        var formatted = SettingsViewModel.FormatVersion(null);

        formatted.ShouldBe("Unknown");
    }

    [Fact]
    public void CurrentVersion_BuiltAssembly_HasNoCommitHash()
    {
        var current = SettingsViewModel.CurrentVersion;

        current.ShouldMatch(@"^v\d+\.\d+\.\d+(-pre\.\d+)?$");
    }
}
