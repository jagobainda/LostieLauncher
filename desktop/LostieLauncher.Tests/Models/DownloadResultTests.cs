using LostieLauncher.Models;

namespace LostieLauncher.Tests.Models;

public class DownloadResultTests
{
    [Fact]
    public void Succeeded_BuildsResultWithSuccessOutcomeAndNoError()
    {
        var result = DownloadResult.Succeeded();

        result.Outcome.ShouldBe(DownloadOutcome.Success);
        result.ErrorMessage.ShouldBeNull();
    }

    [Fact]
    public void Cancelled_BuildsResultWithCancelledOutcomeAndNoError()
    {
        var result = DownloadResult.Cancelled();

        result.Outcome.ShouldBe(DownloadOutcome.Cancelled);
        result.ErrorMessage.ShouldBeNull();
    }

    [Fact]
    public void Failed_BuildsResultWithFailedOutcomeAndProvidedError()
    {
        const string message = "boom";

        var result = DownloadResult.Failed(message);

        result.Outcome.ShouldBe(DownloadOutcome.Failed);
        result.ErrorMessage.ShouldBe(message);
    }

    [Fact]
    public void PermissionDenied_BuildsResultWithPermissionDeniedOutcomeAndNoError()
    {
        var result = DownloadResult.PermissionDenied();

        result.Outcome.ShouldBe(DownloadOutcome.PermissionDenied);
        result.ErrorMessage.ShouldBeNull();
    }

    [Fact]
    public void DownloadProgressInfo_WhenConstructedWithoutOptionalFields_AppliesDocumentedDefaults()
    {
        var info = new DownloadProgressInfo(50, 1000);

        info.Percent.ShouldBe(50);
        info.BytesPerSecond.ShouldBe(1000);
        info.TotalBytes.ShouldBe(-1);
        info.DownloadedBytes.ShouldBe(0);
    }
}
