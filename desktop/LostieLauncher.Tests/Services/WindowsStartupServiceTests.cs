using LostieLauncher.Services;

namespace LostieLauncher.Tests.Services;

public class WindowsStartupServiceTests
{
    [Fact]
    public void TryBuildStartupCommand_WithValidPath_QuotesTheExecutablePath()
    {
        var ok = WindowsStartupService.TryBuildStartupCommand(@"C:\Apps\LostieLauncher.exe", out var command);

        ok.ShouldBeTrue();
        command.ShouldBe("\"C:\\Apps\\LostieLauncher.exe\"");
    }

    [Fact]
    public void TryBuildStartupCommand_WithNullPath_FailsWithoutWritingGarbage()
    {
        var ok = WindowsStartupService.TryBuildStartupCommand(null, out var command);

        ok.ShouldBeFalse();
        command.ShouldBeEmpty();
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void TryBuildStartupCommand_WithEmptyOrWhitespacePath_Fails(string path)
    {
        var ok = WindowsStartupService.TryBuildStartupCommand(path, out var command);

        ok.ShouldBeFalse();
        command.ShouldBeEmpty();
    }
}
