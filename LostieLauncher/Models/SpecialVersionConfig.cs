namespace LostieLauncher.Models;

public class SpecialVersionConfig
{
    public string Sha256 { get; init; } = string.Empty;
    public string Tipo { get; init; } = string.Empty;
    public Guid JuegoPrincipal { get; init; }
    public string Version { get; init; } = string.Empty;
    public string Archivo { get; init; } = string.Empty;

    public static SpecialVersionConfig? Parse(string content)
    {
        var values = new Dictionary<string, string>(StringComparer.OrdinalIgnoreCase);

        foreach (var line in content.Split('\n', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries))
        {
            var separatorIndex = line.IndexOf('=');
            if (separatorIndex <= 0) continue;

            var key = line[..separatorIndex].Trim();
            var value = line[(separatorIndex + 1)..].Trim();
            values[key] = value;
        }

        if (!values.TryGetValue("sha256", out var sha256) ||
            !values.TryGetValue("tipo", out var tipo) ||
            !values.TryGetValue("juego-principal", out var juegoPrincipalStr) ||
            !values.TryGetValue("vers", out var version) ||
            !values.TryGetValue("archivo", out var archivo))
            return null;

        if (!Guid.TryParse(juegoPrincipalStr, out var juegoPrincipal))
            return null;

        return new SpecialVersionConfig
        {
            Sha256 = sha256,
            Tipo = tipo,
            JuegoPrincipal = juegoPrincipal,
            Version = version,
            Archivo = archivo
        };
    }
}
