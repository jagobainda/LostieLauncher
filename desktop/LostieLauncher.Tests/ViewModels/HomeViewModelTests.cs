using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.ViewModels;
using System.Collections.ObjectModel;

namespace LostieLauncher.Tests.ViewModels;

[Collection(WpfCollection.Name)]
public class HomeViewModelTests
{
    private readonly IContentService _contentService = Substitute.For<IContentService>();
    private readonly ITwitchLiveService _twitchLiveService = Substitute.For<ITwitchLiveService>();
    private readonly ISettingsService _settingsService = Substitute.For<ISettingsService>();
    private readonly IWindowsStartupService _startupService = Substitute.For<IWindowsStartupService>();

    public HomeViewModelTests(WpfApplicationFixture _) => _settingsService.Load().Returns(new AppSettings());

    private SettingsViewModel CreateSettings() => new(_settingsService, _startupService, new GlobalViewModel(), Substitute.For<IUpdateService>(),
        Substitute.For<IDownloadLocationService>(), Substitute.For<IDownloadLocationNotifier>());

    private static HomeContent SampleContent() => new()
    {
        News = [new NewsItem { Id = Guid.NewGuid(), Title = "N1" }],
        Notifications = [new NotificationItem { Id = Guid.NewGuid(), Title = "Note" }]
    };

    [Fact]
    public async Task Constructor_KicksOffInitialLoad_PopulatingNewsAndNotifications()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(SampleContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();

        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();

        vm.News.Count.ShouldBe(1);
        vm.Notifications.Count.ShouldBe(1);
        vm.IsLoading.ShouldBeFalse();
    }

    [Theory]
    [InlineData(true)]
    [InlineData(false)]
    public async Task RefreshAsync_PropagatesTwitchLiveStatus(bool isLive)
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        _twitchLiveService.IsLiveAsync().Returns(isLive);
        var vm = new HomeViewModel(_contentService, _twitchLiveService, CreateSettings());

        await vm.RefreshAsync();

        vm.IsTwitchLive.ShouldBe(isLive);
    }

    [Fact]
    public async Task Constructor_PropagatesOfflineMode_FromContentService()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(true);
        var settings = CreateSettings();

        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();

        vm.IsOfflineMode.ShouldBeTrue();
    }

    [Fact]
    public async Task RefreshAsync_RequestsForcedRefresh_FromContentService()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(SampleContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();
        _contentService.ClearReceivedCalls();

        await vm.RefreshAsync();

        await _contentService.Received(1).GetHomeContentAsync(true);
    }

    [Fact]
    public async Task SettingsLanguageChange_TriggersAdditionalLoadHomeContent()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();
        var before = _contentService.ReceivedCalls().Count(c => c.GetMethodInfo().Name == nameof(IContentService.GetHomeContentAsync));

        settings.Language = AppLanguage.Eng;
        await vm.RefreshAsync();

        var after = _contentService.ReceivedCalls().Count(c => c.GetMethodInfo().Name == nameof(IContentService.GetHomeContentAsync));
        (after - before).ShouldBeGreaterThanOrEqualTo(2);
    }

    [Fact]
    public async Task IsEmpty_WhenNoNewsOrNotificationsAndNotLoading_ReturnsTrue()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();

        var empty = vm.IsEmpty;

        empty.ShouldBeTrue();
        vm.IsListVisible.ShouldBeFalse();
    }

    [Fact]
    public async Task IsListVisible_WhenContentArrived_ReturnsTrueAndIsEmptyIsFalse()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(SampleContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();

        vm.IsListVisible.ShouldBeTrue();
        vm.IsEmpty.ShouldBeFalse();
    }

    [Fact]
    public void News_WhenReplaced_RaisesPropertyChangedForDerivedFlags()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        using var recorder = new PropertyChangedRecorder(vm);

        vm.News = new ObservableCollection<NewsItem> { new() { Title = "x" } };

        recorder.WasRaised(nameof(HomeViewModel.News)).ShouldBeTrue();
        recorder.WasRaised(nameof(HomeViewModel.IsEmpty)).ShouldBeTrue();
        recorder.WasRaised(nameof(HomeViewModel.IsListVisible)).ShouldBeTrue();
        recorder.WasRaised(nameof(HomeViewModel.IsNewsEmpty)).ShouldBeTrue();
    }

    [Fact]
    public async Task Dispose_StopsTheBackgroundRefreshLoop()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);

        vm.Dispose();

        await vm.BackgroundRefreshTask.WaitAsync(TimeSpan.FromSeconds(5));
        vm.BackgroundRefreshTask.IsCompletedSuccessfully.ShouldBeTrue();
    }

    [Fact]
    public async Task BackgroundLoop_PerformsForcedRefresh_AndStopsOnDispose()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var ticked = new TaskCompletionSource();
        _contentService.When(c => c.GetHomeContentAsync(true)).Do(_ => ticked.TrySetResult());
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings, TimeSpan.FromMilliseconds(20));

        await ticked.Task.WaitAsync(TimeSpan.FromSeconds(5));

        vm.Dispose();
        await vm.BackgroundRefreshTask.WaitAsync(TimeSpan.FromSeconds(5));
        vm.BackgroundRefreshTask.IsCompletedSuccessfully.ShouldBeTrue();
    }

    [Fact]
    public async Task Dispose_StopsRespondingToSettingsLanguageChanges()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();
        vm.Dispose();
        _contentService.ClearReceivedCalls();

        settings.Language = AppLanguage.Eng;
        await vm.RefreshAsync();

        var loads = _contentService.ReceivedCalls().Count(c => c.GetMethodInfo().Name == nameof(IContentService.GetHomeContentAsync));
        loads.ShouldBe(1);
    }

    [Fact]
    public async Task LoadHomeContent_WhenTheServiceReportsStaleContent_RaisesTheOutOfDateWarning()
    {
        var stale = SampleContent();
        stale.IsStale = true;
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(stale);
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();

        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();

        vm.News.Count.ShouldBe(1);
        vm.IsContentStale.ShouldBeTrue();
        vm.IsOutOfDateWarningVisible.ShouldBeTrue();
    }

    [Fact]
    public async Task LoadHomeContent_WhenTheServerIsUnderMaintenance_ShowsOnlyTheOfflineBanner()
    {
        var stale = SampleContent();
        stale.IsStale = true;
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(stale);
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(true);
        var settings = CreateSettings();

        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();

        vm.IsOfflineMode.ShouldBeTrue();
        vm.IsContentStale.ShouldBeTrue();
        vm.IsOutOfDateWarningVisible.ShouldBeFalse();
    }

    [Fact]
    public async Task LoadHomeContent_WhenAFailedRefreshIsFollowedByAGoodOne_ClearsTheWarning()
    {
        var stale = SampleContent();
        stale.IsStale = true;
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(stale);
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        await vm.RefreshAsync();
        vm.IsContentStale.ShouldBeTrue();

        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(SampleContent());
        await vm.RefreshAsync();

        vm.IsContentStale.ShouldBeFalse();
        vm.IsOutOfDateWarningVisible.ShouldBeFalse();
    }

    [Fact]
    public void Dispose_WhenCalledTwice_DoesNotThrow()
    {
        _contentService.GetHomeContentAsync(Arg.Any<bool>()).Returns(new HomeContent());
        _contentService.IsServerActionBlockedAsync(Arg.Any<bool>(), Arg.Any<CancellationToken>()).Returns(false);
        var settings = CreateSettings();
        var vm = new HomeViewModel(_contentService, _twitchLiveService, settings);
        vm.Dispose();

        var act = vm.Dispose;

        Should.NotThrow(act);
    }
}
