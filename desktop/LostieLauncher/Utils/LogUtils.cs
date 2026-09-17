using System.Globalization;
using System.IO;
using System.Text;

namespace LostieLauncher.Utils;

public static class Logs
{
    private static readonly Lock Sync = new();
    private static readonly string LogDirectory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), AppDomain.CurrentDomain.FriendlyName, "logs");

    private const string LogExtension = ".log";

    private const long MaxFileSizeBytes = 10L * 1024 * 1024;

    private const int RetentionMonths = 6;

    private static volatile bool _directoryEnsured;

    private static string? _activeMonth;
    private static int _activeIndex;
    private static long _activeSize;
    private static string? _activePath;

    public static void ErrorLogManager(Exception e) => AddLog(CreateLogString("ERROR", e.ToString()));

    public static void ErrorLogManager(string errorPersonalizado) => AddLog(CreateLogString("ERROR", errorPersonalizado));

    public static void DebugLogManager(string mensaje) => AddLog(CreateLogString("DEBUG", mensaje));

    public static void InfoLogManager(string mensaje) => AddLog(CreateLogString("INFO", mensaje));

    public static void PurgeOldLogs()
    {
        try
        {
            PurgeExpiredLogs(LogDirectory, DateTimeOffset.Now, RetentionMonths);
        }
        catch
        {
            // Ignored
        }
    }

    private static string CreateLogString(string tipoLog, string mensajeLog)
    {
        var timestamp = DateTimeOffset.Now.ToString("yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture);
        var sb = new StringBuilder(64 + mensajeLog.Length);
        sb.Append(timestamp).Append(" [").Append(tipoLog).Append("] -> ").Append(mensajeLog);
        return sb.ToString();
    }

    private static void AddLog(string nuevoLog)
    {
        try
        {
            var payload = nuevoLog + Environment.NewLine;

            lock (Sync)
            {
                EnsureDirectory();

                var month = DateTimeOffset.Now.ToString("yyyy-MM", CultureInfo.InvariantCulture);
                var path = ResolveActivePath(month);

                File.AppendAllText(path, payload, Encoding.UTF8);
                _activeSize += Encoding.UTF8.GetByteCount(payload);
            }
        }
        catch
        {
            // Ignored — losing a log line is always preferable to crashing the caller.
        }
    }

    private static string ResolveActivePath(string month)
    {
        (_activePath, _activeMonth, _activeIndex, _activeSize) =
            ResolveActiveFile(LogDirectory, month, MaxFileSizeBytes, _activePath, _activeMonth, _activeIndex, _activeSize);

        return _activePath;
    }

    internal static (string Path, string Month, int Index, long Size) ResolveActiveFile(
        string directory, string month, long maxBytes,
        string? activePath, string? activeMonth, int activeIndex, long activeSize)
    {
        if (activePath is null || activeMonth != month)
        {
            var (index, size) = ProbeMonth(directory, month, maxBytes);
            return (Path.Combine(directory, BuildLogFileName(month, index)), month, index, size);
        }

        if (activeSize >= maxBytes)
        {
            var rolled = activeIndex + 1;
            return (Path.Combine(directory, BuildLogFileName(month, rolled)), month, rolled, 0);
        }

        return (activePath, month, activeIndex, activeSize);
    }

    private static void EnsureDirectory()
    {
        if (_directoryEnsured) return;

        Directory.CreateDirectory(LogDirectory);
        _directoryEnsured = true;
    }

    internal static string BuildLogFileName(string month, int index) =>
        index <= 0 ? month + LogExtension : $"{month}.{index}{LogExtension}";

    internal static bool TryParseLogIndex(string fileName, string month, out int index)
    {
        index = 0;
        if (fileName.Equals(month + LogExtension, StringComparison.OrdinalIgnoreCase)) return true;

        var prefix = month + ".";
        if (!fileName.StartsWith(prefix, StringComparison.OrdinalIgnoreCase)
            || !fileName.EndsWith(LogExtension, StringComparison.OrdinalIgnoreCase))
        {
            return false;
        }

        var middle = fileName[prefix.Length..^LogExtension.Length];
        return int.TryParse(middle, NumberStyles.None, CultureInfo.InvariantCulture, out index) && index > 0;
    }

    internal static (int Index, long Size) ProbeMonth(string directory, string month, long maxBytes)
    {
        var highest = -1;
        foreach (var path in EnumerateLogFiles(directory))
        {
            if (TryParseLogIndex(Path.GetFileName(path), month, out var idx) && idx > highest) highest = idx;
        }

        if (highest < 0) return (0, 0);

        var size = GetFileLength(Path.Combine(directory, BuildLogFileName(month, highest)));
        return size >= maxBytes ? (highest + 1, 0) : (highest, size);
    }

    internal static IReadOnlyList<string> SelectExpiredLogFiles(IEnumerable<string> fileNames, DateTimeOffset now, int retentionMonths)
    {
        var cutoff = new DateTime(now.Year, now.Month, 1).AddMonths(-retentionMonths);
        var expired = new List<string>();

        foreach (var fileName in fileNames)
        {
            if (TryParseMonth(fileName, out var month) && month < cutoff) expired.Add(fileName);
        }

        return expired;
    }

    internal static int PurgeExpiredLogs(string directory, DateTimeOffset now, int retentionMonths)
    {
        if (!Directory.Exists(directory)) return 0;

        var fileNames = EnumerateLogFiles(directory).Select(Path.GetFileName).OfType<string>();
        var removed = 0;

        foreach (var fileName in SelectExpiredLogFiles(fileNames, now, retentionMonths))
        {
            try
            {
                File.Delete(Path.Combine(directory, fileName));
                removed++;
            }
            catch (Exception ex)
            {
                ErrorLogManager(ex);
            }
        }

        return removed;
    }

    private static bool TryParseMonth(string fileName, out DateTime month)
    {
        var dot = fileName.IndexOf('.');
        var prefix = dot < 0 ? fileName : fileName[..dot];
        return DateTime.TryParseExact(prefix, "yyyy-MM", CultureInfo.InvariantCulture, DateTimeStyles.None, out month);
    }

    private static IEnumerable<string> EnumerateLogFiles(string directory) =>
        Directory.Exists(directory) ? Directory.EnumerateFiles(directory, "*" + LogExtension) : [];

    private static long GetFileLength(string path)
    {
        try
        {
            return new FileInfo(path).Length;
        }
        catch
        {
            return 0;
        }
    }
}
