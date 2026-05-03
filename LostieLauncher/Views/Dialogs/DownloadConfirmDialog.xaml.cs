using System.Diagnostics;
using System.Globalization;
using System.IO;
using System.Windows;
using System.Windows.Input;
using System.Windows.Media.Imaging;
using LostieLauncher.Content;
using LostieLauncher.Models;

namespace LostieLauncher.Views.Dialogs;

public partial class DownloadConfirmDialog : Window
{
    private readonly string _gameUrl;
    private string? _resultKey;

    private DownloadConfirmDialog(GameInfo game, string downloadPath, IStrings strings)
    {
        InitializeComponent();

        _gameUrl = game.Url;

        TitleText.Text = strings.DownloadDialogTitle;
        GameTitleText.Text = game.Nombre;
        DescriptionText.Text = string.IsNullOrWhiteSpace(game.Descripcion)
            ? strings.DownloadDialogNoDescription
            : game.Descripcion;

        ViewPageText.Text = strings.DownloadDialogViewPage;
        ViewPageButton.Visibility = string.IsNullOrWhiteSpace(game.Url)
            ? Visibility.Collapsed
            : Visibility.Visible;

        PathLabel.Text = strings.DownloadDialogPath;
        DownloadPathText.Text = downloadPath;

        GameSizeLabel.Text = $"{strings.DownloadDialogGameSize}:";
        GameSizeText.Text = game.PesoFormateado;

        FreeSpaceLabel.Text = $"{strings.DownloadDialogFreeSpace}:";
        FreeSpaceText.Text = GetFreeSpace(downloadPath);

        KeyLabel.Text = strings.DownloadDialogKey;

        DownloadButton.Content = strings.BtnDownload;
        CancelButton.Content = strings.BtnCancel;

        if (!string.IsNullOrEmpty(game.LogoUrl) &&
            Uri.TryCreate(game.LogoUrl, UriKind.Absolute, out var logoUri) &&
            logoUri.Scheme == Uri.UriSchemeHttps)
        {
            try
            {
                GameLogoImage.Source = new BitmapImage(logoUri);
                GameLogoImage.Visibility = Visibility.Visible;
                GameLogoFallback.Visibility = Visibility.Collapsed;
            }
            catch { }
        }

        KeyboardShortcuts.RegisterDialog(
            this,
            onConfirm: ConfirmFromShortcut,
            onCancel: () => DialogResult = false);
    }

    private void ConfirmFromShortcut()
    {
        var key = KeyBox.Text.Trim();
        _resultKey = key.Length > 0 ? key : null;
        DialogResult = true;
    }

    public static GameDownloadArgs? Show(GameInfo game, GameDownloadArgs args, string downloadPath, IStrings strings, Window? owner = null)
    {
        var dialog = new DownloadConfirmDialog(game, downloadPath, strings);

        var candidateOwner = owner ?? Application.Current.MainWindow;
        if (candidateOwner?.IsLoaded == true) dialog.Owner = candidateOwner;

        return dialog.ShowDialog() == true
            ? args with { Key = dialog._resultKey }
            : null;
    }

    private static string GetFreeSpace(string path)
    {
        try
        {
            var root = Path.GetPathRoot(path);
            if (string.IsNullOrEmpty(root)) return "—";

            var drive = new DriveInfo(root);
            var freeGB = drive.AvailableFreeSpace / (1024.0 * 1024.0 * 1024.0);

            return freeGB >= 1 ? $"{freeGB.ToString("0.#", CultureInfo.InvariantCulture)} GB" : $"{(freeGB * 1024).ToString("0", CultureInfo.InvariantCulture)} MB";
        }
        catch
        {
            return "—";
        }
    }

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ClickCount == 1) DragMove();
    }

    private void CloseButton_Click(object sender, RoutedEventArgs e)
    {
        Close();
    }

    private void DownloadButton_Click(object sender, RoutedEventArgs e)
    {
        var key = KeyBox.Text.Trim();
        _resultKey = key.Length > 0 ? key : null;
        DialogResult = true;
    }

    private void CancelButton_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = false;
    }

    private void ViewPageButton_Click(object sender, RoutedEventArgs e)
    {
        if (string.IsNullOrWhiteSpace(_gameUrl)) return;

        if (!Uri.TryCreate(_gameUrl, UriKind.Absolute, out var uri) || uri.Scheme != Uri.UriSchemeHttps) return;

        try
        {
            Process.Start(new ProcessStartInfo(uri.AbsoluteUri) { UseShellExecute = true });
        }
        catch
        {
            // Ignore if the URL can't be opened
        }
    }
}
