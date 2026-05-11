using LostieLauncher.ViewModels;

namespace LostieLauncher.Tests.ViewModels;

public class GlobalViewModelTests
{
    [Fact]
    public void IsBusy_DefaultsToFalse_WhenNothingIsRunning()
    {
        // Arrange
        var vm = new GlobalViewModel();

        // Act
        var busy = vm.IsBusy;

        // Assert
        busy.ShouldBeFalse();
    }

    [Fact]
    public void IsBusy_WhenIsDownloadingTrue_BecomesTrue()
    {
        // Arrange
        var vm = new GlobalViewModel();

        // Act
        vm.IsDownloading = true;

        // Assert
        vm.IsBusy.ShouldBeTrue();
    }

    [Fact]
    public void IsBusy_WhenIsRefreshingTrue_BecomesTrue()
    {
        // Arrange
        var vm = new GlobalViewModel();

        // Act
        vm.IsRefreshing = true;

        // Assert
        vm.IsBusy.ShouldBeTrue();
    }

    [Fact]
    public void IsDownloadingChange_RaisesPropertyChangedForIsBusy()
    {
        // Arrange
        var vm = new GlobalViewModel();
        using var recorder = new PropertyChangedRecorder(vm);

        // Act
        vm.IsDownloading = true;

        // Assert
        recorder.WasRaised(nameof(GlobalViewModel.IsBusy)).ShouldBeTrue();
    }

    [Fact]
    public void IsRefreshingChange_RaisesPropertyChangedForIsBusy()
    {
        // Arrange
        var vm = new GlobalViewModel();
        using var recorder = new PropertyChangedRecorder(vm);

        // Act
        vm.IsRefreshing = true;

        // Assert
        recorder.WasRaised(nameof(GlobalViewModel.IsBusy)).ShouldBeTrue();
    }
}
