using System.Windows;

namespace LostieLauncher.Tests.Helpers;

[Collection(WpfCollection.Name)]
public class WpfApplicationFixtureTests
{
    public WpfApplicationFixtureTests(WpfApplicationFixture _) { }

    [Fact]
    public void Application_Current_IsInitialised()
    {
        // Arrange & Act
        var app = Application.Current;

        // Assert
        app.ShouldNotBeNull();
    }

    [Fact]
    public void Application_ResourceAssembly_PointsToProductionAssembly()
    {
        // Arrange & Act
        var asm = Application.ResourceAssembly;

        // Assert — required so pack:// URIs in production code resolve against the
        // LostieLauncher assembly (not the test runner exe).
        asm.ShouldNotBeNull();
        asm!.GetName().Name.ShouldBe("LostieLauncher");
    }
}
