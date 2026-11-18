package network.bisq.mobile.presentation.offer.take_offer.review

import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TRADE_TERMS_ACCEPTANCE_NOTICE_TAG
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TRADE_TERMS_BLOCK_TAG
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TRADE_TERMS_TEXT_TAG
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TRADE_TERMS_TOGGLE_TAG
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsDisplayMode
import network.bisq.mobile.presentation.common.ui.components.molecules.trade_terms.TradeTermsUiState
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for the maker's trade-terms section of the take-offer review step. Which terms the
 * offer carries is covered by [TakeOfferReviewPresenterTest]; these verify the section and its
 * acceptance notice appear together or not at all, and that the expander dispatches its action.
 */
class TakeOfferReviewTradeTermsUiTest : BisqComposeUiTestBase() {
    @Test
    fun `an offer with terms shows the maker's terms and the acceptance notice`() {
        I18nSupport.initialize("en")
        setTestContent {
            TakeOfferReviewTradeTerms(tradeTerms = collapsible(SHORT_TERMS), onAction = {})
        }

        composeTestRule.onNodeWithText("bisqEasy.offerDetails.makersTradeTerms".i18n()).assertExists()
        composeTestRule.onNodeWithTag(TRADE_TERMS_TEXT_TAG).assertTextEquals(SHORT_TERMS)
        composeTestRule.onNodeWithTag(TRADE_TERMS_ACCEPTANCE_NOTICE_TAG).assertExists()
    }

    @Test
    fun `an offer without terms shows neither the terms nor the acceptance notice`() {
        I18nSupport.initialize("en")
        setTestContent {
            TakeOfferReviewTradeTerms(tradeTerms = null, onAction = {})
        }

        composeTestRule.onNodeWithTag(TRADE_TERMS_BLOCK_TAG).assertDoesNotExist()
        composeTestRule.onNodeWithTag(TRADE_TERMS_ACCEPTANCE_NOTICE_TAG).assertDoesNotExist()
    }

    @Test
    fun `tapping show more dispatches the toggle action`() {
        I18nSupport.initialize("en")
        val onAction = mockk<(TakeOfferReviewUiAction) -> Unit>(relaxed = true)
        setTestContent {
            TakeOfferReviewTradeTerms(tradeTerms = collapsible(LONG_TERMS), onAction = onAction)
        }

        composeTestRule.onNodeWithTag(TRADE_TERMS_TOGGLE_TAG).performClick()

        verify(exactly = 1) { onAction(TakeOfferReviewUiAction.OnToggleTradeTerms) }
    }

    private fun collapsible(text: String) = TradeTermsUiState(text = text, displayMode = TradeTermsDisplayMode.COLLAPSIBLE)

    private companion object {
        const val SHORT_TERMS = "I only accept payment from an account in your own name."

        val LONG_TERMS = (1..8).joinToString("\n") { "Rule $it: own-name account only." }
    }
}
