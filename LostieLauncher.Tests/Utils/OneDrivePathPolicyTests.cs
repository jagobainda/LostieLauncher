using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class OneDrivePathPolicyTests
{
    private static readonly string[] NoRoots = [];

    // -------------------- Explicit sync roots --------------------

    [Fact]
    public void IsSynced_WhenThePathIsUnderAConfiguredRoot_ReturnsTrue()
    {
        // Arrange — the exact shape from the user report: Documents redirected into OneDrive.
        string[] roots = [@"C:\Users\someone\OneDrive"];

        // Act
        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\OneDrive\Documentos\LostieLauncher", roots);

        // Assert
        synced.ShouldBeTrue();
    }

    [Fact]
    public void IsSynced_WhenThePathIsTheRootItself_ReturnsTrue()
    {
        // Arrange
        string[] roots = [@"C:\Users\someone\OneDrive"];

        // Act
        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\OneDrive\", roots);

        // Assert
        synced.ShouldBeTrue();
    }

    [Fact]
    public void IsSynced_WhenASiblingSharesTheRootPrefix_ReturnsFalse()
    {
        // Arrange — "OneDriveBackup" merely starts with the root's text; it is not inside it.
        string[] roots = [@"C:\Users\someone\OneDrive"];

        // Act
        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\OneDriveBackup\Games", roots);

        // Assert
        synced.ShouldBeFalse();
    }

    [Fact]
    public void IsSynced_IgnoresBlankRoots()
    {
        // Arrange — an account that is not signed in leaves its variable empty or unset.
        string?[] roots = [null, "", "   "];

        // Act
        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\AppData\Local\LostieLauncher", roots);

        // Assert
        synced.ShouldBeFalse();
    }

    // -------------------- Folder-name fallback --------------------

    [Theory]
    [InlineData(@"C:\Users\someone\OneDrive\Documentos\LostieLauncher")]
    [InlineData(@"C:\Users\someone\onedrive\Games")]
    [InlineData(@"C:\Users\someone\OneDrive - Contoso\Games")]
    public void IsSynced_WithNoConfiguredRoots_FallsBackToTheFolderName(string path)
    {
        // Arrange & Act — a second account or a hand-moved sync root is not advertised by the
        // environment, but the folder is still literally named OneDrive.
        var synced = OneDrivePathPolicy.IsSynced(path, NoRoots);

        // Assert
        synced.ShouldBeTrue();
    }

    [Theory]
    [InlineData(@"C:\Users\someone\AppData\Local\LostieLauncher")]
    [InlineData(@"D:\Games\LostieLauncher")]
    [InlineData(@"C:\Users\someone\OneDriveBackup\LostieLauncher")]
    [InlineData(@"C:\Users\someone\Documents\MyOneDrive\LostieLauncher")]
    public void IsSynced_ForAnUnsyncedPath_ReturnsFalse(string path)
    {
        // Arrange & Act
        var synced = OneDrivePathPolicy.IsSynced(path, NoRoots);

        // Assert
        synced.ShouldBeFalse();
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    public void IsSynced_WithABlankPath_ReturnsFalse(string? path)
    {
        // Arrange & Act — nothing to warn about, and the check must not throw.
        var synced = OneDrivePathPolicy.IsSynced(path, NoRoots);

        // Assert
        synced.ShouldBeFalse();
    }

    [Fact]
    public void IsSynced_WithAMalformedPath_ReturnsFalseInsteadOfThrowing()
    {
        // Arrange — a hand-edited settings file can hold anything.
        var synced = OneDrivePathPolicy.IsSynced("C:\\inva|id\0path", NoRoots);

        // Assert
        synced.ShouldBeFalse();
    }

    [Fact]
    public void IsSynced_WithAMalformedRoot_SkipsItAndKeepsChecking()
    {
        // Arrange — one unusable root must not hide a later, valid one.
        string[] roots = ["C:\\inva|id\0root", @"C:\Users\someone\OneDrive"];

        // Act
        var synced = OneDrivePathPolicy.IsSynced(@"C:\Users\someone\OneDrive\Games", roots);

        // Assert
        synced.ShouldBeTrue();
    }
}
