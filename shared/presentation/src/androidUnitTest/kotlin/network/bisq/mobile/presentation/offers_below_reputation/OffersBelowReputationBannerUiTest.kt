package network.bisq.mobile.presentation.offers_below_reputation

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.domain.service.offers.OffendingOffer
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OffersBelowReputationBannerUiTest : PresentationKoinComposeTestBase() {
    private val headline get() = "bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.headline".i18n()

    private val visibleState =
        OffersBelowReputationUiState(
            offendingOffers = listOf(OffendingOffer(offerId = "a", market = MarketVOFactory.USD, formattedAmount = "50 USD")),
            isBannerVisible = true,
        )

    @Test
    fun `banner shows while there are undismissed offers`() {
        setTestContent { OffersBelowReputationBanner(uiState = visibleState, onAction = {}) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(headline).assertIsDisplayed()
    }

    @Test
    fun `banner is hidden once dismissed`() {
        setTestContent { OffersBelowReputationBanner(uiState = visibleState.copy(isBannerVisible = false), onAction = {}) }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText(headline).assertCountEquals(0)
    }

    @Test
    fun `tapping the banner opens the dialog`() {
        val actions = mutableListOf<OffersBelowReputationUiAction>()
        setTestContent { OffersBelowReputationBanner(uiState = visibleState, onAction = { actions += it }) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(headline).performClick()

        assertEquals(listOf<OffersBelowReputationUiAction>(OffersBelowReputationUiAction.OpenDialog), actions)
    }

    @Test
    fun `the close icon dismisses the banner`() {
        val actions = mutableListOf<OffersBelowReputationUiAction>()
        setTestContent { OffersBelowReputationBanner(uiState = visibleState, onAction = { actions += it }) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("mobile.alert.actions.dismiss.label".i18n()).performClick()

        assertEquals(listOf<OffersBelowReputationUiAction>(OffersBelowReputationUiAction.DismissBanner), actions)
    }
}
