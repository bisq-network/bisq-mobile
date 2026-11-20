package network.bisq.mobile.presentation.offerbook

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.replicated.offer.price.spec.FloatPriceSpecVO
import network.bisq.mobile.data.replicated.offer.price.spec.PriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Before
import org.junit.Test

/**
 * UI tests for [OfferCard]'s price line and offer age: the price always names its type, every
 * card (peer or own) shows its age in the payment-methods row, and the age costs no height.
 */
class OfferCardUiTest : BisqComposeUiTestBase() {
    @Before
    fun setUpI18n() {
        I18nSupport.initialize("en")
    }

    @Test
    fun `a fixed price offer says Fixed on the price line`() {
        renderCard(offer())

        composeTestRule
            .onNodeWithTag(OFFER_CARD_PRICE_TAG, useUnmergedTree = true)
            .assertTextEquals("@ 100,000 (Fixed)")
    }

    @Test
    fun `a floating price offer shows its percentage vs market on the price line`() {
        renderCard(offer(priceSpec = FloatPriceSpecVO(0.02)))

        composeTestRule
            .onNodeWithTag(OFFER_CARD_PRICE_TAG, useUnmergedTree = true)
            .assertTextEquals("@ 100,000 (+2.00 % vs market)")
    }

    @Test
    fun `a peer offer shows its age with a spoken label`() {
        renderCard(offer(date = DateUtils.now() - 3 * DAY - HOUR))

        composeTestRule
            .onNodeWithTag(OFFER_CARD_AGE_TAG, useUnmergedTree = true)
            .assertTextEquals("3 days")
            .assertContentDescriptionEquals("Offer age: 3 days")
    }

    @Test
    fun `an own offer shows its age too`() {
        renderCard(offer(date = DateUtils.now(), isMyOffer = true))

        composeTestRule
            .onNodeWithTag(OFFER_CARD_AGE_TAG, useUnmergedTree = true)
            .assertTextEquals("Today")
    }

    @Test
    fun `a regular card keeps its height with the age present`() {
        renderCard(offer(date = DateUtils.now() - 812 * DAY))

        composeTestRule.onNodeWithTag(OFFER_CARD_AGE_TAG, useUnmergedTree = true).assertTextEquals("812 days")
        composeTestRule.onNodeWithTag(CARD_TAG).assertHeightIsEqualTo(150.dp)
    }

    @Test
    fun `the age never squeezes the remove icon of an own offer in a crowded row`() {
        // 3 fiat + 2 bitcoin methods take 155dp of the 185dp right column a 298dp card has: the
        // remove icon's 20dp still fits, the age and its gaps must make do with what is left.
        val crowded =
            OfferItemPresentationModel(
                OfferTestFactory.makeOfferDto(
                    paymentMethods = listOf("SEPA", "REVOLUT", "WISE"),
                    btcMethods = listOf("MAIN_CHAIN", "LN"),
                    date = DateUtils.now() - 812 * DAY,
                    isMyOffer = true,
                    formattedPrice = "100,000",
                ),
            )
        renderCard(crowded, modifier = Modifier.width(298.dp))

        composeTestRule
            .onNodeWithContentDescription("Remove offer icon", useUnmergedTree = true)
            .assertWidthIsEqualTo(20.dp)
    }

    @Test
    fun `a full payment row never pushes the badge or the remove icon off an own offer`() {
        // 4 fiat + 2 bitcoin methods (the most an offer can carry) take 180dp of the 185dp right
        // column a 298dp card has, leaving no room for the badge and remove icon unless the icons yield.
        val full =
            OfferItemPresentationModel(
                OfferTestFactory.makeOfferDto(
                    paymentMethods = listOf("SEPA", "REVOLUT", "WISE", "ZELLE"),
                    btcMethods = listOf("MAIN_CHAIN", "LN"),
                    isMyOffer = true,
                    formattedPrice = "100,000",
                ),
            )
        renderCard(full, modifier = Modifier.width(298.dp), isBelowOwnReputation = true)

        composeTestRule
            .onNodeWithContentDescription("Remove offer icon", useUnmergedTree = true)
            .assertWidthIsEqualTo(20.dp)
        composeTestRule
            .onNodeWithContentDescription(BELOW_REPUTATION_BADGE, useUnmergedTree = true)
            .assertWidthIsEqualTo(BisqUIConstants.ScreenPadding2X)
    }

    private fun renderCard(
        item: OfferItemPresentationModel,
        modifier: Modifier = Modifier,
        isBelowOwnReputation: Boolean = false,
    ) {
        setTestContent {
            Box(modifier = modifier.testTag(CARD_TAG)) {
                OfferCard(
                    item = item,
                    onSelectOffer = {},
                    userProfileIconProvider = { createEmptyImage() },
                    onPeerProfileClick = {},
                    isBelowOwnReputation = isBelowOwnReputation,
                )
            }
        }
    }

    private fun offer(
        priceSpec: PriceSpecVO = OfferTestFactory.makeOfferDto().bisqEasyOffer.priceSpec,
        date: Long = DateUtils.now(),
        isMyOffer: Boolean = false,
    ) = OfferItemPresentationModel(
        OfferTestFactory.makeOfferDto(priceSpec = priceSpec, date = date, isMyOffer = isMyOffer, formattedPrice = "100,000"),
    )

    private companion object {
        const val CARD_TAG = "offerCardUnderTest"
        const val BELOW_REPUTATION_BADGE = "Offer exceeds your reputation limit"
        const val HOUR = 3_600_000L
        const val DAY = 86_400_000L
    }
}
