using System.Globalization;
using System.Windows.Data;
using EricLostieLauncher.Models;

namespace EricLostieLauncher.Converters;

public class GameDownloadArgsConverter : IMultiValueConverter
{
    public object? Convert(object[] values, Type targetType, object parameter, CultureInfo culture)
    {
        if (values is [string gameId, string version])
            return new GameDownloadArgs(gameId, version);

        return null;
    }

    public object[] ConvertBack(object value, Type[] targetTypes, object parameter, CultureInfo culture) => throw new NotSupportedException();
}
