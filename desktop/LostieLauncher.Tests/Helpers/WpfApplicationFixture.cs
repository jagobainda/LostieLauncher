using LostieLauncher.ViewModels;
using System.Reflection;
using System.Windows;

namespace LostieLauncher.Tests.Helpers;

public sealed class WpfApplicationFixture
{
    private static readonly object Gate = new();
    private static bool _initialized;

    public WpfApplicationFixture()
    {
        lock (Gate)
        {
            if (_initialized) return;

            if (Application.Current is null) _ = new Application();

            var prodAssembly = typeof(SettingsViewModel).Assembly;
            var resourceAsmField = typeof(Application).GetField("_resourceAssembly",
                BindingFlags.Static | BindingFlags.NonPublic);
            if (resourceAsmField is not null)
            {
                resourceAsmField.SetValue(null, prodAssembly);
            }
            var baseUriHelper = typeof(Application).Assembly.GetType("System.Windows.Navigation.BaseUriHelper");
            baseUriHelper?.GetField("_resourceAssembly", BindingFlags.Static | BindingFlags.NonPublic)
                ?.SetValue(null, prodAssembly);

            _initialized = true;
        }
    }
}

[CollectionDefinition(Name)]
public sealed class WpfCollection : ICollectionFixture<WpfApplicationFixture>
{
    public const string Name = "WPF Application";
}
