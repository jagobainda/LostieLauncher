using CommunityToolkit.Mvvm.ComponentModel;
using LostieLauncher.Models;
using LostieLauncher.Services;
using System.Collections.ObjectModel;
using System.ComponentModel;

namespace LostieLauncher.ViewModels;

public partial class HomeViewModel : ObservableObject
{
    private static readonly TimeSpan BackgroundRefreshInterval = TimeSpan.FromMinutes(2);
    private readonly IContentService _contentService;
    private readonly SettingsViewModel _settingsViewModel;
    private readonly SemaphoreSlim _refreshGate = new(1, 1);

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

    [ObservableProperty]
    public partial bool IsOfflineMode { get; set; }

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
        _ = RefreshHomeContentPeriodicallyAsync();
    }

    private void OnSettingsPropertyChanged(object? sender, PropertyChangedEventArgs e)
    {
        if (e.PropertyName == nameof(SettingsViewModel.Language)) _ = LoadHomeContentAsync();
    }

    public async Task RefreshAsync() => await LoadHomeContentAsync(forceRefresh: true);

    private async Task LoadHomeContentAsync(bool forceRefresh = false, bool showLoading = true)
    {
        await _refreshGate.WaitAsync();

        try
        {
            if (showLoading) IsLoading = true;

            var offlineMode = await _contentService.IsServerActionBlockedAsync(forceRefresh);
            var content = await _contentService.GetHomeContentAsync(forceRefresh);

            News = new ObservableCollection<NewsItem>(content.News);
            Notifications = new ObservableCollection<NotificationItem>(content.Notifications);
            IsOfflineMode = offlineMode;

            Logs.DebugLogManager($"Home content loaded: {content.News.Count} news, {content.Notifications.Count} notifications. Offline mode: {offlineMode}.");
        }
        finally
        {
            if (showLoading) IsLoading = false;
            _refreshGate.Release();
        }
    }

    private async Task RefreshHomeContentPeriodicallyAsync()
    {
        using var timer = new PeriodicTimer(BackgroundRefreshInterval);

        while (await timer.WaitForNextTickAsync())
        {
            try
            {
                await LoadHomeContentAsync(forceRefresh: true, showLoading: false);
            }
            catch (Exception ex)
            {
                Logs.ErrorLogManager(ex);
            }
        }
    }

}
