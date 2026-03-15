using System.Collections.ObjectModel;
using System.Diagnostics;
using System.IO;
using CommunityToolkit.Mvvm.ComponentModel;
using CommunityToolkit.Mvvm.Input;
using EricLostieLauncher.Models;
using EricLostieLauncher.Services;
using EricLostieLauncher.Views.Dialogs;

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
        _ = LoadInstalledGamesAsync(waitForLibrary: true);
    }

    public async Task RefreshAsync() => await LoadInstalledGamesAsync(waitForLibrary: false);

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
    private void OpenFolder(string gameName)
    {
        Logs.DebugLogManager($"Opening folder for: {gameName}.");
        var path = _contentService.GetGameDirectory(gameName);

        if (!Directory.Exists(path))
        {
            var result = CustomMessageBox.Show(
                SettingsViewModel.Instance.Strings.FolderNotFoundTitle,
                SettingsViewModel.Instance.Strings.FolderNotFoundMessage,
                CustomMessageBoxButton.YesNo,
                CustomMessageBoxIcon.Information);

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
