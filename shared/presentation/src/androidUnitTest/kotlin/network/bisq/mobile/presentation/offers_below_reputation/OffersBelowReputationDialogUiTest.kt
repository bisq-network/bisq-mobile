package network.bisq.mobile.presentation.offers_below_reputation

import androidx.compose.ui.test.assertAll
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.isNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.service.offers.OffendingOffer
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import kotlin.test.assertEquals

@OptIn(ExperimentalCoroutinesApi::class)
class OffersBelowReputationDialogUiTest : PresentationKoinComposeTestBase() {
    private val buildReputation get() = "bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.buildReputation".i18n()

    private fun state(isRemoving: Boolean = false) =
        OffersBelowReputationUiState(
            offendingOffers =
                listOf(
                    OffendingOffer(offerId = "a", market = MarketVO("BTC", "EUR", "Bitcoin", "Euro"), formattedAmount = "500 EUR"),
                    OffendingOffer(offerId = "b", market = MarketVO("BTC", "GBP", "Bitcoin", "Pound"), formattedAmount = "45 GBP"),
                ),
            isDialogVisible = true,
            isRemoving = isRemoving,
        )

    @Test
    fun `dialog lists every offending offer`() {
        setTestContent { OffersBelowReputationDialog(uiState = state(), onAction = {}) }
        composeTestRule.waitForIdle()

        composeTestRule
            .onNodeWithText("bisqEasy.offerbook.offerList.popup.offersWithInsufficientReputationWarning.headline".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("BTC/EUR").assertIsDisplayed()
        composeTestRule.onNodeWithText("500 EUR").assertIsDisplayed()
        composeTestRule.onNodeWithText("BTC/GBP").assertIsDisplayed()
        composeTestRule.onNodeWithText("45 GBP").assertIsDisplayed()
    }

    @Test
    fun `only a row whose removal failed is marked with an error icon`() {
        val state = state()
        setTestContent {
            OffersBelowReputationDialog(
                uiState = state.copy(offendingOffers = state.offendingOffers.mapIndexed { i, offer -> offer.copy(hasRemoveError = i == 0) }),
                onAction = {},
            )
        }
        composeTestRule.waitForIdle()

        // A single-node finder: it fails on no icon and on one per row alike.
        composeTestRule
            .onNodeWithContentDescription("mobile.bisqEasy.offerbook.offersBelowReputation.dialog.removeFailed".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `dialog buttons dispatch remove, keep and build reputation`() {
        val actions = mutableListOf<OffersBelowReputationUiAction>()
        setTestContent { OffersBelowReputationDialog(uiState = state(), onAction = { actions += it }) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithContentDescription("dialog_confirm_yes").performClick()
        composeTestRule.onNodeWithContentDescription("dialog_confirm_no").performClick()
        composeTestRule.onNodeWithText(buildReputation).performClick()
        composeTestRule.waitForIdle()

        assertEquals(
            listOf(
                OffersBelowReputationUiAction.RemoveOffers,
                OffersBelowReputationUiAction.Keep,
                OffersBelowReputationUiAction.BuildReputation,
            ),
            actions,
        )
    }

    @Test
    fun `tapping a row goes to its market`() {
        val actions = mutableListOf<OffersBelowReputationUiAction>()
        val state = state()
        setTestContent { OffersBelowReputationDialog(uiState = state, onAction = { actions += it }) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("BTC/GBP").performClick()
        composeTestRule.waitForIdle()

        assertEquals(listOf<OffersBelowReputationUiAction>(OffersBelowReputationUiAction.GoToMarket(state.offendingOffers[1])), actions)
    }

    @Test
    fun `every row offers to go to its market`() {
        setTestContent { OffersBelowReputationDialog(uiState = state(), onAction = {}) }
        composeTestRule.waitForIdle()

        composeTestRule.onAllNodesWithText("mobile.bisqEasy.offerbook.offersBelowReputation.dialog.goToMarket".i18n()).assertCountEquals(2)
    }

    @Test
    fun `while removing the build reputation link is disabled`() {
        setTestContent { OffersBelowReputationDialog(uiState = state(isRemoving = true), onAction = {}) }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(buildReputation).assertIsNotEnabled()
    }

    @Test
    fun `while removing the market rows are disabled`() {
        setTestContent { OffersBelowReputationDialog(uiState = state(isRemoving = true), onAction = {}) }
        composeTestRule.waitForIdle()

        composeTestRule
            .onAllNodesWithText("mobile.bisqEasy.offerbook.offersBelowReputation.dialog.goToMarket".i18n())
            .assertCountEquals(2)
            .assertAll(isNotEnabled())
    }
}
