using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;

namespace EricLostieLauncher.ViewModels;

public partial class HomeViewModel : ObservableObject
{
    private readonly IContentService _contentService;

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

    public HomeViewModel(IContentService contentService)
    {
        _contentService = contentService;
        _ = LoadHomeContentAsync();
    }

    public async Task RefreshAsync() => await LoadHomeContentAsync();

    private async Task LoadHomeContentAsync()
    {
        IsLoading = true;

        var content = await _contentService.GetHomeContentAsync();
        News = new ObservableCollection<NewsItem>(content.News);
        Notifications = new ObservableCollection<NotificationItem>(content.Notifications);

        IsLoading = false;
    }
}
