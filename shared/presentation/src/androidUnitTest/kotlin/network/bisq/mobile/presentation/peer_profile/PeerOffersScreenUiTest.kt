package network.bisq.mobile.presentation.peer_profile

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for the full "Trade again" list screen — the overflow surface behind the capped
 * section on the peer profile. Driven through [PeerOffersScreenContent] with crafted states:
 * unlike the profile's capped preview, EVERY offer renders here.
 */
class PeerOffersScreenUiTest : BisqComposeUiTestBase() {
    private lateinit var mockOnAction: (PeerProfileUiAction) -> Unit

    private val eurMarket = MarketVO("BTC", "EUR", "Bitcoin", "Euro")

    override fun setUpUiTest() {
        super.setUpUiTest()
        mockOnAction = mockk(relaxed = true)
    }

    private fun offer(
        id: String,
        date: Long,
        amount: String,
    ): OfferItemPresentationModel {
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "Alice"),
            )
        val bisqEasyOffer =
            BisqEasyOfferVO(
                id = id,
                date = date,
                makerNetworkId = makerNetworkId,
                direction = DirectionEnum.SELL,
                market = eurMarket,
                amountSpec = QuoteSideFixedAmountSpecVO(500_000L),
                priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_000_00L, eurMarket) }),
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = emptyList(),
            )
        return OfferItemPresentationModel(
            OfferItemPresentationDto(
                bisqEasyOffer = bisqEasyOffer,
                isMyOffer = false,
                userProfile = createMockUserProfile("Alice"),
                formattedDate = "",
                formattedQuoteAmount = amount,
                formattedBaseAmount = "",
                formattedPrice = "50,000 EUR",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = listOf("SEPA"),
                baseSidePaymentMethods = listOf("MAIN_CHAIN"),
                reputationScore = ReputationScoreVO(0, 0.0, 0),
            ),
        )
    }

    private fun state(offers: List<OfferItemPresentationModel>) =
        PeerProfileUiState(
            userProfile = createMockUserProfile("Alice"),
            displayName = "Alice",
            isLoading = false,
            hasTradedBefore = true,
            peerOffers = PeerOffersMarketGroupUiState.groupByMarket(offers),
        )

    private fun render(uiState: PeerProfileUiState) {
        setTestContent {
            PeerOffersScreenContent(
                uiState = uiState,
                onAction = mockOnAction,
            )
        }
    }

    @Test
    fun `every offer renders including those the profile preview cuts`() {
        render(
            state(
                listOf(
                    offer("o1", date = 4_000L, amount = "100.00 EUR"),
                    offer("o2", date = 3_000L, amount = "200.00 EUR"),
                    offer("o3", date = 2_000L, amount = "300.00 EUR"),
                    offer("o4", date = 1_000L, amount = "400.00 EUR"),
                ),
            ),
        )

        composeTestRule.onNodeWithText("100.00 EUR").assertIsDisplayed()
        composeTestRule.onNodeWithText("400.00 EUR").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.peerProfile.offers.viewAll".i18n(4)).assertDoesNotExist()
    }

    @Test
    fun `tapping a row dispatches the same offer click action as the profile section`() {
        render(state(listOf(offer("o1", date = 1_000L, amount = "100.00 EUR"))))

        composeTestRule.onNodeWithText("100.00 EUR").performClick()

        verify { mockOnAction(PeerProfileUiAction.OnPeerOfferClick("o1")) }
    }

    @Test
    fun `an emptied list falls back to the empty communication`() {
        render(state(emptyList()))

        composeTestRule.onNodeWithText("mobile.peerProfile.offers.empty".i18n()).assertIsDisplayed()
    }
}
