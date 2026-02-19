using EricLostieLauncher.Views;
using Microsoft.Extensions.DependencyInjection;

namespace EricLostieLauncher.Core;

public static class DependencyInjection
{
    public static IServiceProvider Configure()
    {
        var services = new ServiceCollection();

        // Views
        services.AddSingleton<MainWindow>();

        return services.BuildServiceProvider();
    }
}
