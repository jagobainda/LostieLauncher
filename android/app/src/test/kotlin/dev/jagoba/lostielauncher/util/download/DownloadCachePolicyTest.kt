package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.DownloadCacheEntry
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

/** Ported declaration for declaration from the desktop's `Utils/DownloadCachePolicyTests.cs`. */
@DisplayName("DownloadCachePolicy.selectStaleFiles")
class DownloadCachePolicyTest {
    private val now: Instant = Instant.parse("2026-09-09T12:00:00Z")
    private val maxAge: Duration = Duration.ofDays(14)
    private val knownGames = setOf("demo-game")

    private fun entry(fileName: String, ageInDays: Long = 0) =
        DownloadCacheEntry(fileName, now.minus(Duration.ofDays(ageInDays)))

    private fun select(vararg entries: DownloadCacheEntry) =
        DownloadCachePolicy.selectStaleFiles(entries.asList(), knownGames, now, maxAge)

    @Test
    fun `keeps a recent partial file of a known game so the download can resume`() {
        // Arrange & Act — this is the whole reason partial files are not deleted on every error.
        val stale = select(
            entry("demo-game.abc123.zip.part", ageInDays = 1),
            entry("demo-game.abc123.zip.part.meta", ageInDays = 1),
        )

        // Assert
        stale.shouldBeEmpty()
    }

    @Test
    fun `purges a partial file nobody resumed in weeks, and its metadata`() {
        // Arrange & Act — the gigabytes of invisible orphans a failing session used to leave.
        val stale = select(
            entry("demo-game.abc123.zip.part", ageInDays = 30),
            entry("demo-game.abc123.zip.part.meta", ageInDays = 30),
        )

        // Assert
        stale shouldContainExactlyInAnyOrder listOf(
            "demo-game.abc123.zip.part",
            "demo-game.abc123.zip.part.meta",
        )
    }

    @Test
    fun `purges a game the catalogue no longer lists, regardless of age`() {
        // Arrange & Act — no download the launcher can start would ever resume this file.
        val stale = select(entry("removed-game.abc123.zip.part"))

        // Assert
        stale shouldContainExactlyInAnyOrder listOf("removed-game.abc123.zip.part")
    }

    @Test
    fun `purges resume metadata whose partial file is gone`() {
        // Arrange & Act — metadata describing a partial file that no longer exists is dead weight.
        val stale = select(entry("demo-game.abc123.zip.part.meta"))

        // Assert
        stale shouldContainExactlyInAnyOrder listOf("demo-game.abc123.zip.part.meta")
    }

    @Test
    fun `keeps an unknown version token of a known game`() {
        // Arrange & Act — a special-version token hashes the download key, which the catalogue
        // does not carry, so requiring a token match would break exactly those resumes.
        val stale = select(entry("demo-game.ffffffffffffffff.zip.part"))

        // Assert
        stale.shouldBeEmpty()
    }

    @Test
    fun `leaves alone the files it does not manage`() {
        // Arrange & Act — the folder is the launcher's, but a file it did not write is not its
        // business, however old it is.
        val stale = select(entry("notes.txt", ageInDays = 400), entry("demo-game.abc123.zip", ageInDays = 1))

        // Assert
        stale.shouldBeEmpty()
    }

    @Test
    fun `purges a completed archive left behind once it is old enough`() {
        // Arrange & Act — a verified archive is deleted after install; one that outlived that
        // is waste.
        val stale = select(entry("demo-game.abc123.zip", ageInDays = 30))

        // Assert
        stale shouldContainExactlyInAnyOrder listOf("demo-game.abc123.zip")
    }

    // ---- Android-only: rules the desktop states but does not pin ----

    @Test
    fun `compares names and game ids without regard to case`() {
        // Arrange — the desktop uses ordinal ignore-case throughout; nothing there asserts it.
        // Act
        val stale = DownloadCachePolicy.selectStaleFiles(
            listOf(entry("DEMO-GAME.ABC123.ZIP.PART"), entry("DEMO-GAME.ABC123.ZIP.PART.META")),
            setOf("Demo-Game"),
            now,
            maxAge,
        )

        // Assert — recognised as managed, matched to the known game, and its sidecar paired up.
        stale.shouldBeEmpty()
    }

    @Test
    fun `treats a name with no game id as stale`() {
        // Arrange — the id is everything before the first dot, so a leading dot leaves nothing,
        // and nothing is never a known game.
        // Act
        val stale = select(entry(".abc123.zip"))

        // Assert
        stale shouldContainExactlyInAnyOrder listOf(".abc123.zip")
    }
}
