using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.ViewModels;
using System.Collections.ObjectModel;

namespace LostieLauncher.Tests.ViewModels;

[Collection(WpfCollection.Name)]
public class HomeViewModelTests
{
    private readonly IContentService _contentService = Substitute.For<IContentService>();
    private readonly ISettingsService _settingsService = Substitute.For<ISettingsService>();
    private readonly IWindowsStartupService _startupService = Substitute.For<IWindowsStartupService>();

    public HomeViewModelTests(WpfApplicationFixture _) => _settingsService.Load().Returns(new AppSettings());

    private SettingsViewModel CreateSettings() => new(_settingsService, _startupService);

    private static HomeContent SampleContent() => new()
    {
        News = [new NewsItem { Id = Guid.NewGuid(), Title = "N1" }],
        Notifications = [new NotificationItem { Id = Guid.NewGuid(), Title = "Note" }]
    };

    [Fact]
    public async Task Constructor_KicksOffInitialLoad_PopulatingNewsAndNotifications()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(SampleContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();

        // Act
        var vm = new HomeViewModel(_contentService, settings);
        await vm.RefreshAsync(); // serialised via the same gate, so the initial load completes first.

        // Assert
        vm.News.Count.ShouldBe(1);
        vm.Notifications.Count.ShouldBe(1);
        vm.IsLoading.ShouldBeFalse();
    }

    [Fact]
    public async Task Constructor_PropagatesOfflineMode_FromContentService()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(true);
        var settings = CreateSettings();

        // Act
        var vm = new HomeViewModel(_contentService, settings);
        await vm.RefreshAsync();

        // Assert
        vm.IsOfflineMode.ShouldBeTrue();
    }

    [Fact]
    public async Task RefreshAsync_RequestsForcedRefresh_FromContentService()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(SampleContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, settings);
        await vm.RefreshAsync();
        _contentService.ClearReceivedCalls();

        // Act
        await vm.RefreshAsync();

        // Assert — RefreshAsync must always pass forceRefresh=true.
        await _contentService.Received(1).GetHomeContentAsync(true);
    }

    [Fact]
    public async Task SettingsLanguageChange_TriggersAdditionalLoadHomeContent()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, settings);
        await vm.RefreshAsync();
        var before = _contentService.ReceivedCalls().Count(c => c.GetMethodInfo().Name == nameof(IContentService.GetHomeContentAsync));

        // Act
        settings.Language = AppLanguage.Eng;
        await vm.RefreshAsync(); // drains the fire-and-forget reload via the gate.

        // Assert
        var after = _contentService.ReceivedCalls().Count(c => c.GetMethodInfo().Name == nameof(IContentService.GetHomeContentAsync));
        (after - before).ShouldBeGreaterThanOrEqualTo(2); // at least the language reload + our explicit refresh.
    }

    [Fact]
    public async Task IsEmpty_WhenNoNewsOrNotificationsAndNotLoading_ReturnsTrue()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, settings);
        await vm.RefreshAsync();

        // Act
        var empty = vm.IsEmpty;

        // Assert
        empty.ShouldBeTrue();
        vm.IsListVisible.ShouldBeFalse();
    }

    [Fact]
    public async Task IsListVisible_WhenContentArrived_ReturnsTrueAndIsEmptyIsFalse()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(SampleContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, settings);
        await vm.RefreshAsync();

        // Act & Assert
        vm.IsListVisible.ShouldBeTrue();
        vm.IsEmpty.ShouldBeFalse();
    }

    [Fact]
    public void News_WhenReplaced_RaisesPropertyChangedForDerivedFlags()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, settings);
        using var recorder = new PropertyChangedRecorder(vm);

        // Act
        vm.News = new ObservableCollection<NewsItem> { new() { Title = "x" } };

        // Assert
        recorder.WasRaised(nameof(HomeViewModel.News)).ShouldBeTrue();
        recorder.WasRaised(nameof(HomeViewModel.IsEmpty)).ShouldBeTrue();
        recorder.WasRaised(nameof(HomeViewModel.IsListVisible)).ShouldBeTrue();
        recorder.WasRaised(nameof(HomeViewModel.IsNewsEmpty)).ShouldBeTrue();
    }

    [Fact]
    public async Task Dispose_StopsTheBackgroundRefreshLoop()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        // Intervalo por defecto (2 min): el bucle queda esperando el primer tick, así que solo puede
        // terminar por la cancelación de Dispose — nunca por agotar el intervalo. Es la regresión exacta
        // de BUG-019 (antes el bucle era imparable y el `using` del timer nunca disponía).
        var vm = new HomeViewModel(_contentService, settings);

        // Act
        vm.Dispose();

        // Assert — el bucle termina limpiamente sin colgarse (WaitAsync acota el caso "imparable").
        await vm.BackgroundRefreshTask.WaitAsync(TimeSpan.FromSeconds(5));
        vm.BackgroundRefreshTask.IsCompletedSuccessfully.ShouldBeTrue();
    }

    [Fact]
    public async Task BackgroundLoop_PerformsForcedRefresh_AndStopsOnDispose()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var ticked = new TaskCompletionSource();
        // El bucle de fondo es el único que pide GetHomeContentAsync(true); la carga inicial usa forceRefresh=false.
        _contentService.When(c => c.GetHomeContentAsync(true)).Do(_ => ticked.TrySetResult());
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, settings, TimeSpan.FromMilliseconds(20));

        // Act & Assert — el bucle sigue refrescando periódicamente tras añadir la cancelación...
        await ticked.Task.WaitAsync(TimeSpan.FromSeconds(5));

        // ...y se detiene de forma determinista al disponer.
        vm.Dispose();
        await vm.BackgroundRefreshTask.WaitAsync(TimeSpan.FromSeconds(5));
        vm.BackgroundRefreshTask.IsCompletedSuccessfully.ShouldBeTrue();
    }

    [Fact]
    public async Task Dispose_StopsRespondingToSettingsLanguageChanges()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, settings);
        await vm.RefreshAsync();
        vm.Dispose();
        _contentService.ClearReceivedCalls();

        // Act — tras Dispose, un cambio de idioma ya no debe disparar una recarga (suscripción desuscrita).
        settings.Language = AppLanguage.Eng;
        await vm.RefreshAsync(); // drena por el gate cualquier recarga que el cambio hubiese encolado.

        // Assert — solo la carga de nuestro RefreshAsync explícito; el cambio de idioma no añadió ninguna.
        var loads = _contentService.ReceivedCalls().Count(c => c.GetMethodInfo().Name == nameof(IContentService.GetHomeContentAsync));
        loads.ShouldBe(1);
    }

    [Fact]
    public void Dispose_WhenCalledTwice_DoesNotThrow()
    {
        // Arrange
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, settings);
        vm.Dispose();

        // Act
        var act = vm.Dispose;

        // Assert
        Should.NotThrow(act);
    }
}
