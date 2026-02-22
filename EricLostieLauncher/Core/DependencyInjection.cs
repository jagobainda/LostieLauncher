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

        // Services
        services.AddSingleton<ISettingsService, SettingsService>();
        services.AddSingleton<IWindowsStartupService, WindowsStartupService>();
        services.AddSingleton<IContentService, ContentService>();
        services.AddSingleton<IDownloadService, DownloadService>();
        services.AddSingleton<ITelemetryService, TelemetryService>();

        // ViewModels
        services.AddSingleton<GlobalViewModel>();
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
