using CommunityToolkit.Mvvm.ComponentModel;

namespace EricLostieLauncher.ViewModels;

public partial class MainViewModel : ObservableObject
{
    [ObservableProperty]
    private string _currentTitle = "Inicio";
}
