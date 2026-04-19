using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.ViewModels;
using LostieLauncher.Views;
using Microsoft.Extensions.DependencyInjection;

namespace LostieLauncher.Core;

public static class DependencyInjection
{
    private const string TelemetryEndpoint = "https://ericlostie-launcher.jagoba.dev/";
    private const string ContentEndpoint = "https://ericlostie-launcher.jagoba.dev/games/listado.json";
    private const string NotificationsEndpoint = "https://cdn.jagoba.dev/ericlostie-launcher/homepage-notifications.json";
    private const string DownloadBaseUrl = "https://ericlostie-launcher.jagoba.dev/games";

    public static IServiceProvider Configure()
    {
        var services = new ServiceCollection();

        // Configuration
        services.AddSingleton(new TelemetryOptions(ApiKey: "4V7p0XSJ9C6FgCE7ae3c", Endpoint: TelemetryEndpoint));
        services.AddSingleton(new ContentOptions(ContentEndpoint: ContentEndpoint, NotificationsEndpoint: NotificationsEndpoint));
        services.AddSingleton(new DownloadOptions(BaseUrl: DownloadBaseUrl));

        // Services
        services.AddSingleton<ISettingsService, SettingsService>();
        services.AddSingleton<IWindowsStartupService, WindowsStartupService>();
        services.AddSingleton<IContentService, ContentService>();
        services.AddSingleton<IDownloadService, DownloadService>();
        services.AddSingleton<ITelemetryService, TelemetryService>();
        services.AddHttpClient("Telemetry", client => { client.Timeout = TimeSpan.FromSeconds(5); });
        services.AddHttpClient("Content", client => { client.Timeout = TimeSpan.FromSeconds(10); });
        services.AddHttpClient("Download", client => { client.Timeout = Timeout.InfiniteTimeSpan; });

        // ViewModels
        services.AddSingleton<GlobalViewModel>();
        services.AddSingleton<HomeViewModel>();
        services.AddSingleton<GamesViewModel>();
        services.AddSingleton<LibraryViewModel>();
        services.AddSingleton<SettingsViewModel>();
        services.AddSingleton<MainViewModel>();

        // Views
        services.AddSingleton<MainWindow>();

        return services.BuildServiceProvider();
    }
}
