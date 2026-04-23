using System.Windows;
using System.Windows.Input;
using LostieLauncher.ViewModels;

namespace LostieLauncher.Views.Dialogs;

public partial class WelcomeDialog : Window
{
    private WelcomeDialog()
    {
        InitializeComponent();
        DataContext = SettingsViewModel.Instance;

        KeyboardShortcuts.RegisterDialog(
            this,
            onConfirm: Close,
            onCancel: Close);
    }

    public static void Show(Window? owner = null)
    {
        var dialog = new WelcomeDialog();

        var candidateOwner = owner ?? Application.Current.MainWindow;
        if (candidateOwner?.IsLoaded == true) dialog.Owner = candidateOwner;

        dialog.ShowDialog();
    }

    private void ContinueButton_Click(object sender, RoutedEventArgs e) => Close();

    private void CloseButton_Click(object sender, RoutedEventArgs e) => Close();

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e) { if (e.ClickCount == 1) DragMove(); }

    private void RepositoryButton_Click(object sender, RoutedEventArgs e)
    {
        try
        {
            System.Diagnostics.Process.Start(new System.Diagnostics.ProcessStartInfo
            {
                FileName = SettingsViewModel.Instance.Strings.RepositoryUrl,
                UseShellExecute = true
            });
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager($"Failed to open repository URL: {SettingsViewModel.Instance.Strings.RepositoryUrl}. Exception: {ex}");
        }
    }
}
