namespace EricLostieLauncher.Models;

public record GameDownloadArgs(string GameId, string Version, string? Key = null);
