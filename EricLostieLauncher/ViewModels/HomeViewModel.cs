using System.Collections.ObjectModel;
using CommunityToolkit.Mvvm.ComponentModel;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;

namespace EricLostieLauncher.ViewModels;

public partial class HomeViewModel : ObservableObject
{
    private readonly IContentService _contentService;

    [ObservableProperty]
    private ObservableCollection<NewsItem> _news = [];

    [ObservableProperty]
    private ObservableCollection<NotificationItem> _notifications = [];

    [ObservableProperty]
    private bool _isLoading;

    public HomeViewModel(IContentService contentService)
    {
        _contentService = contentService;
        _ = LoadHomeContentAsync();
    }

    private async Task LoadHomeContentAsync()
    {
        IsLoading = true;

        var content = await _contentService.GetHomeContentAsync();
        News = new ObservableCollection<NewsItem>(content.News);
        Notifications = new ObservableCollection<NotificationItem>(content.Notifications);

        IsLoading = false;
    }
}
