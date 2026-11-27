using LostieLauncher.Services;

namespace LostieLauncher.Tests.Services;

public class UpdateServiceTests
{
    private readonly IUpdateGateway _gateway = Substitute.For<IUpdateGateway>();
    private readonly IUpdateNotifier _notifier = Substitute.For<IUpdateNotifier>();

    private UpdateService CreateSut() => new(_gateway, _notifier);

    private IUpdatePackage StubUpdate(string version = "1.2.3")
    {
        var update = Substitute.For<IUpdatePackage>();
        update.Version.Returns(version);
        update.DownloadAsync().Returns(Task.CompletedTask);
        return update;
    }

    [Fact]
    public void Constructor_NullGateway_Throws() => Should.Throw<ArgumentNullException>(() => new UpdateService(null!, _notifier));

    [Fact]
    public void Constructor_NullNotifier_Throws() => Should.Throw<ArgumentNullException>(() => new UpdateService(_gateway, null!));

    [Fact]
    public async Task CheckForUpdates_WhenUpToDateAndNotifyRequested_NotifiesUpToDate()
    {
        _gateway.CheckForUpdatesAsync().Returns((IUpdatePackage?)null);
        var sut = CreateSut();

        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        _notifier.Received(1).NotifyUpToDate();
        _notifier.DidNotReceive().NotifyCheckFailed();
    }

    [Fact]
    public async Task CheckForUpdates_WhenUpToDateAndNotifyNotRequested_StaysSilent()
    {
        _gateway.CheckForUpdatesAsync().Returns((IUpdatePackage?)null);
        var sut = CreateSut();

        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: false);

        _notifier.DidNotReceive().NotifyUpToDate();
    }

    [Fact]
    public async Task CheckForUpdates_WhenCheckThrowsAndNotifyRequested_NotifiesFailure()
    {
        _gateway.CheckForUpdatesAsync().Returns<IUpdatePackage?>(_ => throw new InvalidOperationException("boom"));
        var sut = CreateSut();

        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        _notifier.Received(1).NotifyCheckFailed();
        _notifier.DidNotReceive().NotifyUpToDate();
    }

    [Fact]
    public async Task CheckForUpdates_WhenCheckThrowsAndNotifyNotRequested_StaysSilent()
    {
        _gateway.CheckForUpdatesAsync().Returns<IUpdatePackage?>(_ => throw new InvalidOperationException("boom"));
        var sut = CreateSut();

        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: false);

        _notifier.DidNotReceive().NotifyCheckFailed();
    }

    [Fact]
    public async Task CheckForUpdates_WhenDownloadThrowsAndNotifyRequested_NotifiesFailure()
    {
        var update = StubUpdate();
        update.DownloadAsync().Returns(Task.FromException(new IOException("disk full")));
        _gateway.CheckForUpdatesAsync().Returns(update);
        var sut = CreateSut();

        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        _notifier.Received(1).NotifyCheckFailed();
        update.DidNotReceive().ApplyAndRestart();
    }

    [Fact]
    public async Task CheckForUpdates_WhenUpdateAvailableAndUserAccepts_DownloadsAndApplies()
    {
        var update = StubUpdate("2.0.0");
        _gateway.CheckForUpdatesAsync().Returns(update);
        _notifier.PromptApply("2.0.0").Returns(true);
        var sut = CreateSut();

        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        await update.Received(1).DownloadAsync();
        update.Received(1).ApplyAndRestart();
        _notifier.DidNotReceive().NotifyUpToDate();
        _notifier.DidNotReceive().NotifyCheckFailed();
    }

    [Fact]
    public async Task CheckForUpdates_WhenUpdateAvailableAndUserDeclines_DownloadsButDoesNotApply()
    {
        var update = StubUpdate("2.0.0");
        _gateway.CheckForUpdatesAsync().Returns(update);
        _notifier.PromptApply("2.0.0").Returns(false);
        var sut = CreateSut();

        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        await update.Received(1).DownloadAsync();
        update.DidNotReceive().ApplyAndRestart();
    }

    [Fact]
    public void NotifyDownloadInProgress_DelegatesToNotifier()
    {
        var sut = CreateSut();

        sut.NotifyDownloadInProgress();

        _notifier.Received(1).NotifyDownloadInProgress();
    }
}
