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


        // ViewModels
        services.AddSingleton<GlobalViewModel>();

        // Views
        services.AddSingleton<MainWindow>();

        // Singletons


        return services.BuildServiceProvider();
    }
}
