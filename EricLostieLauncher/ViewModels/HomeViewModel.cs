using System.Collections.ObjectModel;
using System.ComponentModel;
using CommunityToolkit.Mvvm.ComponentModel;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;

namespace EricLostieLauncher.ViewModels;

public partial class HomeViewModel : ObservableObject
{
    private readonly IContentService _contentService;
    private readonly SettingsViewModel _settingsViewModel;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    private ObservableCollection<NewsItem> _news = [];

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    private ObservableCollection<NotificationItem> _notifications = [];

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    private bool _isLoading;

    public bool IsEmpty => !IsLoading && News.Count == 0 && Notifications.Count == 0;
    public bool IsListVisible => !IsLoading && (News.Count > 0 || Notifications.Count > 0);

    public HomeViewModel(IContentService contentService, SettingsViewModel settingsViewModel)
    {
        _contentService = contentService;
        _settingsViewModel = settingsViewModel;
        _settingsViewModel.PropertyChanged += OnSettingsPropertyChanged;
        _ = LoadHomeContentAsync();
    }

    private void OnSettingsPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(SettingsViewModel.Language))
            _ = LoadHomeContentAsync();
    }

    public async Task RefreshAsync() => await LoadHomeContentAsync(forceRefresh: true);

    private async Task LoadHomeContentAsync(bool forceRefresh = false)
    {
        IsLoading = true;

        var content = await _contentService.GetHomeContentAsync(forceRefresh);
        News = new ObservableCollection<NewsItem>(content.News);
        Notifications = new ObservableCollection<NotificationItem>(content.Notifications);

        Logs.DebugLogManager($"Home content loaded: {content.News.Count} news, {content.Notifications.Count} notifications.");

        IsLoading = false;
    }
}
