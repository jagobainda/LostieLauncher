using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;
using EricLostieLauncher.Views.Dialogs;
using System.Collections.ObjectModel;
using System.Diagnostics;
using System.IO;
using System.Windows;

namespace EricLostieLauncher.ViewModels;

public partial class GamesViewModel : ObservableObject
{
    private readonly IContentService _contentService;
    private readonly LibraryViewModel _libraryViewModel;

    public event Action? NavigateToLibraryRequested;

    [ObservableProperty]
    public partial ObservableCollection<InstalledGameInfo> InstalledGames { get; set; } = [];

    [ObservableProperty]
    public partial bool IsLoading { get; set; }

    public GamesViewModel(IContentService contentService, LibraryViewModel libraryViewModel)
    {
        _contentService = contentService;
        _libraryViewModel = libraryViewModel;
        _libraryViewModel.GameInstalled += OnGameInstalled;
        _ = LoadInstalledGamesAsync(waitForLibrary: true);
    }

    public async Task RefreshAsync() => await LoadInstalledGamesAsync(waitForLibrary: false);

    private void OnGameInstalled(string gameName, string version)
    {
        App.Current.Dispatcher.Invoke(() =>
        {
            var remote = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
            var existing = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
            if (existing != null) InstalledGames.Remove(existing);
            InstalledGames.Add(new InstalledGameInfo { Id = remote?.Id ?? Guid.Empty, Nombre = gameName, InstalledVersion = version });
        });
        Logs.DebugLogManager($"Games list updated after install: {gameName} v{version}.");
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
            var remote = remoteGames.FirstOrDefault(r =>
                (local.Id != Guid.Empty && r.Id == local.Id) ||
                string.Equals(r.Nombre, local.Nombre, StringComparison.OrdinalIgnoreCase));
            var hasUpdate = remote != null && remote.Version != local.Version;
            var playtimeMinutes = local.Id != Guid.Empty && playtimes.TryGetValue(local.Id, out var pt) ? pt : 0;
            return new InstalledGameInfo
            {
                Id = local.Id,
                Nombre = local.Nombre,
                InstalledVersion = local.Version,
                HasUpdate = hasUpdate,
                UpdateVersion = hasUpdate ? remote!.Version : string.Empty,
                PlaytimeMinutes = playtimeMinutes
            };
        })];

        InstalledGames = new ObservableCollection<InstalledGameInfo>(installed);
        Logs.DebugLogManager($"Installed games loaded: {InstalledGames.Count} games.");
        IsLoading = false;

        if (waitForLibrary && SettingsViewModel.Instance.AutoUpdate)
        {
            foreach (var game in InstalledGames.Where(g => g.HasUpdate).ToList())
            {
                await UpdateCoreAsync(game.Nombre, navigateToLibrary: false);
            }
        }
    }

    [RelayCommand]
    private Task UpdateAsync(string gameName) => UpdateCoreAsync(gameName, navigateToLibrary: true);

    private async Task UpdateCoreAsync(string gameName, bool navigateToLibrary)
    {
        var libraryGame = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        if (libraryGame is null) return;

        var installedGame = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        if (installedGame is not null) installedGame.IsUpdating = true;

        if (navigateToLibrary) NavigateToLibraryRequested?.Invoke();

        var args = new GameDownloadArgs(libraryGame.GameId, libraryGame.Version, libraryGame.RutaRelativa);
        await _libraryViewModel.StartUpdateCommand.ExecuteAsync(args);

        if (installedGame is not null) installedGame.IsUpdating = false;
    }

    [RelayCommand]
    private void Play(string gameName)
    {
        Logs.DebugLogManager($"Launching game: {gameName}.");
        var exePath = Path.Combine(_contentService.GetGameDirectory(gameName), "Game.exe");
        var gameId = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase))?.Id ?? Guid.Empty;

        if (!File.Exists(exePath))
        {
            CustomMessageBox.Show(SettingsViewModel.Instance.Strings.GameExeNotFoundTitle, SettingsViewModel.Instance.Strings.GameExeNotFoundMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
            return;
        }

        try
        {
            var startTime = DateTime.UtcNow;
            var process = Process.Start(new ProcessStartInfo(exePath) { UseShellExecute = true, WorkingDirectory = Path.GetDirectoryName(exePath)! });
            if (process is not null)
            {
                process.EnableRaisingEvents = true;
                process.Exited += async (_, _) =>
                {
                    var minutes = (int)(DateTime.UtcNow - startTime).TotalMinutes;
                    Logs.DebugLogManager($"Game process exited: {gameName}. Session: {minutes} min.");
                    if (minutes > 0 && gameId != Guid.Empty)
                        await _contentService.AddPlaytimeAsync(gameId, minutes).ConfigureAwait(false);
                    Application.Current.Dispatcher.Invoke(() =>
                    {
                        if (minutes > 0)
                        {
                            var installedGame = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
                            if (installedGame is not null) installedGame.PlaytimeMinutes += minutes;
                            var libraryGame = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
                            if (libraryGame is not null) libraryGame.PlaytimeMinutes += minutes;
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
    private async Task UninstallAsync(string gameName)
    {
        var strings = SettingsViewModel.Instance.Strings;

        var confirm = CustomMessageBox.Show(strings.UninstallConfirmTitle, string.Format(strings.UninstallConfirmMessage, gameName), CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Information);

        if (confirm != true) return;

        Logs.InfoLogManager($"Uninstalling game: {gameName}.");
        var path = _contentService.GetGameDirectory(gameName);
        var folderExisted = Directory.Exists(path);

        if (folderExisted)
        {
            try
            {
                Directory.Delete(path, recursive: true);
            }
            catch (Exception ex)
            {
                Logs.ErrorLogManager(ex);
                CustomMessageBox.Show(strings.UninstallErrorTitle, strings.UninstallErrorMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Error);
                return;
            }
        }

        await _contentService.RemoveGameRegistryAsync(gameName);

        var toRemove = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        if (toRemove != null) InstalledGames.Remove(toRemove);

        var libraryGame = _libraryViewModel.Games.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
        if (libraryGame != null) libraryGame.DownloadStatus = GameDownloadStatus.Available;

        Logs.InfoLogManager($"Game uninstalled: {gameName}.");

        if (!folderExisted)
        {
            CustomMessageBox.Show(strings.UninstallNotFoundTitle, strings.UninstallNotFoundMessage, CustomMessageBoxButton.OK, CustomMessageBoxIcon.Information);
        }
    }
}
