using System.IO;

namespace LostieLauncher.Utils;

public static class DownloadArtifacts
{
    public static int Delete(string zipPath)
    {
        var partPath = DownloadPathUtils.GetPartFilePath(zipPath);

        return DeleteFile(partPath)
             + DeleteFile(zipPath)
             + DeleteFile(DownloadPathUtils.GetMetaFilePath(partPath));
    }

    private static int DeleteFile(string path)
    {
        try
        {
            if (!File.Exists(path)) return 0;

            File.Delete(path);
            return 1;
        }
        catch (Exception ex)
        {
            Logs.ErrorLogManager(ex);
            return 0;
        }
    }
}
