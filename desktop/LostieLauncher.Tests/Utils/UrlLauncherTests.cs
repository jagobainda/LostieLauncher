using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class UrlLauncherTests
{
    // -------------------- TryGetHttpsUri --------------------

    [Fact]
    public void TryGetHttpsUri_AcceptsAbsoluteHttpsUrl()
    {
        // Arrange & Act
        var ok = UrlLauncher.TryGetHttpsUri("https://github.com/jagobainda/LostieLauncher", out var uri);

        // Assert
        ok.ShouldBeTrue();
        uri.ShouldNotBeNull();
        uri.Scheme.ShouldBe(Uri.UriSchemeHttps);
    }

    [Theory]
    [InlineData("http://example.com")]      // plain HTTP is rejected (only HTTPS allowed)
    [InlineData("file:///C:/Windows/System32/calc.exe")]  // the BUG-063 exploit vector
    [InlineData("ftp://example.com/file")]
    [InlineData("javascript:alert(1)")]
    [InlineData("cmd://whatever")]
    public void TryGetHttpsUri_RejectsNonHttpsSchemes(string url)
    {
        // Act
        var ok = UrlLauncher.TryGetHttpsUri(url, out var uri);

        // Assert — the shell must never receive a non-HTTPS scheme.
        ok.ShouldBeFalse();
        uri.ShouldBeNull();
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("github.com/jagobainda")]   // relative / no scheme
    [InlineData("not a uri at all")]
    public void TryGetHttpsUri_RejectsNullEmptyOrMalformed(string? url)
    {
        // Act
        var ok = UrlLauncher.TryGetHttpsUri(url, out var uri);

        // Assert
        ok.ShouldBeFalse();
        uri.ShouldBeNull();
    }
}
