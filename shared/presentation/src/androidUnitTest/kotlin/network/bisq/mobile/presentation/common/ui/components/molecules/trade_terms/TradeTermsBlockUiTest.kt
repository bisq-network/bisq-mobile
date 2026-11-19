package network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * UI tests for [TradeTermsBlock] and [TradeTermsAcceptanceNotice]: the expander appears only when
 * collapsed text actually overflows, the full text is always in the semantics tree, and the
 * peer-profile mode never collapses.
 */
class TradeTermsBlockUiTest : BisqComposeUiTestBase() {
    private val title = "Makers trade terms"

    @Test
    fun `short collapsible terms show the text without an expander`() {
        I18nSupport.initialize("en")
        setTestContent {
            TradeTermsBlock(state = collapsible(SHORT_TERMS), title = title)
        }

        composeTestRule.onNodeWithText(title).assertIsDisplayed()
        composeTestRule.onNodeWithTag(TRADE_TERMS_TEXT_TAG).assertTextEquals(SHORT_TERMS)
        composeTestRule.onNodeWithTag(TRADE_TERMS_TOGGLE_TAG).assertDoesNotExist()
    }

    @Test
    fun `long collapsed terms offer show more and keep the full text for screen readers`() {
        I18nSupport.initialize("en")
        val onToggleExpand = mockk<() -> Unit>(relaxed = true)
        setTestContent {
            TradeTermsBlock(state = collapsible(LONG_TERMS), title = title, onToggleExpand = onToggleExpand)
        }

        composeTestRule.onNodeWithTag(TRADE_TERMS_TEXT_TAG).assertTextEquals(LONG_TERMS)
        val layout = textLayout()
        assertEquals(3, layout.lineCount)
        assertTrue(layout.hasVisualOverflow)
        composeTestRule.onNodeWithText("mobile.tradeTerms.showMore".i18n()).assertIsDisplayed()

        composeTestRule.onNodeWithTag(TRADE_TERMS_TOGGLE_TAG).performClick()

        verify(exactly = 1) { onToggleExpand() }
    }

    @Test
    fun `expanded terms offer show less`() {
        I18nSupport.initialize("en")
        setTestContent {
            TradeTermsBlock(state = collapsible(LONG_TERMS, isExpanded = true), title = title)
        }

        composeTestRule.onNodeWithTag(TRADE_TERMS_TEXT_TAG).assertTextEquals(LONG_TERMS)
        assertRendersInFull()
        composeTestRule.onNodeWithText("mobile.tradeTerms.showLess".i18n()).assertIsDisplayed()
    }

    @Test
    fun `full mode shows long terms without an expander`() {
        I18nSupport.initialize("en")
        setTestContent {
            TradeTermsBlock(
                state = TradeTermsUiState(text = LONG_TERMS, displayMode = TradeTermsDisplayMode.FULL),
                title = title,
            )
        }

        composeTestRule.onNodeWithTag(TRADE_TERMS_TEXT_TAG).assertTextEquals(LONG_TERMS)
        assertRendersInFull()
        composeTestRule.onNodeWithTag(TRADE_TERMS_TOGGLE_TAG).assertDoesNotExist()
    }

    @Test
    fun `the acceptance notice renders its copy`() {
        I18nSupport.initialize("en")
        setTestContent {
            TradeTermsAcceptanceNotice()
        }

        composeTestRule
            .onNodeWithTag(TRADE_TERMS_ACCEPTANCE_NOTICE_TAG)
            .assertTextEquals("mobile.tradeTerms.acceptanceNotice".i18n())
    }

    // The semantics text is always complete, so only the layout shows whether lines were cut.
    private fun textLayout(): TextLayoutResult {
        val results = mutableListOf<TextLayoutResult>()
        composeTestRule
            .onNodeWithTag(TRADE_TERMS_TEXT_TAG)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action
            ?.invoke(results)
        return results.single()
    }

    private fun assertRendersInFull() {
        val layout = textLayout()
        assertTrue(layout.lineCount >= LONG_TERMS_LINES)
        assertFalse(layout.hasVisualOverflow)
    }

    private fun collapsible(
        text: String,
        isExpanded: Boolean = false,
    ) = TradeTermsUiState(text = text, displayMode = TradeTermsDisplayMode.COLLAPSIBLE, isExpanded = isExpanded)

    private companion object {
        const val SHORT_TERMS = "I only accept payment from an account in your own name."

        // Explicit line breaks overflow three lines at any width or font, so the test does not
        // depend on how Robolectric measures glyphs.
        const val LONG_TERMS_LINES = 8
        val LONG_TERMS =
            (1..LONG_TERMS_LINES).joinToString("\n") { "Rule $it: own-name account only, no third-party payments." }
    }
}
