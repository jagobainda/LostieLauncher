using System.Diagnostics;
using System.Diagnostics.CodeAnalysis;

namespace LostieLauncher.Utils;

public static class UrlLauncher
{
    public static void OpenHttps(string? url)
    {
        if (!TryGetHttpsUri(url, out var uri))
        {
            Logs.ErrorLogManager($"Refused to open URL with the shell (not an absolute HTTPS URL): {url ?? "<null>"}");
            return;
        }

        try
        {
            Process.Start(new ProcessStartInfo(uri.AbsoluteUri) { UseShellExecute = true });
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
    }

    public static bool TryGetHttpsUri(string? url, [NotNullWhen(true)] out Uri? uri)
    {
        if (!string.IsNullOrWhiteSpace(url)
            && Uri.TryCreate(url, UriKind.Absolute, out var parsed)
            && parsed.Scheme == Uri.UriSchemeHttps)
        {
            uri = parsed;
            return true;
        }

        uri = null;
        return false;
    }
}
