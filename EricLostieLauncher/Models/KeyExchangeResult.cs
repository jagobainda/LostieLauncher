namespace EricLostieLauncher.Models;

public class KeyExchangeResult
{
    public bool IsSuccess { get; init; }
    public string? DownloadUrl { get; init; }
    public string? ErrorMessage { get; init; }
    public int HttpStatus { get; init; }

    public static KeyExchangeResult Success(string downloadUrl) => new() { IsSuccess = true, DownloadUrl = downloadUrl };
    public static KeyExchangeResult Failed(string message, int httpStatus) => new() { IsSuccess = false, ErrorMessage = message, HttpStatus = httpStatus };
}
