using System.Globalization;
using System.Windows;
using System.Windows.Data;

namespace EricLostieLauncher.Converters;

public class EnumToVisibilityConverter : IValueConverter
{
    public string TargetValue { get; set; } = string.Empty;

    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        if (value is null) return Visibility.Collapsed;

        return value.ToString() == TargetValue ? Visibility.Visible : Visibility.Collapsed;
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotSupportedException();
}
