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

    public SettingsViewModelTests(WpfApplicationFixture _) { /* fixture ensures Application.Current */ }

    private SettingsViewModel CreateSut(AppSettings? initial = null, bool startupEnabled = false)
    {
        var settings = initial ?? new AppSettings();
        _settingsService.Load().Returns(settings);
        _startupService.IsEnabled().Returns(startupEnabled);
        return new SettingsViewModel(_settingsService, _startupService);
    }

    // -------------------- LoadSettings (constructor) --------------------

    [Fact]
    public void Constructor_LoadsValuesFromSettingsService()
    {
        // Arrange
        var stored = new AppSettings
        {
            Language = AppLanguage.Eng,
            Theme = AppTheme.Empoleon,
            StartMinimized = true,
            AutoUpdate = true,
            DownloadDirectory = @"C:\games"
        };

        // Act
        var vm = CreateSut(stored, startupEnabled: true);

        // Assert
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
        // Arrange & Act
        _ = CreateSut();

        // Assert
        _settingsService.Received(1).EnsureGamesRootDirectoryExists();
    }

    [Fact]
    public void Constructor_DoesNotPersistDuringInitialLoad()
    {
        // Arrange & Act — populating the VM from disk should never trigger a Save round-trip.
        _ = CreateSut(new AppSettings { Language = AppLanguage.Cat });

        // Assert
        _settingsService.DidNotReceive().Save(Arg.Any<AppSettings>());
    }

    [Fact]
    public void Constructor_AssignsItselfToInstanceSingleton()
    {
        // Arrange & Act
        var vm = CreateSut();

        // Assert — production code (e.g. dialogs) reads SettingsViewModel.Instance.
        SettingsViewModel.Instance.ShouldBeSameAs(vm);
    }

    // -------------------- Language --------------------

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
        // Arrange
        var vm = CreateSut();

        // Act
        vm.Language = lang;

        // Assert
        vm.Strings.ShouldBeOfType(expectedStringsType);
    }

    [Fact]
    public void Language_WhenChanged_PersistsSettings()
    {
        // Arrange
        var vm = CreateSut();
        _settingsService.ClearReceivedCalls();

        // Act
        vm.Language = AppLanguage.Eng;

        // Assert
        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s.Language == AppLanguage.Eng));
    }

    [Fact]
    public void Language_WhenChanged_RaisesPropertyChangedForBothLanguageAndStrings()
    {
        // Arrange
        var vm = CreateSut();
        using var recorder = new PropertyChangedRecorder(vm);

        // Act
        vm.Language = AppLanguage.Eng;

        // Assert
        recorder.WasRaised(nameof(SettingsViewModel.Language)).ShouldBeTrue();
        recorder.WasRaised(nameof(SettingsViewModel.Strings)).ShouldBeTrue();
    }

    // -------------------- StartWithWindows --------------------

    [Fact]
    public void StartWithWindows_WhenSetToTrueAfterLoad_EnablesStartupService()
    {
        // Arrange
        var vm = CreateSut(startupEnabled: false);

        // Act
        vm.StartWithWindows = true;

        // Assert
        _startupService.Received(1).Enable();
    }

    [Fact]
    public void StartWithWindows_WhenSetToFalseAfterLoad_DisablesStartupService()
    {
        // Arrange
        var vm = CreateSut(startupEnabled: true);

        // Act
        vm.StartWithWindows = false;

        // Assert
        _startupService.Received(1).Disable();
    }

    [Fact]
    public void StartWithWindows_DuringInitialLoad_DoesNotInvokeStartupService()
    {
        // Arrange & Act — the constructor reads IsEnabled() and assigns the property; that
        // assignment must NOT trigger Enable/Disable (that's the user's job).
        _ = CreateSut(startupEnabled: true);

        // Assert
        _startupService.DidNotReceive().Enable();
        _startupService.DidNotReceive().Disable();
    }

    // -------------------- Boolean toggles --------------------

    [Fact]
    public void StartMinimized_WhenChanged_PersistsSettings()
    {
        // Arrange
        var vm = CreateSut();
        _settingsService.ClearReceivedCalls();

        // Act
        vm.StartMinimized = true;

        // Assert
        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s.StartMinimized));
    }

    [Fact]
    public void AutoUpdate_WhenChanged_PersistsSettings()
    {
        // Arrange
        var vm = CreateSut();
        _settingsService.ClearReceivedCalls();

        // Act
        vm.AutoUpdate = true;

        // Assert
        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s.AutoUpdate));
    }

    // -------------------- DownloadDirectory --------------------

    [Fact]
    public void DownloadDirectory_WhenChanged_PersistsAndEnsuresDirectoryExists()
    {
        // Arrange
        var vm = CreateSut();
        _settingsService.ClearReceivedCalls();

        // Act
        vm.DownloadDirectory = @"D:\NewFolder";

        // Assert
        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s.DownloadDirectory == @"D:\NewFolder"));
        _settingsService.Received(1).EnsureGamesRootDirectoryExists();
    }

    // -------------------- MarkWelcomeSeen --------------------

    [Fact]
    public void MarkWelcomeSeen_PersistsHasSeenWelcomeAsTrue()
    {
        // Arrange — initial settings indicate user has NOT seen the welcome dialog yet.
        var vm = CreateSut(new AppSettings { HasSeenWelcome = false });
        _settingsService.ClearReceivedCalls();

        // Act
        vm.MarkWelcomeSeen();

        // Assert
        vm.HasSeenWelcome.ShouldBeTrue();
        _settingsService.Received(1).Save(Arg.Is<AppSettings>(s => s.HasSeenWelcome));
    }

    [Fact]
    public void HasSeenWelcome_WhenInitialSettingsHadFlagSet_ReflectsStoredValue()
    {
        // Arrange
        var vm = CreateSut(new AppSettings { HasSeenWelcome = true });

        // Act
        var seen = vm.HasSeenWelcome;

        // Assert
        seen.ShouldBeTrue();
    }
}
