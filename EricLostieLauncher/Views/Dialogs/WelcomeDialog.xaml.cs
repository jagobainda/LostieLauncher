using System.Windows;
using System.Windows.Input;
using EricLostieLauncher.Content;

namespace EricLostieLauncher.Views.Dialogs;

public partial class WelcomeDialog : Window
{
    private readonly IStrings _strings;

    private WelcomeDialog(IStrings strings)
    {
        _strings = strings;
        InitializeComponent();

        TitleText.Text = strings.WelcomeDialogTitle;
        DescriptionText.Text = strings.WelcomeDialogDescription;
        ContinueButton.Content = strings.WelcomeDialogContinue;
    }

    public static void Show(IStrings strings, Window? owner = null)
    {
        var dialog = new WelcomeDialog(strings);

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
                FileName = _strings.RepositoryUrl,
                UseShellExecute = true
            });
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager($"Failed to open repository URL: {_strings.RepositoryUrl}. Exception: {ex}");
        }
    }
}
