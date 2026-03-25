using CommunityToolkit.Mvvm.ComponentModel;

namespace EricLostieLauncher.ViewModels;

public partial class GlobalViewModel : ObservableObject
{
    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsBusy))]
    public partial bool IsDownloading { get; set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsBusy))]
    public partial bool IsRefreshing { get; set; }

    public bool IsBusy => IsDownloading || IsRefreshing;
}
