using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using LostieLauncher.Models;
using LostieLauncher.Services;
using LostieLauncher.Views.Dialogs;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.IO;
using System.Windows;

namespace LostieLauncher.ViewModels;

public partial class GamesViewModel : ObservableObject
{
    private readonly IContentService _contentService;
    private readonly LibraryViewModel _libraryViewModel;
    private readonly ITelemetryService _telemetryService;

    public event Action? NavigateToLibraryRequested;

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    public partial ObservableCollection<InstalledGameInfo> InstalledGames { get; set; } = [];

    [ObservableProperty]
    [NotifyPropertyChangedFor(nameof(IsEmpty))]
    [NotifyPropertyChangedFor(nameof(IsListVisible))]
    public partial bool IsLoading { get; set; }

    public bool IsEmpty => !IsLoading && InstalledGames.Count == 0;
    public bool IsListVisible => !IsLoading && InstalledGames.Count > 0;

    public GamesViewModel(IContentService contentService, LibraryViewModel libraryViewModel, ITelemetryService telemetryService)
    {
        _contentService = contentService;
        _libraryViewModel = libraryViewModel;
        _telemetryService = telemetryService;
        _libraryViewModel.GameInstalled += OnGameInstalled;
        _ = LoadInstalledGamesAsync(waitForLibrary: true);
    }

    public async Task RefreshAsync() => await LoadInstalledGamesAsync(waitForLibrary: false);

    private void OnGameInstalled(string gameName, string version, string? tipo)
    {
        App.Current.Dispatcher.Invoke(() =>
        {
            var remote = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
            var existing = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
            if (existing != null) InstalledGames.Remove(existing);
            InstalledGames.Add(new InstalledGameInfo { Id = remote?.Id ?? Guid.Empty, Nombre = gameName, InstalledVersion = version, Logo = remote?.Logo ?? string.Empty, Tipo = tipo });
        });
        Logs.DebugLogManager($"Games list updated after install: {gameName} v{version}{(tipo is not null ? $" ({tipo})" : "")}.");
    }

    private async Task LoadInstalledGamesAsync(bool waitForLibrary = true)
    {
        IsLoading = true;

        if (waitForLibrary) await _libraryViewModel.LibraryLoadedTask;

        var localGames = await _contentService.GetLocalGamesAsync();
        var playtimes = await _contentService.GetAllPlaytimesAsync();
        var remoteGames = _libraryViewModel.Games;

        IEnumerable<InstalledGameInfo> installed = [.. localGames.Select(local =>
        {
            var remote = remoteGames.FirstOrDefault(r => (local.Id != Guid.Empty && r.Id == local.Id) || string.Equals(r.Nombre, local.Nombre, StringComparison.OrdinalIgnoreCase));
            var hasUpdate = remote != null && Utils.VersionUtils.IsNewerVersion(remote.Version, local.Version);
            var playtimeMinutes = local.Id != Guid.Empty && playtimes.TryGetValue(local.Id, out var pt) ? pt : 0;

            return new InstalledGameInfo
            {
                Id = local.Id,
                Nombre = local.Nombre,
                InstalledVersion = local.Version,
                HasUpdate = hasUpdate,
                UpdateVersion = hasUpdate ? remote!.Version : string.Empty,
                Logo = remote?.Logo ?? string.Empty,
                Tipo = local.Tipo,
                PlaytimeMinutes = playtimeMinutes,
                HasHelpFolder = HasHelpSubfolder(_contentService.GetGameDirectory(local.Nombre))
            };
        })];

        InstalledGames = new ObservableCollection<InstalledGameInfo>(installed);
        Logs.DebugLogManager($"Installed games loaded: {InstalledGames.Count} games.");
        IsLoading = false;

        if (!waitForLibrary || !SettingsViewModel.Instance.AutoUpdate) return;
        foreach (var game in InstalledGames.Where(g => g.HasUpdate && !g.IsSpecialVersion).ToList()) await UpdateCoreAsync(game.Nombre, navigateToLibrary: false);
    }

    [RelayCommand]
    private Task UpdateAsync(string gameName) => UpdateCoreAsync(gameName, navigateToLibrary: true);

    private static bool HasHelpSubfolder(string gameDir)
    {
        if (!Directory.Exists(gameDir)) return false;
        return Directory.EnumerateDirectories(gameDir)
            .Any(d => string.Equals(Path.GetFileName(d), "ayuda", StringComparison.OrdinalIgnoreCase));
    }

    private async Task UpdateCoreAsync(string gameName, bool navigateToLibrary)
    {
        var libraryGame = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        if (libraryGame is null) return;

        var installedGame = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        installedGame?.IsUpdating = true;

        if (navigateToLibrary) NavigateToLibraryRequested?.Invoke();

        var args = new GameDownloadArgs(libraryGame.GameId, libraryGame.Version, libraryGame.RutaRelativa);
        await _libraryViewModel.StartUpdateCommand.ExecuteAsync(args);

        installedGame?.IsUpdating = false;
    }

    [RelayCommand]
    private void NavigateToLibrary() => NavigateToLibraryRequested?.Invoke();

    [RelayCommand]
    private async Task SwitchToSpecialVersionAsync(string gameName)
    {
        var libraryGame = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        if (libraryGame is null) return;

        NavigateToLibraryRequested?.Invoke();

        var args = new GameDownloadArgs(libraryGame.GameId, libraryGame.Version, libraryGame.RutaRelativa);
        await _libraryViewModel.SwitchToSpecialVersionCommand.ExecuteAsync(args);
    }

    [RelayCommand]
    private void Play(string gameName)
    {
        Logs.DebugLogManager($"Launching game: {gameName}.");
        var exePath = Path.Combine(_contentService.GetGameDirectory(gameName), "Game.exe");
        var installedGame = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        var gameGuid = installedGame?.Id ?? Guid.Empty;

        if (!File.Exists(exePath))
        {
            CustomMessageBox.Show(SettingsViewModel.Instance.Strings.GameExeNotFoundTitle, SettingsViewModel.Instance.Strings.GameExeNotFoundMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
            return;
        }

        try
        {
            var gameVersion = installedGame?.InstalledVersion ?? "0.0.0";
            var libraryGame = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
            _telemetryService.TrackGameLaunched(libraryGame?.GameId ?? gameName.ToLowerInvariant(), gameVersion);

            var startTime = DateTime.UtcNow;
            var process = Process.Start(new ProcessStartInfo(exePath) { UseShellExecute = true, WorkingDirectory = Path.GetDirectoryName(exePath)! });
            if (process is not null)
            {
                process.EnableRaisingEvents = true;
                process.Exited += async (_, _) =>
                {
                    var minutes = (int)(DateTime.UtcNow - startTime).TotalMinutes;
                    Logs.DebugLogManager($"Game process exited: {gameName}. Session: {minutes} min.");
                    if (minutes > 0 && gameGuid != Guid.Empty)
                        await _contentService.AddPlaytimeAsync(gameGuid, minutes).ConfigureAwait(false);
                    Application.Current.Dispatcher.Invoke(() =>
                    {
                        if (minutes > 0)
                        {
                            var installedGame = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
                            installedGame?.PlaytimeMinutes += minutes;
                            var libraryGame = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
                            libraryGame?.PlaytimeMinutes += minutes;
                        }
                        Application.Current.MainWindow.WindowState = WindowState.Normal;
                        Application.Current.MainWindow.Activate();
                    });
                };
            }
            Application.Current.MainWindow.WindowState = WindowState.Minimized;
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
    }

    [RelayCommand]
    private async Task OpenFolderAsync(string gameName)
    {
        Logs.DebugLogManager($"Opening folder for: {gameName}.");
        var path = _contentService.GetGameDirectory(gameName);

        if (!Directory.Exists(path))
        {
            var result = CustomMessageBox.Show(SettingsViewModel.Instance.Strings.FolderNotFoundTitle, SettingsViewModel.Instance.Strings.FolderNotFoundMessage, CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Information);

            if (result == true)
            {
                var libraryGame = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
                if (libraryGame is null) return;

                NavigateToLibraryRequested?.Invoke();
                var args = new GameDownloadArgs(libraryGame.GameId, libraryGame.Version, libraryGame.RutaRelativa);
                await _libraryViewModel.StartDownloadCommand.ExecuteAsync(args);
            }
            return;
        }

        try
        {
            Process.Start(new ProcessStartInfo("explorer.exe", path) { UseShellExecute = true });
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
    }

    [RelayCommand]
    private void OpenHelpFolder(string gameName)
    {
        Logs.DebugLogManager($"Opening help folder for: {gameName}.");
        var gameDir = _contentService.GetGameDirectory(gameName);
        var helpDir = Directory.EnumerateDirectories(gameDir)
            .FirstOrDefault(d => string.Equals(Path.GetFileName(d), "ayuda", StringComparison.OrdinalIgnoreCase));

        if (helpDir is null) return;

        try
        {
            Process.Start(new ProcessStartInfo("explorer.exe", helpDir) { UseShellExecute = true });
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
    }

    [RelayCommand]
    private async Task UninstallAsync(string gameName)
    {
        var strings = SettingsViewModel.Instance.Strings;

        var confirm = CustomMessageBox.Show(strings.UninstallConfirmTitle, string.Format(strings.UninstallConfirmMessage, gameName), CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Information);

        if (confirm != true) return;

        Logs.InfoLogManager($"Uninstalling game: {gameName}.");
        var path = _contentService.GetGameDirectory(gameName);
        var folderExisted = Directory.Exists(path);

        var target = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        target?.IsUninstalling = true;

        if (folderExisted)
        {
            try
            {
                await Task.Run(() => Directory.Delete(path, recursive: true));
            }
            catch (Exception ex)
            {
                Logs.ErrorLogManager(ex);
                target?.IsUninstalling = false;
                CustomMessageBox.Show(strings.UninstallErrorTitle, strings.UninstallErrorMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
                return;
            }
        }

        await _contentService.RemoveGameRegistryAsync(gameName);

        if (target != null)
        {
            InstalledGames.Remove(target);
            OnPropertyChanged(nameof(IsEmpty));
            OnPropertyChanged(nameof(IsListVisible));
        }

        var libraryGame = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        libraryGame?.DownloadStatus = GameDownloadStatus.Available;

        Logs.InfoLogManager($"Game uninstalled: {gameName}.");

        if (!folderExisted) CustomMessageBox.Show(strings.UninstallNotFoundTitle, strings.UninstallNotFoundMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Information);
    }
}
