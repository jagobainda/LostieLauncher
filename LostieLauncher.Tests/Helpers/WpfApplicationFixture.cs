using System.Reflection;
using System.Windows;
using LostieLauncher.ViewModels;

namespace LostieLauncher.Tests.Helpers;

/// <summary>
/// Lazily ensures a single <see cref="System.Windows.Application"/> instance exists for
/// the test AppDomain so view models that touch <c>Application.Current.Resources</c>
/// (SettingsViewModel, MainViewModel via ApplyTheme, etc.) can be instantiated.
/// </summary>
/// <remarks>
/// <para>
/// WPF allows only one <c>Application</c> per AppDomain, so we expose this through a
/// shared xUnit collection (<see cref="WpfCollection"/>) and disable parallelization
/// on tests that depend on it. We never pump a <c>Dispatcher</c>: tests must not invoke
/// any code that calls <c>Dispatcher.Invoke</c>.
/// </para>
/// <para>
/// The fixture also pre-registers a real production theme dictionary so the
/// <c>SettingsViewModel</c> constructor's "find an active theme" lookup succeeds and
/// skips <c>ApplyTheme</c> (which requires running inside the launcher exe).
/// </para>
/// </remarks>
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

            // Production ApplyTheme uses pack://application:,,,/Themes/{theme}.xaml.
            // Pack URIs without an explicit ;component segment resolve against
            // Application.ResourceAssembly. WPF eagerly initialises that property to the
            // entry assembly (testhost) on first access, after which the public setter
            // refuses any change ("ResourceAssembly cannot be changed once set"). We
            // therefore overwrite the private backing field via reflection so the pack
            // resolver returns the embedded /Themes/*.xaml resources from the production
            // assembly instead.
            var prodAssembly = typeof(SettingsViewModel).Assembly;
            var resourceAsmField = typeof(Application).GetField("_resourceAssembly",
                BindingFlags.Static | BindingFlags.NonPublic);
            if (resourceAsmField is not null)
            {
                resourceAsmField.SetValue(null, prodAssembly);
            }
            // Best-effort: also synchronise the BaseUriHelper cache that some WPF
            // builds use as the pack-URI resolution root.
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
