using LostieLauncher.Models;
using System.Security.Cryptography;
using System.Text;

namespace LostieLauncher.Utils;

/// <summary>
/// Construye nombres de archivo de descarga deterministas y seguros para el sistema de archivos.
/// El nombre del <c>.zip</c> (y por derivación su <c>.part</c>/<c>.part.meta</c>) incorpora un token
/// estable de la versión y la clave, de modo que descargas distintas del mismo juego —p. ej. una
/// versión especial (con clave) frente a la estándar— nunca comparten archivo y no pueden mezclar
/// bytes entre sí.
/// </summary>
public static class DownloadPathUtils
{
    /// <summary>Nombre del archivo <c>.zip</c> intermedio para una descarga concreta.</summary>
    public static string GetZipFileName(GameDownloadArgs args) =>
        $"{args.GameId}.{ComputeToken(args.Version, args.Key)}.zip";

    /// <summary>
    /// Token corto, estable y seguro para nombre de archivo que distingue una descarga por su
    /// combinación de versión y clave. Misma (versión, clave) ⇒ mismo token; cualquier diferencia
    /// ⇒ token distinto.
    /// </summary>
    public static string ComputeToken(string version, string? key)
    {
        var raw = $"{version}|{key ?? string.Empty}";
        var hash = SHA256.HashData(Encoding.UTF8.GetBytes(raw));
        return Convert.ToHexString(hash.AsSpan(0, 8)).ToLowerInvariant();
    }
}
