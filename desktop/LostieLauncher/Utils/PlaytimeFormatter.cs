namespace LostieLauncher.Utils;

public static class PlaytimeFormatter
{
    public static string Format(int minutes)
    {
        if (minutes <= 0) return string.Empty;
        if (minutes < 60) return $"{minutes} min";
        var h = minutes / 60;
        var m = minutes % 60;
        return m > 0 ? $"{h} h {m} min" : $"{h} h";
    }
}
