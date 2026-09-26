package dev.jagoba.lostielauncher.ui.viewmodel

import dev.jagoba.lostielauncher.content.faqsFor
import dev.jagoba.lostielauncher.model.AppLanguage
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FaqsViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val settings = TestSettingsStore()
    private val links = TestExternalLinkService()

    @BeforeEach
    fun setUp() = Dispatchers.setMain(dispatcher)

    @AfterEach
    fun tearDown() = Dispatchers.resetMain()

    @Test
    fun `loads all FAQs collapsed in current language`() = runTest(dispatcher) {
        val sut = FaqsViewModel(settings, links)
        runCurrent()
        sut.state.value.rows.size shouldBe faqsFor(AppLanguage.ESP).size
        sut.state.value.rows.all { !it.isExpanded } shouldBe true
    }

    @Test
    fun `search matches answer ignoring accents and expands matches`() = runTest(dispatcher) {
        val sut = FaqsViewModel(settings, links)
        runCurrent()
        sut.setSearchText("instalacion")
        runCurrent()
        sut.state.value.rows.isNotEmpty() shouldBe true
        sut.state.value.rows.all { it.isExpanded } shouldBe true
    }

    @Test
    fun `no match and clear search reproduce empty and collapsed states`() = runTest(dispatcher) {
        val sut = FaqsViewModel(settings, links)
        runCurrent()
        sut.setSearchText("unmatched-term-xyz")
        runCurrent()
        sut.state.value.hasNoResults shouldBe true
        sut.clearSearch()
        runCurrent()
        sut.state.value.rows.size shouldBe faqsFor(AppLanguage.ESP).size
        sut.state.value.rows.all { !it.isExpanded } shouldBe true
    }

    @Test
    fun `language change reloads FAQs`() = runTest(dispatcher) {
        val sut = FaqsViewModel(settings, links)
        runCurrent()
        settings.current.value = settings.current.value.copy(language = AppLanguage.FRA)
        runCurrent()
        sut.state.value.rows.map { it.entry } shouldBe faqsFor(AppLanguage.FRA)
    }

    @Test
    fun `tapping FAQ toggles expansion and search resets it`() = runTest(dispatcher) {
        val sut = FaqsViewModel(settings, links)
        runCurrent()
        val index = sut.state.value.rows.first().index
        sut.toggleFaq(index)
        runCurrent()
        sut.state.value.rows.first().isExpanded shouldBe true
        sut.toggleFaq(index)
        runCurrent()
        sut.state.value.rows.first().isExpanded shouldBe false
        sut.toggleFaq(index)
        sut.setSearchText("a")
        runCurrent()
        sut.clearSearch()
        runCurrent()
        sut.state.value.rows.first().isExpanded shouldBe false
    }

    @Test
    fun `language change discards expansion overrides`() = runTest(dispatcher) {
        val sut = FaqsViewModel(settings, links)
        runCurrent()
        sut.toggleFaq(sut.state.value.rows.first().index)
        runCurrent()
        settings.current.value = settings.current.value.copy(language = AppLanguage.FRA)
        runCurrent()
        sut.state.value.rows.first().isExpanded shouldBe false
    }

    @Test
    fun `all supported languages have the same FAQ count`() {
        AppLanguage.entries.map { faqsFor(it).size }.distinct().size shouldBe 1
    }

    @Test
    fun `answer links open through the link service`() = runTest(dispatcher) {
        val sut = FaqsViewModel(settings, links)
        sut.openLink("https://example.com")
        links.openedUrls shouldBe listOf("https://example.com")
    }
}
