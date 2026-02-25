using CommunityToolkit.Mvvm.ComponentModel;

namespace EricLostieLauncher.ViewModels;

public partial class GlobalViewModel : ObservableObject
{
    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsBusy))]
    private bool _isDownloading;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsBusy))]
    private bool _isRefreshing;

    public bool IsBusy => IsDownloading || IsRefreshing;
}
