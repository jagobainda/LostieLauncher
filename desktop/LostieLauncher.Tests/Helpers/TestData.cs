using LostieLauncher.Models;

namespace LostieLauncher.Tests.Helpers;

/// <summary>
/// Centralised factories for valid model instances used across tests. Keeps Arrange
/// blocks short and ensures every test starts from the same well-formed baseline.
/// </summary>
public static class TestData
{
    public static AppSettings AppSettings(
        AppLanguage language = AppLanguage.Esp,
        AppTheme theme = AppTheme.Volcarona,
        string? downloadDirectory = null) => new()
        {
            Language = language,
            Theme = theme,
            DownloadDirectory = downloadDirectory ?? Path.Combine(Path.GetTempPath(), "LostieLauncherTests-default"),
        };

    public static GameInfo Game(
        string name = "Test Game",
        string version = "1.0.0",
        double pesoGB = 0.5,
        Guid? id = null) => new()
        {
            Id = id ?? Guid.NewGuid(),
            Nombre = name,
            Version = version,
            PesoGB = pesoGB,
            Url = "/games/test.zip",
            RutaRelativa = "test/test.exe",
            Sha256 = string.Empty,
        };

    public static LocalGameInfo LocalGame(
        string name = "Test Game",
        string version = "1.0.0",
        Guid? id = null,
        string? tipo = null) => new()
        {
            Id = id ?? Guid.NewGuid(),
            Nombre = name,
            Version = version,
            Tipo = tipo,
        };
}
