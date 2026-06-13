using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.Views.Dialogs;
using SharpCompress.Archives;
using SharpCompress.Common;
using SharpCompress.Readers;
using System.Collections.ObjectModel;
using System.IO;
using System.Security.Cryptography;
using System.Text.RegularExpressions;

namespace LostieLauncher.ViewModels;

public partial class LibraryViewModel : ObservableObject
{
    private readonly ITelemetryService _telemetryService;
    private readonly IContentService _contentService;
    private readonly ISettingsService _settingsService;
    private readonly IDownloadService _downloadService;
    private readonly GlobalViewModel _globalViewModel;
    private readonly DownloadOptions _downloadOptions;
    private readonly TaskCompletionSource _libraryLoadedTcs = new();

    private CancellationTokenSource? _downloadCts;
    private GameDownloadArgs? _activeDownloadArgs;
    private SpecialVersionConfig? _activeSpecialConfig;
    private bool _serverActionsBlockedMessageShown;
    private bool _isCancelling;

    private static readonly Regex KeyFormatRegex = new(@"^[A-Za-z0-9]{4}(-[A-Za-z0-9]{4}){4}$", RegexOptions.Compiled);
    private static readonly Regex ArchivoFormatRegex = new(@"^[A-Za-z0-9._-]+\.zip$", RegexOptions.Compiled);
    private static readonly Regex Sha256FormatRegex = new(@"^[A-Fa-f0-9]{64}$", RegexOptions.Compiled);

    public event Action<string, string, string?>? GameInstalled;
    public event Action<string>? ScrollToGameRequested;
    public string? PendingScrollGameId { get; private set; }

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    public partial ObservableCollection<GameInfo> Games { get; set; } = [];

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    public partial bool IsLoading { get; set; }

    public bool IsEmpty => !IsLoading && Games.Count == 0;
    public bool IsListVisible => !IsLoading && Games.Count > 0;

    public Task LibraryLoadedTask => _libraryLoadedTcs.Task;

    public LibraryViewModel(ITelemetryService telemetryService, IContentService contentService, ISettingsService settingsService,
        IDownloadService downloadService, GlobalViewModel globalViewModel, DownloadOptions downloadOptions)
    {
        _telemetryService = telemetryService;
        _contentService = contentService;
        _settingsService = settingsService;
        _downloadService = downloadService;
        _globalViewModel = globalViewModel;
        _downloadOptions = downloadOptions;
        _ = LoadGamesAsync();
    }

    public async Task RefreshAsync() => await LoadGamesAsync();

    private async Task LoadGamesAsync()
    {
        IsLoading = true;

        var result = await _contentService.GetGamesAsync();
        var localGames = await _contentService.GetLocalGamesAsync();
        var playtimes = await _contentService.GetAllPlaytimesAsync();
        var downloadCounts = await _telemetryService.GetDownloadCountsAsync();

        var installedById = localGames
            .Where(g => g.Id != Guid.Empty)
            .ToDictionary(g => g.Id);
        var installedByName = localGames
            .Where(g => g.Id == Guid.Empty)
            .ToDictionary(g => g.Nombre, StringComparer.OrdinalIgnoreCase);

        foreach (var game in result)
        {
            LocalGameInfo? local =
                (game.Id != Guid.Empty && installedById.TryGetValue(game.Id, out var byId)) ? byId :
                installedByName.TryGetValue(game.Nombre, out var byName) ? byName : null;

            if (local is not null)
                game.DownloadStatus = Utils.VersionUtils.IsNewerVersion(game.Version, local.Version) ? GameDownloadStatus.UpdateAvailable : GameDownloadStatus.Downloaded;

            if (game.Id != Guid.Empty && playtimes.TryGetValue(game.Id, out var pt))
                game.PlaytimeMinutes = pt;

            if (downloadCounts.TryGetValue(game.GameId, out var count))
                game.TotalDownloads = count;
        }

        Games = new ObservableCollection<GameInfo>(result);
        Logs.DebugLogManager($"Games library loaded: {result.Count} games.");
        IsLoading = false;
        _libraryLoadedTcs.TrySetResult();
    }

    [RelayCommand]
    private async Task StartDownloadAsync(GameDownloadArgs args)
    {
        if (_globalViewModel.IsDownloading)
        {
            Logs.DebugLogManager($"Download request ignored for {args.GameId}: another download is already active.");
            return;
        }

        if (!await EnsureServerActionsAvailableAsync()) return;

        var game = Games.FirstOrDefault(g => g.GameId == args.GameId);
        if (game is null) return;

        var strings = SettingsViewModel.Instance.Strings;

        if (game.DownloadStatus == GameDownloadStatus.Paused && _activeDownloadArgs is not null)
        {
            args = _activeDownloadArgs;
        }
        else
        {
            var downloadPath = _contentService.GetGameDirectory(game.Nombre);

            var confirmed = DownloadConfirmDialog.Show(game, args, downloadPath, strings);
            if (confirmed is null) return;

            args = confirmed;
        }

        _activeDownloadArgs = args;
        _activeSpecialConfig = null;
        string url;

        if (!string.IsNullOrEmpty(args.Key))
        {
            if (!KeyFormatRegex.IsMatch(args.Key!))
            {
                Logs.InfoLogManager($"Download key rejected for {args.GameId}: invalid format.");
                CustomMessageBox.Show(strings.DownloadKeyInvalidTitle, strings.DownloadKeyInvalidMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
                _activeDownloadArgs = null;
                return;
            }

            var config = await _downloadService.FetchSpecialVersionConfigAsync(args.Key!);

            if (config is null)
            {
                CustomMessageBox.Show(strings.DownloadKeyNotFoundTitle, strings.DownloadKeyNotFoundMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
                _activeDownloadArgs = null;
                return;
            }

            if (!IsValidSpecialVersionConfig(config))
            {
                Logs.ErrorLogManager($"Invalid special version config for key {args.Key}: archivo='{config.Archivo}'.");
                CustomMessageBox.Show(strings.DownloadKeyNotFoundTitle, strings.DownloadKeyNotFoundMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
                _activeDownloadArgs = null;
                return;
            }

            if (config.JuegoPrincipal != game.Id)
            {
                CustomMessageBox.Show(strings.DownloadKeyMismatchTitle, strings.DownloadKeyMismatchMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
                _activeDownloadArgs = null;
                return;
            }

            _activeSpecialConfig = config;
            args = args with { Version = config.Version };
            _activeDownloadArgs = args;
            url = $"{_downloadOptions.BaseUrl}/{args.Key}/{config.Archivo}";
        }
        else
        {
            url = $"{_downloadOptions.BaseUrl}{args.RutaRelativa}";
        }

        await ExecuteDownloadAndInstallAsync(game, args, url, isUpdate: false);
    }

    [RelayCommand]
    private async Task StartUpdateAsync(GameDownloadArgs args)
    {
        if (_globalViewModel.IsDownloading)
        {
            Logs.DebugLogManager($"Update request ignored for {args.GameId}: another download is already active.");
            return;
        }

        if (!await EnsureServerActionsAvailableAsync()) return;

        var game = Games.FirstOrDefault(g => g.GameId == args.GameId);
        if (game is null) return;

        _activeDownloadArgs = args;
        _activeSpecialConfig = null;
        var url = $"{_downloadOptions.BaseUrl}{args.RutaRelativa}";

        PendingScrollGameId = args.GameId;
        ScrollToGameRequested?.Invoke(args.GameId);
        await ExecuteDownloadAndInstallAsync(game, args, url, isUpdate: true);
        PendingScrollGameId = null;
    }

    [RelayCommand]
    private async Task SwitchToSpecialVersionAsync(GameDownloadArgs args)
    {
        if (_globalViewModel.IsDownloading)
        {
            Logs.DebugLogManager($"Switch request ignored for {args.GameId}: another download is already active.");
            return;
        }

        if (!await EnsureServerActionsAvailableAsync()) return;

        var game = Games.FirstOrDefault(g => g.GameId == args.GameId);
        if (game is null) return;

        var strings = SettingsViewModel.Instance.Strings;
        var key = SpecialVersionDialog.Show(strings);
        if (key is null) return;

        if (!KeyFormatRegex.IsMatch(key))
        {
            Logs.InfoLogManager($"Special version key rejected for {args.GameId}: invalid format.");
            CustomMessageBox.Show(strings.DownloadKeyInvalidTitle, strings.DownloadKeyInvalidMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
            return;
        }

        var config = await _downloadService.FetchSpecialVersionConfigAsync(key);

        if (config is null)
        {
            CustomMessageBox.Show(strings.DownloadKeyNotFoundTitle, strings.DownloadKeyNotFoundMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
            return;
        }

        if (!IsValidSpecialVersionConfig(config))
        {
            Logs.ErrorLogManager($"Invalid special version config for key {key}: archivo='{config.Archivo}'.");
            CustomMessageBox.Show(strings.DownloadKeyNotFoundTitle, strings.DownloadKeyNotFoundMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
            return;
        }

        if (config.JuegoPrincipal != game.Id)
        {
            CustomMessageBox.Show(strings.DownloadKeyMismatchTitle, strings.DownloadKeyMismatchMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
            return;
        }

        _activeSpecialConfig = config;
        var switchArgs = new GameDownloadArgs(args.GameId, config.Version, args.RutaRelativa, key);
        _activeDownloadArgs = switchArgs;
        var url = $"{_downloadOptions.BaseUrl}/{key}/{config.Archivo}";

        PendingScrollGameId = args.GameId;
        ScrollToGameRequested?.Invoke(args.GameId);
        await ExecuteDownloadAndInstallAsync(game, switchArgs, url, isUpdate: true);
        PendingScrollGameId = null;
    }

    private async Task ExecuteDownloadAndInstallAsync(GameInfo game, GameDownloadArgs args, string url, bool isUpdate)
    {
        var gamesRoot = _settingsService.GetGamesRootDirectory();
        var zipPath = Path.Combine(gamesRoot, ".downloads", $"{args.GameId}.zip");
        var extractDir = _contentService.GetGameDirectory(game.Nombre);

        game.DownloadStatus = GameDownloadStatus.Downloading;
        game.DownloadProgressValue = 0;
        _globalViewModel.IsDownloading = true;

        _downloadCts = new CancellationTokenSource();
        var progress = new Progress<DownloadProgressInfo>(p =>
        {
            game.DownloadProgressValue = p.Percent;
            game.DownloadSpeedBytesPerSec = p.BytesPerSecond;
            game.DownloadRemainingText = p.BytesPerSecond > 0 && p.TotalBytes > 0
                ? $"· {FormatRemainingTime((p.TotalBytes - p.DownloadedBytes) / p.BytesPerSecond)}"
                : string.Empty;
        });

        var isSpecial = _activeSpecialConfig is not null;
        Logs.InfoLogManager($"Downloading: {args.GameId} v{args.Version}{(isSpecial ? $" (special: {_activeSpecialConfig!.Tipo})" : "")}.");
        var result = await _downloadService.DownloadAsync(url, zipPath, progress, _downloadCts.Token);

        switch (result.Outcome)
        {
            case DownloadOutcome.Success:
                await HandleDownloadSuccessAsync(game, args, zipPath, extractDir, isUpdate);
                break;
            case DownloadOutcome.Cancelled:
                HandleDownloadCancelled(game, args, isUpdate);
                break;
            case DownloadOutcome.Failed:
                HandleDownloadFailed(game, args, isUpdate, result.ErrorMessage);
                break;
        }

        game.DownloadSpeedBytesPerSec = 0;
        game.DownloadRemainingText = string.Empty;
        _globalViewModel.IsDownloading = false;
        _downloadCts = null;
    }

    private async Task<bool> EnsureServerActionsAvailableAsync()
    {
        var blocked = await _contentService.IsServerActionBlockedAsync();
        if (!blocked)
        {
            _serverActionsBlockedMessageShown = false;
            return true;
        }

        Logs.InfoLogManager("Server-backed action blocked by maintenance flag.");

        if (!_serverActionsBlockedMessageShown)
        {
            var strings = SettingsViewModel.Instance.Strings;
            CustomMessageBox.Show(strings.ServerActionsUnavailableTitle, strings.ServerActionsUnavailableMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Information);
            _serverActionsBlockedMessageShown = true;
        }

        return false;
    }

    private async Task HandleDownloadSuccessAsync(GameInfo game, GameDownloadArgs args, string zipPath, string extractDir, bool isUpdate)
    {
        try
        {
            Logs.InfoLogManager($"Download complete, extracting: {args.GameId}.");

            var expectedHash = _activeSpecialConfig?.Sha256 ?? game.Sha256;
            if (!string.IsNullOrEmpty(expectedHash) && !await VerifyIntegrityAsync(game, zipPath, expectedHash))
            {
                File.Delete(zipPath);
                Logs.ErrorLogManager($"Hash mismatch for {args.GameId}. Expected: {expectedHash}");
                ResetDownloadState(game, isUpdate);
                var strings = SettingsViewModel.Instance.Strings;
                CustomMessageBox.Show(strings.HashMismatchTitle, strings.HashMismatchMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
                return;
            }

            game.DownloadStatus = GameDownloadStatus.Extracting;
            game.DownloadProgressValue = 100;
            game.DownloadRemainingText = string.Empty;

            await ExtractArchiveAsync(zipPath, extractDir);

            var tipo = _activeSpecialConfig?.Tipo;
            await _contentService.RegisterGameAsync(game.Id, game.Nombre, args.Version, tipo);

            game.DownloadStatus = GameDownloadStatus.Downloaded;
            game.DownloadProgressValue = 100;
            _activeDownloadArgs = null;
            _activeSpecialConfig = null;
            Logs.InfoLogManager($"Game installed: {args.GameId} v{args.Version}{(tipo is not null ? $" ({tipo})" : "")}.");
            GameInstalled?.Invoke(game.Nombre, args.Version, tipo);
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            ResetDownloadState(game, isUpdate);
        }
    }

    private static async Task<bool> VerifyIntegrityAsync(GameInfo game, string zipPath, string expectedHash)
    {
        game.DownloadStatus = GameDownloadStatus.VerifyingIntegrity;
        return await Task.Run(() =>
        {
            using var sha = SHA256.Create();
            using var fs = File.OpenRead(zipPath);
            var actualHash = Convert.ToHexString(sha.ComputeHash(fs));
            return actualHash.Equals(expectedHash, StringComparison.OrdinalIgnoreCase);
        });
    }

    private static async Task ExtractArchiveAsync(string zipPath, string extractDir)
    {
        await Task.Run(() =>
        {
            Directory.CreateDirectory(extractDir);
            var readerOptions = new ReaderOptions
            {
                ArchiveEncoding = new ArchiveEncoding { Default = System.Text.Encoding.UTF8 }
            };

            var extractDirFull = Path.GetFullPath(extractDir) + Path.DirectorySeparatorChar;
            using (var stream = File.OpenRead(zipPath))
            using (var archive = ArchiveFactory.OpenArchive(stream, readerOptions))
            {
                foreach (var entry in archive.Entries.Where(e => !e.IsDirectory && e.Key is not null))
                {
                    var destPath = Path.GetFullPath(Path.Combine(extractDir, entry.Key!));
                    if (!destPath.StartsWith(extractDirFull, StringComparison.OrdinalIgnoreCase))
                        throw new InvalidOperationException($"Zip Slip attempt detected in entry: {entry.Key}");
                    Directory.CreateDirectory(Path.GetDirectoryName(destPath)!);
                    using var entryStream = entry.OpenEntryStream();
                    using var outStream = new FileStream(destPath, FileMode.Create, FileAccess.Write, FileShare.None);
                    entryStream.CopyTo(outStream);
                }
            }
            File.Delete(zipPath);
        });
    }

    private void HandleDownloadCancelled(GameInfo game, GameDownloadArgs args, bool isUpdate)
    {
        if (_isCancelling)
        {
            Logs.InfoLogManager($"Download cancelled: {args.GameId}.");
            CleanupDownloadFiles(game);
            ResetDownloadState(game, isUpdate);
            _isCancelling = false;
        }
        else if (isUpdate)
        {
            Logs.InfoLogManager($"Update cancelled: {args.GameId}.");
            ResetDownloadState(game, isUpdate: true);
        }
        else
        {
            Logs.InfoLogManager($"Download paused: {args.GameId}.");
            game.DownloadStatus = GameDownloadStatus.Paused;
        }
    }

    private void HandleDownloadFailed(GameInfo game, GameDownloadArgs args, bool isUpdate, string? errorMessage)
    {
        var strings = SettingsViewModel.Instance.Strings;
        Logs.ErrorLogManager($"Download failed: {args.GameId}: {errorMessage}");
        ResetDownloadState(game, isUpdate);
        CustomMessageBox.Show(strings.DownloadErrorTitle, strings.DownloadErrorMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
    }

    private void ResetDownloadState(GameInfo game, bool isUpdate)
    {
        game.DownloadStatus = isUpdate ? GameDownloadStatus.UpdateAvailable : GameDownloadStatus.Available;
        game.DownloadProgressValue = 0;
        _activeDownloadArgs = null;
    }

    private static string FormatRemainingTime(double seconds)
    {
        if (seconds <= 0) return string.Empty;
        var ts = TimeSpan.FromSeconds(seconds);
        if (ts.TotalHours >= 1) return $"{(int)ts.TotalHours}h {ts.Minutes}m";
        if (ts.TotalMinutes >= 1) return $"{ts.Minutes}m {ts.Seconds}s";
        return $"{ts.Seconds}s";
    }

    private static bool IsValidSpecialVersionConfig(SpecialVersionConfig config) =>
        ArchivoFormatRegex.IsMatch(config.Archivo) &&
        (string.IsNullOrEmpty(config.Sha256) || Sha256FormatRegex.IsMatch(config.Sha256)) &&
        !string.IsNullOrWhiteSpace(config.Version);

    [RelayCommand]
    private void PauseDownload()
    {
        _downloadCts?.Cancel();
    }

    [RelayCommand]
    private void CancelDownload()
    {
        var game = _activeDownloadArgs is not null
            ? Games.FirstOrDefault(g => g.GameId == _activeDownloadArgs.GameId)
            : null;

        if (game is null) return;

        var strings = SettingsViewModel.Instance.Strings;
        var confirmed = CustomMessageBox.Show(
            strings.CancelDownloadConfirmTitle,
            strings.CancelDownloadConfirmMessage,
            CustomMessageBoxButton.YesNo,
            CustomMessageBoxIcon.Information);

        if (confirmed != true) return;

        if (game.DownloadStatus == GameDownloadStatus.Paused)
        {
            CleanupDownloadFiles(game);
            ResetDownloadState(game, isUpdate: false);
            _globalViewModel.IsDownloading = false;
            return;
        }

        _isCancelling = true;
        _downloadCts?.Cancel();
    }

    private void CleanupDownloadFiles(GameInfo game)
    {
        if (_activeDownloadArgs is null) return;

        var gamesRoot = _settingsService.GetGamesRootDirectory();
        var zipPath = Path.Combine(gamesRoot, ".downloads", $"{_activeDownloadArgs.GameId}.zip");
        var partPath = zipPath + ".part";
        var metaPath = partPath + ".meta";

        try { if (File.Exists(partPath)) File.Delete(partPath); } catch { }
        try { if (File.Exists(zipPath)) File.Delete(zipPath); } catch { }
        try { if (File.Exists(metaPath)) File.Delete(metaPath); } catch { }
    }
}
