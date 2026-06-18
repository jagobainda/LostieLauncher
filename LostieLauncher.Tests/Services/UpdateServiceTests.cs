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

    // -------------------- Constructor guards --------------------

    [Fact]
    public void Constructor_NullGateway_Throws()
    {
        Should.Throw<ArgumentNullException>(() => new UpdateService(null!, _notifier));
    }

    [Fact]
    public void Constructor_NullNotifier_Throws()
    {
        Should.Throw<ArgumentNullException>(() => new UpdateService(_gateway, null!));
    }

    // -------------------- Up to date --------------------

    [Fact]
    public async Task CheckForUpdates_WhenUpToDateAndNotifyRequested_NotifiesUpToDate()
    {
        // Arrange — gateway reports no pending update.
        _gateway.CheckForUpdatesAsync().Returns((IUpdatePackage?)null);
        var sut = CreateSut();

        // Act — a user-initiated check asks to be told when already current.
        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        // Assert
        _notifier.Received(1).NotifyUpToDate();
        _notifier.DidNotReceive().NotifyCheckFailed();
    }

    [Fact]
    public async Task CheckForUpdates_WhenUpToDateAndNotifyNotRequested_StaysSilent()
    {
        // Arrange — background check (notifyWhenUpToDate: false) must never pop dialogs.
        _gateway.CheckForUpdatesAsync().Returns((IUpdatePackage?)null);
        var sut = CreateSut();

        // Act
        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: false);

        // Assert
        _notifier.DidNotReceive().NotifyUpToDate();
    }

    // -------------------- Failure feedback (the regression this fix targets) --------------------

    [Fact]
    public async Task CheckForUpdates_WhenCheckThrowsAndNotifyRequested_NotifiesFailure()
    {
        // Arrange — Velopack throws (e.g. network error, or app not installed in a dev build).
        _gateway.CheckForUpdatesAsync().Returns<IUpdatePackage?>(_ => throw new InvalidOperationException("boom"));
        var sut = CreateSut();

        // Act — a manual check must NOT fail silently (the original bug).
        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        // Assert
        _notifier.Received(1).NotifyCheckFailed();
        _notifier.DidNotReceive().NotifyUpToDate();
    }

    [Fact]
    public async Task CheckForUpdates_WhenCheckThrowsAndNotifyNotRequested_StaysSilent()
    {
        // Arrange — a background check that throws should swallow the error without UI.
        _gateway.CheckForUpdatesAsync().Returns<IUpdatePackage?>(_ => throw new InvalidOperationException("boom"));
        var sut = CreateSut();

        // Act
        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: false);

        // Assert
        _notifier.DidNotReceive().NotifyCheckFailed();
    }

    [Fact]
    public async Task CheckForUpdates_WhenDownloadThrowsAndNotifyRequested_NotifiesFailure()
    {
        // Arrange — the check succeeds but the download fails mid-way.
        var update = StubUpdate();
        update.DownloadAsync().Returns(Task.FromException(new IOException("disk full")));
        _gateway.CheckForUpdatesAsync().Returns(update);
        var sut = CreateSut();

        // Act
        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        // Assert — failure still reaches the user; no apply attempt.
        _notifier.Received(1).NotifyCheckFailed();
        update.DidNotReceive().ApplyAndRestart();
    }

    // -------------------- Update available --------------------

    [Fact]
    public async Task CheckForUpdates_WhenUpdateAvailableAndUserAccepts_DownloadsAndApplies()
    {
        // Arrange — an update exists and the user confirms the prompt.
        var update = StubUpdate("2.0.0");
        _gateway.CheckForUpdatesAsync().Returns(update);
        _notifier.PromptApply("2.0.0").Returns(true);
        var sut = CreateSut();

        // Act
        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        // Assert
        await update.Received(1).DownloadAsync();
        update.Received(1).ApplyAndRestart();
        _notifier.DidNotReceive().NotifyUpToDate();
        _notifier.DidNotReceive().NotifyCheckFailed();
    }

    [Fact]
    public async Task CheckForUpdates_WhenUpdateAvailableAndUserDeclines_DownloadsButDoesNotApply()
    {
        // Arrange — an update exists but the user declines restarting now.
        var update = StubUpdate("2.0.0");
        _gateway.CheckForUpdatesAsync().Returns(update);
        _notifier.PromptApply("2.0.0").Returns(false);
        var sut = CreateSut();

        // Act
        await sut.CheckForUpdatesAsync(notifyWhenUpToDate: true);

        // Assert — download happens eagerly, but nothing is applied without consent.
        await update.Received(1).DownloadAsync();
        update.DidNotReceive().ApplyAndRestart();
    }

    // -------------------- NotifyDownloadInProgress --------------------

    [Fact]
    public void NotifyDownloadInProgress_DelegatesToNotifier()
    {
        // Arrange
        var sut = CreateSut();

        // Act
        sut.NotifyDownloadInProgress();

        // Assert
        _notifier.Received(1).NotifyDownloadInProgress();
    }
}
