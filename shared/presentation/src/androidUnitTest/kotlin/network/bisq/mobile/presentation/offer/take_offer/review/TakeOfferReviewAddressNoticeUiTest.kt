package network.bisq.mobile.presentation.offer.take_offer.review

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for the three payout-address notice variants on the take-offer review step. Variant
 * SELECTION is covered by [TakeOfferReviewPresenterTest]; these verify each variant renders its
 * own copy and wires its action.
 */
class TakeOfferReviewAddressNoticeUiTest : BisqComposeUiTestBase() {
    init {
        I18nSupport.initialize("en")
    }

    @Test
    fun `announce variant shows the ask and routes the wallet guide action`() {
        val onOpenWalletGuide = mockk<() -> Unit>(relaxed = true)

        setTestContent {
            TakeOfferReviewAddressNotice(
                notice = TakeOfferReviewPresenter.AddressNotice.MainchainAnnounce,
                onEditAddress = {},
                onOpenWalletGuide = onOpenWalletGuide,
            )
        }

        composeTestRule
            .onNodeWithText("mobile.takeOffer.review.addressNotice.mainchain.announce.headline".i18n())
            .assertExists()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton".i18n())
            .performClick()
        verify(exactly = 1) { onOpenWalletGuide() }
    }

    @Test
    fun `confirmed variant shows the truncated address and routes edit`() {
        val onEditAddress = mockk<() -> Unit>(relaxed = true)

        setTestContent {
            TakeOfferReviewAddressNotice(
                notice = TakeOfferReviewPresenter.AddressNotice.MainchainConfirmed("bc1qw508d6…f3t4"),
                onEditAddress = onEditAddress,
                onOpenWalletGuide = {},
            )
        }

        composeTestRule
            .onNodeWithText("mobile.takeOffer.review.addressNotice.mainchain.confirmed.headline".i18n())
            .assertExists()
        composeTestRule.onNodeWithText("bc1qw508d6…f3t4").assertExists()
        composeTestRule
            .onNodeWithText("mobile.takeOffer.review.addressNotice.mainchain.confirmed.editAction".i18n())
            .performClick()
        verify(exactly = 1) { onEditAddress() }
    }

    @Test
    fun `lightning variant announces the invoice timing without any action`() {
        setTestContent {
            TakeOfferReviewAddressNotice(
                notice = TakeOfferReviewPresenter.AddressNotice.Lightning,
                onEditAddress = {},
                onOpenWalletGuide = {},
            )
        }

        composeTestRule
            .onNodeWithText("mobile.takeOffer.review.addressNotice.lightning.headline".i18n())
            .assertExists()
        composeTestRule
            .onNodeWithText("bisqEasy.tradeState.info.buyer.phase1a.walletHelpButton".i18n())
            .assertDoesNotExist()
    }
}
