using LostieLauncher.Utils;
using System.Globalization;
using System.Reflection;

namespace LostieLauncher.Tests.Utils;

/// <summary>
/// Coverage for the pure helpers inside <see cref="Logs"/>. The public log methods cannot be unit-tested
/// because they write to <c>%LOCALAPPDATA%</c> directly (no path abstraction in production), but the
/// private <c>CreateLogString</c> formatter is deterministic and worth pinning.
/// </summary>
public class LogsTests
{
    private static string InvokeCreateLogString(string tipo, string mensaje)
    {
        var method = typeof(Logs).GetMethod("CreateLogString", BindingFlags.Static | BindingFlags.NonPublic)
            ?? throw new InvalidOperationException("CreateLogString not found.");
        return (string)method.Invoke(null, [tipo, mensaje])!;
    }

    [Theory]
    [InlineData("ERROR")]
    [InlineData("DEBUG")]
    [InlineData("INFO")]
    public void CreateLogString_WithKnownLevel_EmitsBracketedLevelAndArrowSeparator(string level)
    {
        // Arrange & Act
        var line = InvokeCreateLogString(level, "msg");

        // Assert
        line.ShouldContain($"[{level}] -> msg");
    }

    [Fact]
    public void CreateLogString_WithMessage_PreservesMessageVerbatim()
    {
        // Arrange
        const string message = "Something happened with id=42 and path=C:\\foo\\bar.txt";

        // Act
        var line = InvokeCreateLogString("INFO", message);

        // Assert
        line.ShouldEndWith(message);
    }

    [Fact]
    public void CreateLogString_TimestampPrefix_FollowsInvariantSortableFormat()
    {
        // Arrange & Act
        var line = InvokeCreateLogString("DEBUG", "x");
        var prefix = line[..19];

        // Assert — must be parseable with the documented invariant format.
        var parsed = DateTime.TryParseExact(
            prefix, "yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture, DateTimeStyles.None, out _);
        parsed.ShouldBeTrue($"Timestamp prefix '{prefix}' is not in 'yyyy-MM-dd HH:mm:ss' format.");
    }

    [Fact]
    public void CreateLogString_WithEmptyMessage_StillEmitsLevelAndArrow()
    {
        // Arrange & Act
        var line = InvokeCreateLogString("ERROR", string.Empty);

        // Assert
        line.ShouldEndWith("[ERROR] -> ");
    }

    [Fact]
    public void CreateLogString_TimestampAndLevel_AreSeparatedBySingleSpace()
    {
        // Arrange & Act
        var line = InvokeCreateLogString("INFO", "hello");

        // Assert — index 19 is the space; index 20 must be '['.
        line[19].ShouldBe(' ');
        line[20].ShouldBe('[');
    }
}
