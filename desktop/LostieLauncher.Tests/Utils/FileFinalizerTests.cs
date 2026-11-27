using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class FileFinalizerTests : IDisposable
{
    private readonly TempDirectoryFixture _temp = new("file-finalizer");

    public void Dispose() => _temp.Dispose();

    private string CreateSource(string name = "payload.part")
    {
        var path = _temp.Combine(name);
        File.WriteAllText(path, "payload");
        return path;
    }


    [Theory]
    [InlineData(false, false, true)]
    [InlineData(true, false, false)]
    [InlineData(false, true, false)]
    public void IsRetryable_ForAnAccessDenial_OnlyRetriesWhenTheDestinationCanStillChange(bool isDirectory, bool isReadOnly, bool expected)
    {
        var error = new UnauthorizedAccessException("denied");

        var retryable = FileFinalizer.IsRetryable(error, new FileFinalizer.DestinationState(isDirectory, isReadOnly));

        retryable.ShouldBe(expected);
    }

    [Fact]
    public void IsRetryable_ForAnErrorThatIsNotAnAccessOrIoFailure_DoesNotRetry()
    {
        var error = new InvalidOperationException("bad state");

        FileFinalizer.IsRetryable(error, new FileFinalizer.DestinationState(IsDirectory: false, IsReadOnly: false)).ShouldBeFalse();
    }


    [Fact]
    public async Task MoveAsync_WhenTheDestinationIsHeldOpenAndThenReleased_CompletesOnARetry()
    {
        var source = CreateSource();
        var destination = _temp.Combine("payload.zip");
        File.WriteAllText(destination, "stale");
        var holder = new FileStream(destination, FileMode.Open, FileAccess.Read, FileShare.None);
        var delays = 0;

        await FileFinalizer.MoveAsync(source, destination, maxAttempts: 3, retryDelay: TimeSpan.Zero, delay: _ =>
        {
            delays++;
            holder.Dispose();
            return Task.CompletedTask;
        });

        delays.ShouldBe(1);
        File.Exists(source).ShouldBeFalse();
        File.ReadAllText(destination).ShouldBe("payload");
    }

    [Fact]
    public async Task MoveAsync_WhenTheDestinationIsADirectory_FailsWithoutRetrying()
    {
        var source = CreateSource();
        var destination = _temp.Combine("occupied");
        Directory.CreateDirectory(destination);
        var delays = 0;

        await Should.ThrowAsync<UnauthorizedAccessException>(() =>
            FileFinalizer.MoveAsync(source, destination, maxAttempts: 3, retryDelay: TimeSpan.Zero, delay: _ =>
            {
                delays++;
                return Task.CompletedTask;
            }));

        delays.ShouldBe(0);
    }

    [Fact]
    public async Task MoveAsync_WhenTheDestinationStaysLocked_GivesUpAfterTheConfiguredAttempts()
    {
        var source = CreateSource();
        var destination = _temp.Combine("payload.zip");
        File.WriteAllText(destination, "stale");
        using var holder = new FileStream(destination, FileMode.Open, FileAccess.Read, FileShare.None);
        var delays = 0;

        await Should.ThrowAsync<UnauthorizedAccessException>(() =>
            FileFinalizer.MoveAsync(source, destination, maxAttempts: 3, retryDelay: TimeSpan.Zero, delay: _ =>
            {
                delays++;
                return Task.CompletedTask;
            }));

        delays.ShouldBe(2);
        File.Exists(source).ShouldBeTrue();
    }

    [Fact]
    public async Task MoveAsync_WhenTheDestinationIsFree_MovesOnTheFirstAttempt()
    {
        var source = CreateSource();
        var destination = _temp.Combine("payload.zip");

        await FileFinalizer.MoveAsync(source, destination, maxAttempts: 3, retryDelay: TimeSpan.Zero,
            delay: _ => throw new InvalidOperationException("must not wait on the happy path"));

        File.ReadAllText(destination).ShouldBe("payload");
    }
}
