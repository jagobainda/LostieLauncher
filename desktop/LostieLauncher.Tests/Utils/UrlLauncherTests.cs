using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class UrlLauncherTests
{
    [Fact]
    public void TryGetHttpsUri_AcceptsAbsoluteHttpsUrl()
    {
        var ok = UrlLauncher.TryGetHttpsUri("https://github.com/jagobainda/LostieLauncher", out var uri);

        ok.ShouldBeTrue();
        uri.ShouldNotBeNull();
        uri.Scheme.ShouldBe(Uri.UriSchemeHttps);
    }

    [Theory]
    [InlineData("http://example.com")]
    [InlineData("file:///C:/Windows/System32/calc.exe")]
    [InlineData("ftp://example.com/file")]
    [InlineData("javascript:alert(1)")]
    [InlineData("cmd://whatever")]
    public void TryGetHttpsUri_RejectsNonHttpsSchemes(string url)
    {
        var ok = UrlLauncher.TryGetHttpsUri(url, out var uri);

        ok.ShouldBeFalse();
        uri.ShouldBeNull();
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("github.com/jagobainda")]
    [InlineData("not a uri at all")]
    public void TryGetHttpsUri_RejectsNullEmptyOrMalformed(string? url)
    {
        var ok = UrlLauncher.TryGetHttpsUri(url, out var uri);

        ok.ShouldBeFalse();
        uri.ShouldBeNull();
    }
}
