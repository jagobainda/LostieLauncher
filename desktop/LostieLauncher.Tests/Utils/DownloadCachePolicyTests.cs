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
        var stale = Select(Entry("demo-game.abc123.zip.part", ageInDays: 1), Entry("demo-game.abc123.zip.part.meta", ageInDays: 1));

        stale.ShouldBeEmpty();
    }

    [Fact]
    public void SelectStaleFiles_ForAPartNobodyResumedInWeeks_PurgesItAndItsMetadata()
    {
        var stale = Select(Entry("demo-game.abc123.zip.part", ageInDays: 30), Entry("demo-game.abc123.zip.part.meta", ageInDays: 30));

        stale.ShouldBe(["demo-game.abc123.zip.part", "demo-game.abc123.zip.part.meta"], ignoreOrder: true);
    }

    [Fact]
    public void SelectStaleFiles_ForAGameTheCatalogNoLongerLists_PurgesItRegardlessOfAge()
    {
        var stale = Select(Entry("removed-game.abc123.zip.part"));

        stale.ShouldBe(["removed-game.abc123.zip.part"]);
    }

    [Fact]
    public void SelectStaleFiles_ForResumeMetadataWithoutItsPartialFile_PurgesTheOrphan()
    {
        var stale = Select(Entry("demo-game.abc123.zip.part.meta"));

        stale.ShouldBe(["demo-game.abc123.zip.part.meta"]);
    }

    [Fact]
    public void SelectStaleFiles_ForAnUnknownVersionTokenOfAKnownGame_KeepsIt()
    {
        var stale = Select(Entry("demo-game.ffffffffffffffff.zip.part"));

        stale.ShouldBeEmpty();
    }

    [Fact]
    public void SelectStaleFiles_ForFilesItDoesNotManage_LeavesThemAlone()
    {
        var stale = Select(Entry("notes.txt", ageInDays: 400), Entry("demo-game.abc123.zip", ageInDays: 1));

        stale.ShouldBeEmpty();
    }

    [Fact]
    public void SelectStaleFiles_ForACompletedZipLeftBehind_PurgesItOnceItIsOldEnough()
    {
        var stale = Select(Entry("demo-game.abc123.zip", ageInDays: 30));

        stale.ShouldBe(["demo-game.abc123.zip"]);
    }
}
