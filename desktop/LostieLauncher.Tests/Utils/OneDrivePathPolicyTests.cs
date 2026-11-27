using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class OneDrivePathPolicyTests
{
    private static readonly string[] NoRoots = [];

    [Fact]
    public void IsSynced_WhenThePathIsUnderAConfiguredRoot_ReturnsTrue()
    {
        string[] roots = [@"C:\Users\someone\OneDrive"];

        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\OneDrive\Documentos\LostieLauncher", roots);

        synced.ShouldBeTrue();
    }

    [Fact]
    public void IsSynced_WhenThePathIsTheRootItself_ReturnsTrue()
    {
        string[] roots = [@"C:\Users\someone\OneDrive"];

        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\OneDrive\", roots);

        synced.ShouldBeTrue();
    }

    [Fact]
    public void IsSynced_WhenASiblingSharesTheRootPrefix_ReturnsFalse()
    {
        string[] roots = [@"C:\Users\someone\OneDrive"];

        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\OneDriveBackup\Games", roots);

        synced.ShouldBeFalse();
    }

    [Fact]
    public void IsSynced_IgnoresBlankRoots()
    {
        string?[] roots = [null, "", "   "];

        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\AppData\Local\LostieLauncher", roots);

        synced.ShouldBeFalse();
    }

    [Theory]
    [InlineData(@"C:\Users\someone\OneDrive\Documentos\LostieLauncher")]
    [InlineData(@"C:\Users\someone\onedrive\Games")]
    [InlineData(@"C:\Users\someone\OneDrive - Contoso\Games")]
    public void IsSynced_WithNoConfiguredRoots_FallsBackToTheFolderName(string path)
    {
        var synced = OneDrivePathPolicy.IsSynced(path, NoRoots);

        synced.ShouldBeTrue();
    }

    [Theory]
    [InlineData(@"C:\Users\someone\AppData\Local\LostieLauncher")]
    [InlineData(@"D:\Games\LostieLauncher")]
    [InlineData(@"C:\Users\someone\OneDriveBackup\LostieLauncher")]
    [InlineData(@"C:\Users\someone\Documents\MyOneDrive\LostieLauncher")]
    public void IsSynced_ForAnUnsyncedPath_ReturnsFalse(string path)
    {
        var synced = OneDrivePathPolicy.IsSynced(path, NoRoots);

        synced.ShouldBeFalse();
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    public void IsSynced_WithABlankPath_ReturnsFalse(string? path)
    {
        var synced = OneDrivePathPolicy.IsSynced(path, NoRoots);

        synced.ShouldBeFalse();
    }

    [Fact]
    public void IsSynced_WithAMalformedPath_ReturnsFalseInsteadOfThrowing()
    {
        var synced = OneDrivePathPolicy.IsSynced("C:\\inva|id\0path", NoRoots);

        synced.ShouldBeFalse();
    }

    [Fact]
    public void IsSynced_WithAMalformedRoot_SkipsItAndKeepsChecking()
    {
        string[] roots = ["C:\\inva|id\0root", @"C:\Users\someone\OneDrive"];

        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\OneDrive\Games", roots);

        synced.ShouldBeTrue();
    }
}
