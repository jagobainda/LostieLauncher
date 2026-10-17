using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class FileFinalizerTests : IDisposable
{
    // -------------------- IsRetryable --------------------

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
        // Arrange — a directory or a read-only file blocks every future attempt just as it blocks
        // this one, so the user should not wait through a backoff that cannot help.
        var error = new UnauthorizedAccessException("denied");

        // Act
        var retryable = FileFinalizer.IsRetryable(error, new FileFinalizer.DestinationState(isDirectory, isReadOnly));

        // Assert
        retryable.ShouldBe(expected);
    }

    [Fact]
    public void IsRetryable_ForAnErrorThatIsNotAnAccessOrIoFailure_DoesNotRetry()
    {
        // Arrange — retrying only makes sense for contention on the file itself.
        var error = new InvalidOperationException("bad state");

        // Act & Assert
        FileFinalizer.IsRetryable(error, new FileFinalizer.DestinationState(IsDirectory: false, IsReadOnly: false)).ShouldBeFalse();
    }


    // -------------------- MoveAsync --------------------

    [Fact]
    public async Task MoveAsync_WhenTheDestinationIsHeldOpenAndThenReleased_CompletesOnARetry()
    {
        // Arrange — the realistic transient case: an antivirus or sync client has the destination
        // open when the transfer finishes. The lock is released from the retry delay so the
        // hand-off is deterministic instead of depending on wall-clock timing.
        var source = CreateSource();
        var destination = _temp.Combine("payload.zip");
        File.WriteAllText(destination, "stale");
        var holder = new FileStream(destination, FileMode.Open, FileAccess.Read, FileShare.None);
        var delays = 0;

        // Act
        await FileFinalizer.MoveAsync(source, destination, maxAttempts: 3, retryDelay: TimeSpan.Zero, delay: _ =>
        {
            delays++;
            holder.Dispose();
            return Task.CompletedTask;
        });

        // Assert — the completed transfer survives instead of being discarded on the first failure.
        delays.ShouldBe(1);
        File.Exists(source).ShouldBeFalse();
        File.ReadAllText(destination).ShouldBe("payload");
    }

    [Fact]
    public async Task MoveAsync_WhenTheDestinationIsADirectory_FailsWithoutRetrying()
    {
        // Arrange — a permanently blocked destination: no number of attempts changes it.
        var source = CreateSource();
        var destination = _temp.Combine("occupied");
        Directory.CreateDirectory(destination);
        var delays = 0;

        // Act
        await Should.ThrowAsync<UnauthorizedAccessException>(() =>
            FileFinalizer.MoveAsync(source, destination, maxAttempts: 3, retryDelay: TimeSpan.Zero, delay: _ =>
            {
                delays++;
                return Task.CompletedTask;
            }));

        // Assert
        delays.ShouldBe(0);
    }

    [Fact]
    public async Task MoveAsync_WhenTheDestinationStaysLocked_GivesUpAfterTheConfiguredAttempts()
    {
        // Arrange — contention that never clears must still end, and end as the original error.
        var source = CreateSource();
        var destination = _temp.Combine("payload.zip");
        File.WriteAllText(destination, "stale");
        using var holder = new FileStream(destination, FileMode.Open, FileAccess.Read, FileShare.None);
        var delays = 0;

        // Act — a destination held open by another process surfaces as UnauthorizedAccessException,
        // which is precisely the shape that used to slip past the download retry filter.
        await Should.ThrowAsync<UnauthorizedAccessException>(() =>
            FileFinalizer.MoveAsync(source, destination, maxAttempts: 3, retryDelay: TimeSpan.Zero, delay: _ =>
            {
                delays++;
                return Task.CompletedTask;
            }));

        // Assert — three attempts means two waits between them.
        delays.ShouldBe(2);
        File.Exists(source).ShouldBeTrue();
    }

    [Fact]
    public async Task MoveAsync_WhenTheDestinationIsFree_MovesOnTheFirstAttempt()
    {
        // Arrange
        var source = CreateSource();
        var destination = _temp.Combine("payload.zip");

        // Act
        await FileFinalizer.MoveAsync(source, destination, maxAttempts: 3, retryDelay: TimeSpan.Zero,
            delay: _ => throw new InvalidOperationException("must not wait on the happy path"));

        // Assert
        File.ReadAllText(destination).ShouldBe("payload");
    }
}
