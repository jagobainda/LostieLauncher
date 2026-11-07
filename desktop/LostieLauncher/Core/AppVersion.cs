using System.Reflection;

namespace LostieLauncher.Core;

internal static class AppVersion
{
    internal static readonly string? Packed =
        typeof(AppVersion).Assembly.GetCustomAttribute<AssemblyInformationalVersionAttribute>()?.InformationalVersion;
}
