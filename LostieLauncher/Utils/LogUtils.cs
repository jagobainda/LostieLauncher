using System.Globalization;
using System.IO;
using System.Text;

namespace LostieLauncher.Utils;

public static class Logs
{
    private static readonly Lock Sync = new();
    private static readonly string LogDirectory = Path.Combine(Environment.GetFolderPath(Environment.SpecialFolder.LocalApplicationData), AppDomain.CurrentDomain.FriendlyName, "logs");

    public static void ErrorLogManager(Exception e) => AddLog(CreateLogString("ERROR", e.ToString()));

    public static void ErrorLogManager(string errorPersonalizado) => AddLog(CreateLogString("ERROR", errorPersonalizado));

    public static void DebugLogManager(string mensaje) => AddLog(CreateLogString("DEBUG", mensaje));

    public static void InfoLogManager(string mensaje) => AddLog(CreateLogString("INFO", mensaje));

    private static string CreateLogString(string tipoLog, string mensajeLog)
    {
        var timestamp = DateTimeOffset.Now.ToString("yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture);
        var sb = new StringBuilder(64 + mensajeLog.Length);
        sb.Append(timestamp).Append(" [").Append(tipoLog).Append("] -> ").Append(mensajeLog);
        return sb.ToString();
    }

    private static void AddLog(string nuevoLog)
    {
        var archivoLog = LoadCurrentMonthLog();

        lock (Sync)
        {
            File.AppendAllText(archivoLog, nuevoLog + Environment.NewLine, Encoding.UTF8);
        }
    }

    private static string LoadCurrentMonthLog()
    {
        Directory.CreateDirectory(LogDirectory);

        var nombreArchivo = DateTimeOffset.Now.ToString("yyyy-MM", CultureInfo.InvariantCulture) + ".log";
        return Path.Combine(LogDirectory, nombreArchivo);
    }
}
