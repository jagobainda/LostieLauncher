using System.Globalization;
using System.Windows.Data;
using System.Windows.Media.Imaging;

namespace LostieLauncher.Converters;

public class UriStringToImageSourceConverter : IValueConverter
{
    public object? Convert(object value, Type targetType, object parameter, CultureInfo culture)
    {
        if (value is not string url || string.IsNullOrEmpty(url)) return null;

        if (!Uri.TryCreate(url, UriKind.Absolute, out var uri) || uri.Scheme != Uri.UriSchemeHttps) return null;

        try
        {
            return new BitmapImage(uri);
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return null;
        }
    }

    public object ConvertBack(object value, Type targetType, object parameter, CultureInfo culture) => throw new NotSupportedException();
}
