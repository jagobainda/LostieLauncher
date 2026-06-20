using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class PlaytimeFormatterTests
{
    [Theory]
    [InlineData(0, "")]
    [InlineData(-5, "")]
    [InlineData(int.MinValue, "")]
    public void Format_WhenMinutesIsZeroOrNegative_ReturnsEmpty(int minutes, string expected)
    {
        // Act
        var text = PlaytimeFormatter.Format(minutes);

        // Assert
        text.ShouldBe(expected);
    }

    [Theory]
    [InlineData(1, "1 min")]
    [InlineData(45, "45 min")]
    [InlineData(59, "59 min")]
    public void Format_WhenBelowOneHour_ReturnsMinutesOnly(int minutes, string expected)
    {
        // Act
        var text = PlaytimeFormatter.Format(minutes);

        // Assert
        text.ShouldBe(expected);
    }

    [Theory]
    [InlineData(60, "1 h")]
    [InlineData(120, "2 h")]
    public void Format_WhenWholeHours_OmitsMinutes(int minutes, string expected)
    {
        // Act
        var text = PlaytimeFormatter.Format(minutes);

        // Assert
        text.ShouldBe(expected);
    }

    [Theory]
    [InlineData(75, "1 h 15 min")]
    [InlineData(125, "2 h 5 min")]
    [InlineData(150, "2 h 30 min")]
    public void Format_WhenHoursAndMinutes_ReturnsBoth(int minutes, string expected)
    {
        // Act
        var text = PlaytimeFormatter.Format(minutes);

        // Assert
        text.ShouldBe(expected);
    }
}
