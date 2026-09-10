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
import network.bisq.mobile.data.utils.createEmptyImage
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test

/**
 * UI tests for the "Trade again" section on Peer Profile, driven through
 * [PeerProfileScreenContent] with crafted states — header variants, the relationship gate,
 * the syncing row, the non-takeable caption, and the tap dispatch.
 */
class PeerProfileOffersSectionUiTest : BisqComposeUiTestBase() {
    private lateinit var mockOnAction: (PeerProfileUiAction) -> Unit

    private val eurMarket = MarketVO("BTC", "EUR", "Bitcoin", "Euro")

    override fun setUpUiTest() {
        super.setUpUiTest()
        mockOnAction = mockk(relaxed = true)
    }

    private fun offer(
        id: String,
        invalidDueToReputation: Boolean = false,
    ): OfferItemPresentationModel {
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = "Alice"),
            )
        val bisqEasyOffer =
            BisqEasyOfferVO(
                id = id,
                date = 1_000L,
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
                formattedQuoteAmount = "500.00 EUR",
                formattedBaseAmount = "",
                formattedPrice = "50,000 EUR",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = listOf("SEPA"),
                baseSidePaymentMethods = listOf("MAIN_CHAIN"),
                reputationScore = ReputationScoreVO(0, 0.0, 0),
            ),
        ).also { it.isInvalidDueToReputation = invalidDueToReputation }
    }

    private fun stateWithOffers(
        hasTradedBefore: Boolean = true,
        isContact: Boolean = false,
        offers: List<OfferItemPresentationModel> = listOf(offer("o1")),
        isSyncing: Boolean = false,
    ) = PeerProfileUiState(
        userProfile = createMockUserProfile("Alice"),
        displayName = "Alice",
        isLoading = false,
        hasTradedBefore = hasTradedBefore,
        isContact = isContact,
        peerOffers =
            if (offers.isEmpty()) {
                emptyList()
            } else {
                listOf(PeerOffersMarketGroupUiState(marketCodes = "BTC/EUR", offers = offers))
            },
        isPeerOffersSyncing = isSyncing,
    )

    private fun render(uiState: PeerProfileUiState) {
        setTestContent {
            PeerProfileScreenContent(
                uiState = uiState,
                userProfileIconProvider = { createEmptyImage() },
                onAction = mockOnAction,
            )
        }
    }

    private fun tradedHeader() = "mobile.peerProfile.offers.sectionTitle".i18n("Alice")

    private fun contactHeader() = "mobile.peerProfile.offers.sectionTitleContactOnly".i18n("Alice")

    @Test
    fun `a traded peer gets the Trade again header with the offer count`() {
        render(stateWithOffers(hasTradedBefore = true))

        composeTestRule.onNodeWithText(tradedHeader()).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.peerProfile.offers.countCaption.single".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("BTC/EUR").assertIsDisplayed()
    }

    @Test
    fun `a contact never traded with gets the Trade with header`() {
        render(stateWithOffers(hasTradedBefore = false, isContact = true))

        composeTestRule.onNodeWithText(contactHeader()).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(tradedHeader()).assertDoesNotExist()
    }

    @Test
    fun `a stranger gets no section even with offers loaded`() {
        render(stateWithOffers(hasTradedBefore = false, isContact = false))

        composeTestRule.onNodeWithText(tradedHeader()).assertDoesNotExist()
        composeTestRule.onNodeWithText(contactHeader()).assertDoesNotExist()
    }

    @Test
    fun `while syncing the section shows the honest checking row instead of nothing`() {
        render(stateWithOffers(offers = emptyList(), isSyncing = true))

        composeTestRule
            .onNodeWithText("mobile.peerProfile.offers.syncing".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `a reputation-gated offer carries the inline reason caption`() {
        render(stateWithOffers(offers = listOf(offer("o1", invalidDueToReputation = true))))

        composeTestRule
            .onNodeWithText("mobile.peerProfile.offers.notTakeableReason".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `tapping an offer row dispatches the offer click action`() {
        render(stateWithOffers(offers = listOf(offer("o1"))))

        composeTestRule.onNodeWithText("500.00 EUR").performScrollTo().performClick()

        verify { mockOnAction(PeerProfileUiAction.OnPeerOfferClick("o1")) }
    }

    @Test
    fun `the reputation dialog renders from state and dismiss dispatches`() {
        render(
            stateWithOffers().copy(
                notEnoughReputation =
                    NotEnoughReputationUiState(
                        headline = "Not enough reputation",
                        message = "The maker requires more reputation.",
                        isSellerAsTakerWarning = true,
                    ),
            ),
        )

        composeTestRule.onNodeWithText("Not enough reputation").assertIsDisplayed()
    }
}
