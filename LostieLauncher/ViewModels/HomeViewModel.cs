using CommunityToolkit.Mvvm.ComponentModel;
using LostieLauncher.Models;
using LostieLauncher.Services;
using System.Collections.ObjectModel;
using System.ComponentModel;

namespace LostieLauncher.ViewModels;

public partial class HomeViewModel : ObservableObject, IDisposable
{
    private static readonly TimeSpan DefaultBackgroundRefreshInterval = TimeSpan.FromMinutes(2);
    private readonly IContentService _contentService;
    private readonly SettingsViewModel _settingsViewModel;
    private readonly SemaphoreSlim _refreshGate = new(1, 1);
    private readonly TimeSpan _backgroundRefreshInterval;
    private readonly CancellationTokenSource _backgroundRefreshCts = new();
    private bool _disposed;

    // Tarea del bucle de refresco en segundo plano; expuesta como internal solo para que las pruebas
    // puedan esperar su terminación tras Dispose (el bucle dejó de ser imparable, cf. BUG-019).
    internal Task BackgroundRefreshTask { get; }

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
        : this(contentService, settingsViewModel, DefaultBackgroundRefreshInterval)
    {
    }

    internal HomeViewModel(IContentService contentService, SettingsViewModel settingsViewModel, TimeSpan backgroundRefreshInterval)
    {
        _contentService = contentService;
        _settingsViewModel = settingsViewModel;
        _backgroundRefreshInterval = backgroundRefreshInterval;
        _settingsViewModel.PropertyChanged += OnSettingsPropertyChanged;
        _ = LoadHomeContentAsync();
        BackgroundRefreshTask = RefreshHomeContentPeriodicallyAsync(_backgroundRefreshCts.Token);
    }

    public void Dispose()
    {
        if (_disposed) return;
        _disposed = true;
        _settingsViewModel.PropertyChanged -= OnSettingsPropertyChanged;
        _backgroundRefreshCts.Cancel();
        _backgroundRefreshCts.Dispose();
        GC.SuppressFinalize(this);
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
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
        finally
        {
            if (showLoading) IsLoading = false;
            _refreshGate.Release();
        }
    }

    private async Task RefreshHomeContentPeriodicallyAsync(CancellationToken ct)
    {
        try
        {
            using var timer = new PeriodicTimer(_backgroundRefreshInterval);

            while (await timer.WaitForNextTickAsync(ct))
            {
                // LoadHomeContentAsync nunca lanza (try/catch/finally propio), así que el bucle no necesita
                // un catch por iteración; el único throw esperable aquí es la OCE de la cancelación al disponer.
                await LoadHomeContentAsync(forceRefresh: true, showLoading: false);
            }
        }
        catch (OperationCanceledException)
        {
            // Cancelación esperada al disponer el ViewModel: el bucle termina limpiamente.
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
        }
    }

}
