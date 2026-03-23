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

    [ObservableProperty]
    private ObservableCollection<InstalledGameInfo> _installedGames = [];

    [ObservableProperty]
    private bool _isLoading;

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
            var existing = InstalledGames.FirstOrDefault(g => string.Equals(g.Nombre, gameName, StringComparison.OrdinalIgnoreCase));
            if (existing != null) InstalledGames.Remove(existing);
            InstalledGames.Add(new InstalledGameInfo { Nombre = gameName, InstalledVersion = version });
        });
        Logs.DebugLogManager($"Games list updated after install: {gameName} v{version}.");
    }

    private async Task LoadInstalledGamesAsync(bool waitForLibrary = true)
    {
        IsLoading = true;

        if (waitForLibrary) await _libraryViewModel.LibraryLoadedTask;

        var localGames = await _contentService.GetLocalGamesAsync();
        var remoteGames = _libraryViewModel.Games;

        IEnumerable<InstalledGameInfo> installed = [.. localGames.Select(local =>
        {
            var remote = remoteGames.FirstOrDefault(r => string.Equals(r.Nombre, local.Nombre, StringComparison.OrdinalIgnoreCase));
            var hasUpdate = remote != null && remote.Version != local.Version;
            return new InstalledGameInfo
            {
                Nombre = local.Nombre,
                InstalledVersion = local.Version,
                HasUpdate = hasUpdate,
                UpdateVersion = hasUpdate ? remote!.Version : string.Empty
            };
        })];

        InstalledGames = new ObservableCollection<InstalledGameInfo>(installed);
        Logs.DebugLogManager($"Installed games loaded: {InstalledGames.Count} games.");
        IsLoading = false;
    }

    [RelayCommand]
    private void Play(string gameName)
    {
        Logs.DebugLogManager($"Launching game: {gameName}.");
        var exePath = Path.Combine(_contentService.GetGameDirectory(gameName), "Game.exe");

        if (!File.Exists(exePath))
        {
            CustomMessageBox.Show(
                SettingsViewModel.Instance.Strings.GameExeNotFoundTitle,
                SettingsViewModel.Instance.Strings.GameExeNotFoundMessage,
                CustomMessageBoxButton.OK,
                CustomMessageBoxIcon.Error);
            return;
        }

        try
        {
            var process = Process.Start(new ProcessStartInfo(exePath) { UseShellExecute = true, WorkingDirectory = Path.GetDirectoryName(exePath)! });
            if (process is not null)
            {
                process.EnableRaisingEvents = true;
                process.Exited += (_, _) =>
                {
                    Logs.DebugLogManager($"Game process exited: {gameName}.");
                    Application.Current.Dispatcher.Invoke(() => Application.Current.MainWindow.WindowState = WindowState.Normal);
                };
            }
            Application.Current.MainWindow.WindowState = WindowState.Minimized;
        }
        catch (Exception ex) { Logs.ErrorLogManager(ex); }
    }

    [RelayCommand]
    private void OpenFolder(string gameName)
    {
        Logs.DebugLogManager($"Opening folder for: {gameName}.");
        var path = _contentService.GetGameDirectory(gameName);

        if (!Directory.Exists(path))
        {
            var result = CustomMessageBox.Show(SettingsViewModel.Instance.Strings.FolderNotFoundTitle, SettingsViewModel.Instance.Strings.FolderNotFoundMessage, CustomMessageBoxButton.YesNo, CustomMessageBoxIcon.Information);

            if (result == true)
            {
                // TODO: Trigger reinstall when download feature is implemented
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

        var confirm = CustomMessageBox.Show(
            strings.UninstallConfirmTitle,
            string.Format(strings.UninstallConfirmMessage, gameName),
            CustomMessageBoxButton.YesNo,
            CustomMessageBoxIcon.Information);

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
                CustomMessageBox.Show(
                    strings.UninstallErrorTitle,
                    strings.UninstallErrorMessage,
                    CustomMessageBoxButton.OK,
                    CustomMessageBoxIcon.Error);
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
            CustomMessageBox.Show(
                strings.UninstallNotFoundTitle,
                strings.UninstallNotFoundMessage,
                CustomMessageBoxButton.OK,
                CustomMessageBoxIcon.Information);
        }
    }
}
