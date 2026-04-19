namespace LostieLauncher.Utils;

public static class VersionUtils
{
    public static bool IsNewerVersion(string remoteVersion, string localVersion)
    {
        var remoteParsed = ParseBaseVersion(remoteVersion);
        var localParsed = ParseBaseVersion(localVersion);
        if (remoteParsed is null || localParsed is null) return remoteVersion != localVersion;
        return remoteParsed > localParsed;
    }

    private static Version? ParseBaseVersion(string version)
    {
        var v = version.TrimStart('v', 'V');
        var dashIndex = v.IndexOf('-');
        if (dashIndex >= 0) v = v[..dashIndex];
        return Version.TryParse(v, out var result) ? result : null;
    }
}
