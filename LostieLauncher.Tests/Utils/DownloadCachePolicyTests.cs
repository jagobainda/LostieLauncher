using LostieLauncher.Models;
using LostieLauncher.Utils;

namespace LostieLauncher.Tests.Utils;

public class DownloadCachePolicyTests
{
    private static readonly DateTime Now = new(2026, 9, 9, 12, 0, 0, DateTimeKind.Utc);
    private static readonly TimeSpan MaxAge = TimeSpan.FromDays(14);

    private static readonly IReadOnlySet<string> KnownGames = new HashSet<string>(StringComparer.OrdinalIgnoreCase) { "demo-game" };

    private static DownloadCacheEntry Entry(string fileName, int ageInDays = 0) =>
        new(fileName, Now.AddDays(-ageInDays));

    private static IReadOnlyList<string> Select(params DownloadCacheEntry[] entries) =>
        DownloadCachePolicy.SelectStaleFiles(entries, KnownGames, Now, MaxAge);

    [Fact]
    public void SelectStaleFiles_ForARecentPartOfAKnownGame_KeepsItSoTheDownloadCanResume()
    {
        // Arrange & Act — this is the whole reason .part files are not deleted on every error.
        var stale = Select(Entry("demo-game.abc123.zip.part", ageInDays: 1), Entry("demo-game.abc123.zip.part.meta", ageInDays: 1));

        // Assert
        stale.ShouldBeEmpty();
    }

    [Fact]
    public void SelectStaleFiles_ForAPartNobodyResumedInWeeks_PurgesItAndItsMetadata()
    {
        // Arrange & Act — the 4+ GiB of invisible orphans a failing session used to leave behind.
        var stale = Select(Entry("demo-game.abc123.zip.part", ageInDays: 30), Entry("demo-game.abc123.zip.part.meta", ageInDays: 30));

        // Assert
        stale.ShouldBe(["demo-game.abc123.zip.part", "demo-game.abc123.zip.part.meta"], ignoreOrder: true);
    }

    [Fact]
    public void SelectStaleFiles_ForAGameTheCatalogNoLongerLists_PurgesItRegardlessOfAge()
    {
        // Arrange & Act — no download the launcher can start would ever resume this file.
        var stale = Select(Entry("removed-game.abc123.zip.part"));

        // Assert
        stale.ShouldBe(["removed-game.abc123.zip.part"]);
    }

    [Fact]
    public void SelectStaleFiles_ForResumeMetadataWithoutItsPartialFile_PurgesTheOrphan()
    {
        // Arrange & Act — metadata describing a partial file that is gone is dead weight.
        var stale = Select(Entry("demo-game.abc123.zip.part.meta"));

        // Assert
        stale.ShouldBe(["demo-game.abc123.zip.part.meta"]);
    }

    [Fact]
    public void SelectStaleFiles_ForAnUnknownVersionTokenOfAKnownGame_KeepsIt()
    {
        // Arrange & Act — special-version tokens hash the download key, which the catalog does not
        // carry, so a token match can never be required without breaking those resumes.
        var stale = Select(Entry("demo-game.ffffffffffffffff.zip.part"));

        // Assert
        stale.ShouldBeEmpty();
    }

    [Fact]
    public void SelectStaleFiles_ForFilesItDoesNotManage_LeavesThemAlone()
    {
        // Arrange & Act — the folder is the launcher's, but an unrelated file is not its business.
        var stale = Select(Entry("notes.txt", ageInDays: 400), Entry("demo-game.abc123.zip", ageInDays: 1));

        // Assert
        stale.ShouldBeEmpty();
    }

    [Fact]
    public void SelectStaleFiles_ForACompletedZipLeftBehind_PurgesItOnceItIsOldEnough()
    {
        // Arrange & Act — a verified archive is deleted after install; one that outlived that is waste.
        var stale = Select(Entry("demo-game.abc123.zip", ageInDays: 30));

        // Assert
        stale.ShouldBe(["demo-game.abc123.zip"]);
    }
}
