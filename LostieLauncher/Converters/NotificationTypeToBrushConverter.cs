using LostieLauncher.Models;
using System.Globalization;
using System.Windows.Data;
using System.Windows.Media;
using Color = System.Windows.Media.Color;

namespace LostieLauncher.Converters;

public class NotificationTypeToBrushConverter : IValueConverter
{
    private static readonly SolidColorBrush InfoBrush = new(Color.FromRgb(0x4C, 0xAF, 0x50));
    private static readonly SolidColorBrush WarningBrush = new(Color.FromRgb(0xFF, 0xC1, 0x07));
    private static readonly SolidColorBrush ExclamationBrush = new(Color.FromRgb(0xF4, 0x43, 0x36));

    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        return value is NotificationType type ? type switch
        {
            NotificationType.Warning => WarningBrush,
            NotificationType.Exclamation => ExclamationBrush,
            _ => InfoBrush
        } : InfoBrush;
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}
