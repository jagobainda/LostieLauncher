using EricLostieLauncher.Core;
using EricLostieLauncher.Views;
using Microsoft.Extensions.DependencyInjection;
using System.Windows;
using Velopack;

namespace EricLostieLauncher;

public partial class App : Application
{
    public static IServiceProvider Services { get; private set; } = null!;

    protected override void OnStartup(StartupEventArgs e)
    {
        VelopackApp.Build().Run();

        Services = DependencyInjection.Configure();

        var loginWindow = Services.GetRequiredService<MainWindow>();
        loginWindow.Show();

        base.OnStartup(e);
    }
}
