package dev.jagoba.lostielauncher.core.di

import dev.jagoba.lostielauncher.model.ExternalLink
import dev.jagoba.lostielauncher.util.net.HttpsUrls
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class ExternalLinkOptionsModuleTest {
    @Test
    fun `every shell destination is a validated HTTPS URL`() {
        val urls = ExternalLinkOptionsModule.provideExternalLinkOptions().urls
        urls.keys shouldBe ExternalLink.entries.toSet()
        urls.values.all { HttpsUrls.parseOrNull(it) != null } shouldBe true
    }
}
