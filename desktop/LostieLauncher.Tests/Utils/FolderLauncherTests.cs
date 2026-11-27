using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public sealed class FolderLauncherTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("folderlauncher");

    public void Dispose() => _temp.Dispose();

    [Fact]
    public void TryGetExistingDirectory_AcceptsExistingDirectory()
    {
        var ok = FolderLauncher.TryGetExistingDirectory(_temp.Path, out var directory);

        ok.ShouldBeTrue();
        directory.ShouldBe(_temp.Path);
    }

    [Fact]
    public void TryGetExistingDirectory_AcceptsDirectoryWithCommaInName()
    {
        var withComma = _temp.Combine("My Game, Edition");
        Directory.CreateDirectory(withComma);

        var ok = FolderLauncher.TryGetExistingDirectory(withComma, out var directory);

        ok.ShouldBeTrue();
        directory.ShouldBe(withComma);
    }

    [Fact]
    public void TryGetExistingDirectory_RejectsNonExistentPath()
    {
        var missing = _temp.Combine("does-not-exist");

        var ok = FolderLauncher.TryGetExistingDirectory(missing, out var directory);

        ok.ShouldBeFalse();
        directory.ShouldBeNull();
    }

    [Fact]
    public void TryGetExistingDirectory_RejectsFilePath()
    {
        var file = _temp.Combine("file.txt");
        File.WriteAllText(file, "x");

        var ok = FolderLauncher.TryGetExistingDirectory(file, out var directory);

        ok.ShouldBeFalse();
        directory.ShouldBeNull();
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    public void TryGetExistingDirectory_RejectsNullEmptyOrWhitespace(string? path)
    {
        var ok = FolderLauncher.TryGetExistingDirectory(path, out var directory);

        ok.ShouldBeFalse();
        directory.ShouldBeNull();
    }
}
