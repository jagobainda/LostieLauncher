using LostieLauncher.Services;

namespace LostieLauncher.Tests.Services;

public class WindowsStartupServiceTests
{
    // The registry I/O itself (HKCU\...\Run) is global machine state and is not exercised here,
    // consistent with the rest of the suite. The defect of BUG-052 lived in the command building:
    // a null/empty ProcessPath wrote a garbage `""` startup entry. That logic is extracted into
    // TryBuildStartupCommand and covered below.

    [Fact]
    public void TryBuildStartupCommand_WithValidPath_QuotesTheExecutablePath()
    {
        // Act
        var ok = WindowsStartupService.TryBuildStartupCommand(@"C:\Apps\LostieLauncher.exe", out var command);

        // Assert
        ok.ShouldBeTrue();
        command.ShouldBe("\"C:\\Apps\\LostieLauncher.exe\"");
    }

    [Fact]
    public void TryBuildStartupCommand_WithNullPath_FailsWithoutWritingGarbage()
    {
        // Act — the BUG-052 regression: a null ProcessPath must NOT produce a writable command.
        var ok = WindowsStartupService.TryBuildStartupCommand(null, out var command);

        // Assert
        ok.ShouldBeFalse();
        command.ShouldBeEmpty();
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public void TryBuildStartupCommand_WithEmptyOrWhitespacePath_Fails(string path)
    {
        // Act
        var ok = WindowsStartupService.TryBuildStartupCommand(path, out var command);

        // Assert
        ok.ShouldBeFalse();
        command.ShouldBeEmpty();
    }
}
