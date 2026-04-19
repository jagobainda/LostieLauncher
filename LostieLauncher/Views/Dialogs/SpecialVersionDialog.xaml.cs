using System.Windows;
using System.Windows.Input;
using LostieLauncher.Content;

namespace LostieLauncher.Views.Dialogs;

public partial class SpecialVersionDialog : Window
{
    private string? _resultKey;

    private SpecialVersionDialog(IStrings strings)
    {
        InitializeComponent();

        TitleText.Text = strings.SpecialVersionDialogTitle;
        DescriptionText.Text = strings.SpecialVersionDialogDescription;
        KeyLabel.Text = strings.SpecialVersionDialogKeyLabel;
        ConfirmButton.Content = strings.BtnConfirm;
        CancelButton.Content = strings.BtnCancel;
    }

    public static string? Show(IStrings strings, Window? owner = null)
    {
        var dialog = new SpecialVersionDialog(strings);

        var candidateOwner = owner ?? Application.Current.MainWindow;
        if (candidateOwner?.IsLoaded == true) dialog.Owner = candidateOwner;

        return dialog.ShowDialog() == true ? dialog._resultKey : null;
    }

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ClickCount == 1) DragMove();
    }

    private void CloseButton_Click(object sender, RoutedEventArgs e)
    {
        Close();
    }

    private void ConfirmButton_Click(object sender, RoutedEventArgs e)
    {
        var key = KeyBox.Text.Trim();
        if (key.Length == 0) return;

        _resultKey = key;
        DialogResult = true;
    }

    private void CancelButton_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = false;
    }
}
