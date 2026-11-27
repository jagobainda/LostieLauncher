using System.Windows;

namespace LostieLauncher.Tests.Helpers;

[Collection(WpfCollection.Name)]
public class WpfApplicationFixtureTests
{
    public WpfApplicationFixtureTests(WpfApplicationFixture _) { }

    [Fact]
    public void Application_Current_IsInitialised()
    {
        var app = Application.Current;

        app.ShouldNotBeNull();
    }

    [Fact]
    public void Application_ResourceAssembly_PointsToProductionAssembly()
    {
        var asm = Application.ResourceAssembly;

        asm.ShouldNotBeNull();
        asm!.GetName().Name.ShouldBe("LostieLauncher");
    }
}
