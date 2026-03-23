using EricLostieLauncher.Models;
using EricLostieLauncher.Services;
using EricLostieLauncher.ViewModels;
using EricLostieLauncher.Views;
using Microsoft.Extensions.DependencyInjection;

namespace EricLostieLauncher.Core;

public static class DependencyInjection
{
    public static IServiceProvider Configure()
    {
        var services = new ServiceCollection();

        // Configuration
        services.AddSingleton(new TelemetryOptions(
            ApiKey: "4V7p0XSJ9C6FgCE7ae3c",
            Endpoint: "http://localhost:6969/launcher/api/"
        ));
        services.AddSingleton(new ContentOptions(
            Endpoint: "https://ericlostie-launcher.jagoba.dev/games/listado.json",
            NotificationsEndpoint: "https://cdn.jagoba.dev/ericlostie-launcher/homepage-notifications.json"
        ));
        services.AddSingleton(new DownloadOptions(
            BaseUrl: "https://ericlostie-launcher.jagoba.dev/games",
            KeyManagerEndpoint: "https://ericlostie-launcher.jagoba.dev"
        ));

        // Services
        services.AddSingleton<ISettingsService, SettingsService>();
        services.AddSingleton<IWindowsStartupService, WindowsStartupService>();
        services.AddSingleton<IContentService, ContentService>();
        services.AddSingleton<IDownloadService, DownloadService>();
        services.AddSingleton<ITelemetryService, TelemetryService>();
        services.AddHttpClient("Telemetry", client =>
        {
            client.Timeout = TimeSpan.FromSeconds(3);
        });
        services.AddHttpClient("Content", client =>
        {
            client.Timeout = TimeSpan.FromSeconds(10);
        });
        services.AddHttpClient("Download", client =>
        {
            client.Timeout = Timeout.InfiniteTimeSpan;
        });

        // ViewModels
        services.AddSingleton<GlobalViewModel>();
        services.AddSingleton<HomeViewModel>();
        services.AddSingleton<GamesViewModel>();
        services.AddSingleton<LibraryViewModel>();
        services.AddSingleton<SettingsViewModel>();
        services.AddSingleton<MainViewModel>();

        // Views
        services.AddSingleton<MainWindow>();

        // Singletons


        return services.BuildServiceProvider();
    }
}
