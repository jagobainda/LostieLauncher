using System.Windows;
using System.Windows.Controls;
using LostieLauncher.Models;

namespace LostieLauncher.Views.Components;

public partial class NotificationCardComponent : UserControl
{
    public NotificationCardComponent()
    {
        InitializeComponent();
    }

    public static readonly DependencyProperty TitleProperty =
        DependencyProperty.Register(nameof(Title), typeof(string), typeof(NotificationCardComponent), new PropertyMetadata(string.Empty));

    public string Title
    {
        get => (string)GetValue(TitleProperty);
        set => SetValue(TitleProperty, value);
    }

    public static readonly DependencyProperty MessageProperty =
        DependencyProperty.Register(nameof(Message), typeof(string), typeof(NotificationCardComponent), new PropertyMetadata(string.Empty));

    public string Message
    {
        get => (string)GetValue(MessageProperty);
        set => SetValue(MessageProperty, value);
    }

    public static readonly DependencyProperty NotificationTypeProperty =
        DependencyProperty.Register(nameof(NotificationType), typeof(NotificationType), typeof(NotificationCardComponent), new PropertyMetadata(NotificationType.Info));

    public NotificationType NotificationType
    {
        get => (NotificationType)GetValue(NotificationTypeProperty);
        set => SetValue(NotificationTypeProperty, value);
    }

    public static readonly DependencyProperty DateProperty =
        DependencyProperty.Register(nameof(Date), typeof(DateTime), typeof(NotificationCardComponent));

    public DateTime Date
    {
        get => (DateTime)GetValue(DateProperty);
        set => SetValue(DateProperty, value);
    }
}
