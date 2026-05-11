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
        // Arrange & Act
        var result = VersionUtils.IsNewerVersion(remote, local);

        // Assert
        result.ShouldBe(expected);
    }

    [Theory]
    [InlineData("v2.0.0", "v1.0.0", true)]
    [InlineData("V2.0.0", "1.0.0", true)]
    public void IsNewerVersion_StripsLeadingVPrefixBeforeComparing(string remote, string local, bool expected)
    {
        // Arrange & Act
        var result = VersionUtils.IsNewerVersion(remote, local);

        // Assert
        result.ShouldBe(expected);
    }

    [Fact]
    public void IsNewerVersion_StripsPreReleaseSuffixBeforeComparing()
    {
        // Arrange — base version 1.2.0 must beat 1.1.0 even with -beta suffix.
        var remote = "1.2.0-beta";
        var local = "1.1.0";

        // Act
        var result = VersionUtils.IsNewerVersion(remote, local);

        // Assert
        result.ShouldBeTrue();
    }

    [Fact]
    public void IsNewerVersion_WhenBaseVersionsEqualButPreReleaseSuffixDiffers_ComparesByBaseAndReturnsFalse()
    {
        // Arrange — Both parse to 1.0.0; suffix is ignored => not newer.
        var remote = "1.0.0-beta";
        var local = "1.0.0";

        // Act
        var result = VersionUtils.IsNewerVersion(remote, local);

        // Assert
        result.ShouldBeFalse();
    }

    [Fact]
    public void IsNewerVersion_WhenBothInputsAreUnparsable_FallsBackToStringInequality()
    {
        // Arrange
        var remote = "alpha";
        var local = "beta";

        // Act
        var result = VersionUtils.IsNewerVersion(remote, local);

        // Assert
        result.ShouldBeTrue();
    }

    [Fact]
    public void IsNewerVersion_WhenBothInputsAreUnparsableAndEqual_ReturnsFalse()
    {
        // Arrange
        var remote = "alpha";
        var local = "alpha";

        // Act
        var result = VersionUtils.IsNewerVersion(remote, local);

        // Assert
        result.ShouldBeFalse();
    }

    [Fact]
    public void IsNewerVersion_WhenOnlyOneInputIsUnparsable_FallsBackToStringInequality()
    {
        // Arrange
        var remote = "1.0.0";
        var local = "garbage";

        // Act
        var result = VersionUtils.IsNewerVersion(remote, local);

        // Assert
        result.ShouldBeTrue();
    }
}
