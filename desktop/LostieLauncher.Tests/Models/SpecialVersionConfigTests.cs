using LostieLauncher.Models;

namespace LostieLauncher.Tests.Models;

public class SpecialVersionConfigTests
{
    private const string ValidGuid = "11111111-2222-3333-4444-555555555555";

    private static string ValidContent(string? overrideKey = null, string? overrideValue = null)
    {
        var values = new Dictionary<string, string>
        {
            ["sha256"] = "abc",
            ["tipo"] = "beta",
            ["juego-principal"] = ValidGuid,
            ["vers"] = "1.2.3",
            ["archivo"] = "game.zip",
        };
        if (overrideKey is not null) values[overrideKey] = overrideValue ?? string.Empty;
        return string.Join('\n', values.Select(kv => $"{kv.Key}={kv.Value}"));
    }

    [Fact]
    public void Parse_WithAllRequiredFields_ReturnsPopulatedConfig()
    {
        var content = ValidContent();

        var config = SpecialVersionConfig.Parse(content);

        config.ShouldNotBeNull();
        config!.Sha256.ShouldBe("abc");
        config.Tipo.ShouldBe("beta");
        config.JuegoPrincipal.ShouldBe(Guid.Parse(ValidGuid));
        config.Version.ShouldBe("1.2.3");
        config.Archivo.ShouldBe("game.zip");
    }

    [Fact]
    public void Parse_AcceptsCarriageReturnsAndExtraWhitespacePerLine()
    {
        var content = "  sha256 = abc  \n tipo=beta\njuego-principal=" + ValidGuid + "\nvers=1.0.0\narchivo=g.zip\n";

        var config = SpecialVersionConfig.Parse(content);

        config.ShouldNotBeNull();
        config!.Sha256.ShouldBe("abc");
        config.Tipo.ShouldBe("beta");
    }

    [Fact]
    public void Parse_KeysAreCaseInsensitive()
    {
        var content = "SHA256=abc\nTipo=beta\nJUEGO-PRINCIPAL=" + ValidGuid + "\nVers=2.0.0\nArchivo=g.zip";

        var config = SpecialVersionConfig.Parse(content);

        config.ShouldNotBeNull();
        config!.Version.ShouldBe("2.0.0");
    }

    [Theory]
    [InlineData("sha256")]
    [InlineData("tipo")]
    [InlineData("juego-principal")]
    [InlineData("vers")]
    [InlineData("archivo")]
    public void Parse_WhenRequiredKeyMissing_ReturnsNull(string missingKey)
    {
        var values = new Dictionary<string, string>
        {
            ["sha256"] = "abc",
            ["tipo"] = "beta",
            ["juego-principal"] = ValidGuid,
            ["vers"] = "1.0.0",
            ["archivo"] = "g.zip",
        };
        values.Remove(missingKey);
        var content = string.Join('\n', values.Select(kv => $"{kv.Key}={kv.Value}"));

        var config = SpecialVersionConfig.Parse(content);

        config.ShouldBeNull();
    }

    [Fact]
    public void Parse_WhenJuegoPrincipalIsNotAGuid_ReturnsNull()
    {
        var content = ValidContent(overrideKey: "juego-principal", overrideValue: "not-a-guid");

        var config = SpecialVersionConfig.Parse(content);

        config.ShouldBeNull();
    }

    [Fact]
    public void Parse_IgnoresLinesWithoutEqualsSignAndEmptyLines()
    {
        var content = $"# this is a comment\n\nsha256=abc\ntipo=beta\njuego-principal={ValidGuid}\nvers=1.0.0\narchivo=g.zip\nbroken-line";

        var config = SpecialVersionConfig.Parse(content);

        config.ShouldNotBeNull();
    }

    [Fact]
    public void Parse_WhenContentIsEmpty_ReturnsNull()
    {
        var content = string.Empty;

        var config = SpecialVersionConfig.Parse(content);

        config.ShouldBeNull();
    }
}
