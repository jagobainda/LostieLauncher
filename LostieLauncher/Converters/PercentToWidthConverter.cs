using System.Globalization;
using System.Windows.Data;

namespace LostieLauncher.Converters;

public class PercentToWidthConverter : IValueConverter
{
    public object Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        if (value is double percent && parameter is string s && double.TryParse(s, CultureInfo.InvariantCulture, out var maxWidth))
            return Math.Clamp(percent, 0, 100) / 100.0 * maxWidth;
        return 0.0;
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture)
        => throw new NotSupportedException();
}
