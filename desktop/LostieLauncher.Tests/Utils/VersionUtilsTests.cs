using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class VersionUtilsTests
{
    [Theory]
    [InlineData("1.2.0", "1.1.0", true)]
    [InlineData("1.1.0", "1.2.0", false)]
    [InlineData("1.0.0", "1.0.0", false)]
    public void IsNewerVersion_ComparesNumericallyWhenBothParse(string remote, string local, bool expected)
    {
        var result = VersionUtils.IsNewerVersion(remote, local);

        result.ShouldBe(expected);
    }

    [Theory]
    [InlineData("v2.0.0", "v1.0.0", true)]
    [InlineData("V2.0.0", "1.0.0", true)]
    public void IsNewerVersion_StripsLeadingVPrefixBeforeComparing(string remote, string local, bool expected)
    {
        var result = VersionUtils.IsNewerVersion(remote, local);

        result.ShouldBe(expected);
    }

    [Fact]
    public void IsNewerVersion_StripsPreReleaseSuffixBeforeComparing()
    {
        var remote = "1.2.0-beta";
        var local = "1.1.0";

        var result = VersionUtils.IsNewerVersion(remote, local);

        result.ShouldBeTrue();
    }

    [Fact]
    public void IsNewerVersion_WhenBaseVersionsEqualButPreReleaseSuffixDiffers_ComparesByBaseAndReturnsFalse()
    {
        var remote = "1.0.0-beta";
        var local = "1.0.0";

        var result = VersionUtils.IsNewerVersion(remote, local);

        result.ShouldBeFalse();
    }

    [Theory]
    [InlineData("alpha", "beta")]
    [InlineData("beta", "alpha")]
    [InlineData("alpha", "alpha")]
    public void IsNewerVersion_WhenEitherInputIsUnparsable_FailsClosedAndReturnsFalse(string remote, string local)
    {
        var result = VersionUtils.IsNewerVersion(remote, local);

        result.ShouldBeFalse();
    }

    [Fact]
    public void IsNewerVersion_WhenBaseVersionsParseButCaseDiffersInSuffix_ComparesByBaseAndReturnsFalse()
    {
        var remote = "v1.0-BETA";
        var local = "v1.0-beta";

        var result = VersionUtils.IsNewerVersion(remote, local);

        result.ShouldBeFalse();
    }

    [Theory]
    [InlineData("1.0.0", "garbage")]
    [InlineData("garbage", "1.0.0")]
    public void IsNewerVersion_WhenOnlyOneInputIsUnparsable_FailsClosedAndReturnsFalse(string remote, string local)
    {
        var result = VersionUtils.IsNewerVersion(remote, local);

        result.ShouldBeFalse();
    }

    [Theory]
    [InlineData("", "")]
    [InlineData("1.0.0", "")]
    [InlineData("", "1.0.0")]
    public void IsNewerVersion_WhenEitherInputIsEmpty_FailsClosedAndReturnsFalse(string remote, string local)
    {
        var result = VersionUtils.IsNewerVersion(remote, local);

        result.ShouldBeFalse();
    }

    [Fact]
    public void IsNewerVersion_WhenInputIsNull_FailsClosedAndReturnsFalse()
    {
        var result = VersionUtils.IsNewerVersion(null!, "1.0.0");

        result.ShouldBeFalse();
    }

    [Theory]
    [InlineData("v4.0.3")]
    [InlineData("V4.0.3")]
    [InlineData("4.0.3")]
    public void FormatDisplayVersion_EmitsExactlyOneVPrefixRegardlessOfSourcePrefix(string version)
    {
        var result = VersionUtils.FormatDisplayVersion(version);

        result.ShouldBe("v4.0.3");
    }

    [Fact]
    public void FormatDisplayVersion_WhenSourceAlreadyCarriesPrefix_DoesNotDuplicateIt()
    {
        var version = "v2.18.0";

        var result = VersionUtils.FormatDisplayVersion(version);

        result.ShouldBe("v2.18.0");
        result.ShouldNotStartWith("vv");
    }

    [Fact]
    public void FormatDisplayVersion_CollapsesAnAlreadyDuplicatedPrefix()
    {
        var version = "vv2.18.0";

        var result = VersionUtils.FormatDisplayVersion(version);

        result.ShouldBe("v2.18.0");
    }

    [Fact]
    public void FormatDisplayVersion_PreservesPreReleaseSuffix()
    {
        var version = "v1.2.0-beta";

        var result = VersionUtils.FormatDisplayVersion(version);

        result.ShouldBe("v1.2.0-beta");
    }

    [Fact]
    public void FormatDisplayVersion_TrimsSurroundingWhitespace()
    {
        var version = "  v2.11.0  ";

        var result = VersionUtils.FormatDisplayVersion(version);

        result.ShouldBe("v2.11.0");
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("   ")]
    [InlineData("v")]
    public void FormatDisplayVersion_WhenThereIsNoVersionToShow_ReturnsUnknownInsteadOfALoneV(string? version)
    {
        var result = VersionUtils.FormatDisplayVersion(version);

        result.ShouldBe("unknown");
    }
}
