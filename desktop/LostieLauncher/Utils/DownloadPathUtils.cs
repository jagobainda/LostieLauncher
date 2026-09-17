using LostieLauncher.Models;
using System.Security.Cryptography;
using System.Text;

namespace LostieLauncher.Utils;

public static class DownloadPathUtils
{
    private const string PartExtension = ".part";
    private const string MetaExtension = ".meta";

    public static string GetZipFileName(GameDownloadArgs args) => $"{args.GameId}.{ComputeToken(args.Version, args.Key)}.zip";

    public static string GetPartFilePath(string finalPath) => finalPath + PartExtension;

    public static string GetMetaFilePath(string partPath) => partPath + MetaExtension;

    public static string ComputeToken(string version, string? key)
    {
        var raw = $"{version}|{key ?? string.Empty}";
        var hash = SHA256.HashData(Encoding.UTF8.GetBytes(raw));
        return Convert.ToHexString(hash.AsSpan(0, 8)).ToLowerInvariant();
    }
}
