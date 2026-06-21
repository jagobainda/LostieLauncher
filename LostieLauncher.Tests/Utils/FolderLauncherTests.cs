using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public sealed class FolderLauncherTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("folderlauncher");

    public void Dispose() => _temp.Dispose();

    // -------------------- TryGetExistingDirectory --------------------

    [Fact]
    public void TryGetExistingDirectory_AcceptsExistingDirectory()
    {
        // Act
        var ok = FolderLauncher.TryGetExistingDirectory(_temp.Path, out var directory);

        // Assert
        ok.ShouldBeTrue();
        directory.ShouldBe(_temp.Path);
    }

    [Fact]
    public void TryGetExistingDirectory_AcceptsDirectoryWithCommaInName()
    {
        // Arrange — a comma is valid in NTFS names and is exactly what broke explorer.exe (BUG-044).
        var withComma = _temp.Combine("My Game, Edition");
        Directory.CreateDirectory(withComma);

        // Act
        var ok = FolderLauncher.TryGetExistingDirectory(withComma, out var directory);

        // Assert
        ok.ShouldBeTrue();
        directory.ShouldBe(withComma);
    }

    [Fact]
    public void TryGetExistingDirectory_RejectsNonExistentPath()
    {
        // Arrange
        var missing = _temp.Combine("does-not-exist");

        // Act
        var ok = FolderLauncher.TryGetExistingDirectory(missing, out var directory);

        // Assert
        ok.ShouldBeFalse();
        directory.ShouldBeNull();
    }

    [Fact]
    public void TryGetExistingDirectory_RejectsFilePath()
    {
        // Arrange — a file is not a directory; the shell must not be handed one here.
        var file = _temp.Combine("file.txt");
        File.WriteAllText(file, "x");

        // Act
        var ok = FolderLauncher.TryGetExistingDirectory(file, out var directory);

        // Assert
        ok.ShouldBeFalse();
        directory.ShouldBeNull();
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    public void TryGetExistingDirectory_RejectsNullEmptyOrWhitespace(string? path)
    {
        // Act
        var ok = FolderLauncher.TryGetExistingDirectory(path, out var directory);

        // Assert
        ok.ShouldBeFalse();
        directory.ShouldBeNull();
    }
}
