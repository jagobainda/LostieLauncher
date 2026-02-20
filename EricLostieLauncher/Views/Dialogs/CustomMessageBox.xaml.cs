using System.Windows;
using System.Windows.Input;

namespace EricLostieLauncher.Views.Dialogs;

public enum CustomMessageBoxButton
{
    OK,
    YesNo
}

public partial class CustomMessageBox : Window
{
    private CustomMessageBox(string title, string description, CustomMessageBoxButton button)
    {
        InitializeComponent();
        TitleText.Text = title;
        DescriptionText.Text = description;

        if (button == CustomMessageBoxButton.YesNo)
        {
            OkPanel.Visibility = Visibility.Collapsed;
            YesNoPanel.Visibility = Visibility.Visible;
        }
    }

    public static bool? Show(string title, string description, CustomMessageBoxButton button = CustomMessageBoxButton.OK, Window? owner = null)
    {
        var dialog = new CustomMessageBox(title, description, button);

        var candidateOwner = owner ?? Application.Current.MainWindow;
        
        if (candidateOwner?.IsLoaded == true) dialog.Owner = candidateOwner;

        return dialog.ShowDialog();
    }

    private void TitleBar_MouseLeftButtonDown(object sender, MouseButtonEventArgs e)
    {
        if (e.ClickCount == 1) DragMove();
    }

    private void CloseButton_Click(object sender, RoutedEventArgs e)
    {
        Close();
    }

    private void OkButton_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = true;
    }

    private void YesButton_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = true;
    }

    private void NoButton_Click(object sender, RoutedEventArgs e)
    {
        DialogResult = false;
    }
}
