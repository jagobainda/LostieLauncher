package dev.jagoba.lostielauncher.util.download

import dev.jagoba.lostielauncher.model.DownloadCacheEntry
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import java.time.Duration
import java.time.Instant
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test

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
        val stale = select(
            entry("demo-game.abc123.zip.part", ageInDays = 1),
            entry("demo-game.abc123.zip.part.meta", ageInDays = 1),
        )

        stale.shouldBeEmpty()
    }

    @Test
    fun `purges a partial file nobody resumed in weeks, and its metadata`() {
        val stale = select(
            entry("demo-game.abc123.zip.part", ageInDays = 30),
            entry("demo-game.abc123.zip.part.meta", ageInDays = 30),
        )

        stale shouldContainExactlyInAnyOrder listOf(
            "demo-game.abc123.zip.part",
            "demo-game.abc123.zip.part.meta",
        )
    }

    @Test
    fun `purges a game the catalogue no longer lists, regardless of age`() {
        val stale = select(entry("removed-game.abc123.zip.part"))

        stale shouldContainExactlyInAnyOrder listOf("removed-game.abc123.zip.part")
    }

    @Test
    fun `purges resume metadata whose partial file is gone`() {
        val stale = select(entry("demo-game.abc123.zip.part.meta"))

        stale shouldContainExactlyInAnyOrder listOf("demo-game.abc123.zip.part.meta")
    }

    @Test
    fun `keeps an unknown version token of a known game`() {
        val stale = select(entry("demo-game.ffffffffffffffff.zip.part"))

        stale.shouldBeEmpty()
    }

    @Test
    fun `leaves alone the files it does not manage`() {
        val stale = select(entry("notes.txt", ageInDays = 400), entry("demo-game.abc123.zip", ageInDays = 1))

        stale.shouldBeEmpty()
    }

    @Test
    fun `purges a completed archive left behind once it is old enough`() {
        val stale = select(entry("demo-game.abc123.zip", ageInDays = 30))

        stale shouldContainExactlyInAnyOrder listOf("demo-game.abc123.zip")
    }

    @Test
    fun `compares names and game ids without regard to case`() {
        val stale = DownloadCachePolicy.selectStaleFiles(
            listOf(entry("DEMO-GAME.ABC123.ZIP.PART"), entry("DEMO-GAME.ABC123.ZIP.PART.META")),
            setOf("Demo-Game"),
            now,
            maxAge,
        )

        stale.shouldBeEmpty()
    }

    @Test
    fun `treats a name with no game id as stale`() {
        val stale = select(entry(".abc123.zip"))

        stale shouldContainExactlyInAnyOrder listOf(".abc123.zip")
    }
}
