using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class LinkTextParserTests
{
    [Theory]
    [InlineData(null)]
    [InlineData("")]
    public void Parse_ReturnsEmpty_ForNullOrEmpty(string? text) =>
        LinkTextParser.Parse(text).ShouldBeEmpty();

    [Fact]
    public void Parse_ReturnsSinglePlainSegment_WhenNoLinks()
    {
        var segments = LinkTextParser.Parse("Texto normal sin enlaces.");

        var segment = segments.ShouldHaveSingleItem();
        segment.IsLink.ShouldBeFalse();
        segment.Text.ShouldBe("Texto normal sin enlaces.");
    }

    [Fact]
    public void Parse_DetectsHttpsUrl()
    {
        var segments = LinkTextParser.Parse("Mira https://github.com/jagobainda/LostieLauncher para más info");

        segments.Count.ShouldBe(3);
        segments[0].Text.ShouldBe("Mira ");
        segments[1].IsLink.ShouldBeTrue();
        segments[1].Text.ShouldBe("https://github.com/jagobainda/LostieLauncher");
        segments[1].Url.ShouldBe("https://github.com/jagobainda/LostieLauncher");
        segments[2].Text.ShouldBe(" para más info");
    }

    [Fact]
    public void Parse_DetectsBareDomainWithPath_AndNormalizesToHttps()
    {
        var segments = LinkTextParser.Parse("Puedes consultar el changelog completo en github.com/jagobainda/LostieLauncher/releases.");

        segments.Count.ShouldBe(3);
        segments[1].IsLink.ShouldBeTrue();
        segments[1].Text.ShouldBe("github.com/jagobainda/LostieLauncher/releases");
        segments[1].Url.ShouldBe("https://github.com/jagobainda/LostieLauncher/releases");
        segments[2].Text.ShouldBe(".");
    }

    [Fact]
    public void Parse_DetectsWwwPrefixedDomain()
    {
        var segments = LinkTextParser.Parse("Visita www.example.org ahora");

        segments[1].IsLink.ShouldBeTrue();
        segments[1].Url.ShouldBe("https://www.example.org/");
    }

    [Fact]
    public void Parse_TrimsTrailingPunctuation()
    {
        var segments = LinkTextParser.Parse("(ver https://example.com/docs), ¿vale?");

        var link = segments.Single(s => s.IsLink);
        link.Text.ShouldBe("https://example.com/docs");
        segments.Last().Text.ShouldBe("), ¿vale?");
    }

    [Fact]
    public void Parse_DetectsMultipleLinks()
    {
        var segments = LinkTextParser.Parse("Repo: github.com/a/b y web: https://example.com");

        segments.Count(s => s.IsLink).ShouldBe(2);
    }

    [Theory]
    [InlineData("Guarda el archivo.txt en la carpeta")]
    [InlineData("Escríbenos a soporte@example.com si falla")]
    [InlineData("La versión v0.9.0 ya está disponible")]
    public void Parse_IgnoresNonLinks(string text) =>
        LinkTextParser.Parse(text).ShouldAllBe(s => !s.IsLink);

    [Fact]
    public void Parse_IgnoresHttpUrls()
    {
        var segments = LinkTextParser.Parse("Antiguo mirror en http://example.com/old sin soporte");

        segments.ShouldAllBe(s => !s.IsLink);
    }
}
