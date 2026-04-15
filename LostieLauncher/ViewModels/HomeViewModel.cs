using System.Collections.ObjectModel;
using System.ComponentModel;
using CommunityToolkit.Mvvm.ComponentModel;
using LostieLauncher.Models;
using LostieLauncher.Services;

namespace LostieLauncher.ViewModels;

public partial class HomeViewModel : ObservableObject
{
    private readonly IContentService _contentService;
    private readonly SettingsViewModel _settingsViewModel;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    [NotifyPropertyChangedFor(nameof(IsNewsEmpty))]
    public partial ObservableCollection<NewsItem> News { get; set; } = [];

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    [NotifyPropertyChangedFor(nameof(IsNotificationsEmpty))]
    public partial ObservableCollection<NotificationItem> Notifications { get; set; } = [];

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    [NotifyPropertyChangedFor(nameof(IsNewsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsNotificationsEmpty))]
    public partial bool IsLoading { get; set; }

    public bool IsEmpty => !IsLoading && News.Count == 0 && Notifications.Count == 0;
    public bool IsListVisible => !IsLoading && (News.Count > 0 || Notifications.Count > 0);
    public bool IsNewsEmpty => !IsLoading && News.Count == 0;
    public bool IsNotificationsEmpty => !IsLoading && Notifications.Count == 0;

    public HomeViewModel(IContentService contentService, SettingsViewModel settingsViewModel)
    {
        _contentService = contentService;
        _settingsViewModel = settingsViewModel;
        _settingsViewModel.PropertyChanged += OnSettingsPropertyChanged;
        _ = LoadHomeContentAsync();
    }

    private void OnSettingsPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(SettingsViewModel.Language)) _ = LoadHomeContentAsync();
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
