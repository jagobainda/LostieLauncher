namespace LostieLauncher.Models;

public record GameDownloadArgs(string GameId, string Version, string RutaRelativa, string? Key = null);
