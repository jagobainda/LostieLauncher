using LostieLauncher.Utils;
using System.Globalization;
using System.Reflection;

namespace LostieLauncher.Tests.Utils;

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
        var line = InvokeCreateLogString(level, "msg");

        line.ShouldContain($"[{level}] -> msg");
    }

    [Fact]
    public void CreateLogString_WithMessage_PreservesMessageVerbatim()
    {
        const string message = "Something happened with id=42 and path=C:\\foo\\bar.txt";

        var line = InvokeCreateLogString("INFO", message);

        line.ShouldEndWith(message);
    }

    [Fact]
    public void CreateLogString_TimestampPrefix_FollowsInvariantSortableFormat()
    {
        var line = InvokeCreateLogString("DEBUG", "x");
        var prefix = line[..19];

        var parsed = DateTime.TryParseExact(
            prefix, "yyyy-MM-dd HH:mm:ss", CultureInfo.InvariantCulture, DateTimeStyles.None, out _);
        parsed.ShouldBeTrue($"Timestamp prefix '{prefix}' is not in 'yyyy-MM-dd HH:mm:ss' format.");
    }

    [Fact]
    public void CreateLogString_WithEmptyMessage_StillEmitsLevelAndArrow()
    {
        var line = InvokeCreateLogString("ERROR", string.Empty);

        line.ShouldEndWith("[ERROR] -> ");
    }

    [Fact]
    public void CreateLogString_TimestampAndLevel_AreSeparatedBySingleSpace()
    {
        var line = InvokeCreateLogString("INFO", "hello");

        line[19].ShouldBe(' ');
        line[20].ShouldBe('[');
    }

    [Theory]
    [InlineData(typeof(HttpRequestException), true)]
    [InlineData(typeof(TaskCanceledException), true)]
    [InlineData(typeof(OperationCanceledException), true)]
    [InlineData(typeof(System.Text.Json.JsonException), false)]
    [InlineData(typeof(InvalidOperationException), false)]
    public void IsNetworkFailure_ClassifiesOnlyTransportFailuresAndTimeoutsAsNetworkFailures(Type exceptionType, bool expected)
    {
        var exception = (Exception)Activator.CreateInstance(exceptionType)!;

        var isNetworkFailure = Logs.IsNetworkFailure(exception);

        isNetworkFailure.ShouldBe(expected);
    }
}
