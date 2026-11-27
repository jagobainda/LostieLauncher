using LostieLauncher.ViewModels;

namespace LostieLauncher.Tests.ViewModels;

public class GlobalViewModelTests
{
    [Fact]
    public void IsBusy_DefaultsToFalse_WhenNothingIsRunning()
    {
        var vm = new GlobalViewModel();

        var busy = vm.IsBusy;

        busy.ShouldBeFalse();
    }

    [Fact]
    public void IsBusy_WhenIsDownloadingTrue_BecomesTrue()
    {
        var vm = new GlobalViewModel();

        vm.IsDownloading = true;

        vm.IsBusy.ShouldBeTrue();
    }

    [Fact]
    public void IsBusy_WhenIsRefreshingTrue_BecomesTrue()
    {
        var vm = new GlobalViewModel();

        vm.IsRefreshing = true;

        vm.IsBusy.ShouldBeTrue();
    }

    [Fact]
    public void IsDownloadingChange_RaisesPropertyChangedForIsBusy()
    {
        var vm = new GlobalViewModel();
        using var recorder = new PropertyChangedRecorder(vm);

        vm.IsDownloading = true;

        recorder.WasRaised(nameof(GlobalViewModel.IsBusy)).ShouldBeTrue();
    }

    [Fact]
    public void IsRefreshingChange_RaisesPropertyChangedForIsBusy()
    {
        var vm = new GlobalViewModel();
        using var recorder = new PropertyChangedRecorder(vm);

        vm.IsRefreshing = true;

        recorder.WasRaised(nameof(GlobalViewModel.IsBusy)).ShouldBeTrue();
    }

    [Fact]
    public void IsGameRunning_DefaultsToFalse_WhenNoGameWasLaunched()
    {
        var vm = new GlobalViewModel();

        vm.ActivePlaySessions.ShouldBe(0);
        vm.IsGameRunning.ShouldBeFalse();
    }

    [Fact]
    public void BeginPlaySession_MarksAGameAsRunning()
    {
        var vm = new GlobalViewModel();

        vm.BeginPlaySession();

        vm.ActivePlaySessions.ShouldBe(1);
        vm.IsGameRunning.ShouldBeTrue();
    }

    [Fact]
    public void EndPlaySession_AfterTheLastGameExits_ClearsIsGameRunning()
    {
        var vm = new GlobalViewModel();
        vm.BeginPlaySession();

        vm.EndPlaySession();

        vm.ActivePlaySessions.ShouldBe(0);
        vm.IsGameRunning.ShouldBeFalse();
    }

    [Fact]
    public void EndPlaySession_WithSeveralGamesOpen_KeepsIsGameRunningUntilTheLastOne()
    {
        var vm = new GlobalViewModel();
        vm.BeginPlaySession();
        vm.BeginPlaySession();

        vm.EndPlaySession();

        vm.ActivePlaySessions.ShouldBe(1);
        vm.IsGameRunning.ShouldBeTrue();
    }

    [Fact]
    public void PlaySessionChange_RaisesPropertyChangedForIsGameRunning()
    {
        var vm = new GlobalViewModel();
        using var recorder = new PropertyChangedRecorder(vm);

        vm.BeginPlaySession();

        recorder.WasRaised(nameof(GlobalViewModel.IsGameRunning)).ShouldBeTrue();
    }
}
