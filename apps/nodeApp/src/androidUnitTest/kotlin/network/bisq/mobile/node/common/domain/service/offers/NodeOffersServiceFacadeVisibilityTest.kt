package network.bisq.mobile.node.common.domain.service.offers

import bisq.bisq_easy.BisqEasyOfferbookMessageService
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookChannel
import bisq.chat.bisq_easy.offerbook.BisqEasyOfferbookMessage
import bisq.common.market.Market
import bisq.common.observable.Observable
import bisq.common.observable.collection.ObservableSet
import bisq.common.observable.map.ObservableHashMap
import bisq.offer.bisq_easy.BisqEasyOffer
import bisq.user.identity.UserIdentityService
import bisq.user.profile.UserProfileService
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory
import network.bisq.mobile.data.replicated.common.monetary.PriceQuoteVOFactory.fromPrice
import network.bisq.mobile.data.replicated.common.network.AddressByTransportTypeMapVO
import network.bisq.mobile.data.replicated.network.identity.NetworkIdVO
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.data.replicated.offer.price.spec.FixPriceSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.data.replicated.security.keys.PubKeyVO
import network.bisq.mobile.data.replicated.security.keys.PublicKeyVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.node.common.domain.mapping.OfferItemPresentationVOFactory
import network.bisq.mobile.node.common.domain.service.AndroidApplicationService
import network.bisq.mobile.node.common.test_utils.NodeKoinIntegrationTestBase
import org.junit.Test
import java.util.Optional
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * The node shows offers like bisq2's offerbook API (`OfferVisibility.find`, bisq2#5095): valid offers
 * are shown, invalid offers of other makers are hidden, and my own invalid offers are shown with the
 * seller reputation verdict so the app can warn me. The item factory is mocked: these tests are about
 * which messages become items and with which verdict, not about formatting.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NodeOffersServiceFacadeVisibilityTest : NodeKoinIntegrationTestBase() {
    private companion object {
        const val MAKER = "maker-a"
    }

    private val messageService: BisqEasyOfferbookMessageService = mockk()
    private val userProfileService: UserProfileService = mockk()
    private val userIdentityService: UserIdentityService = mockk()
    private val chatMessages = ObservableSet<BisqEasyOfferbookMessage>()
    private lateinit var facade: NodeOffersServiceFacade

    override fun onSetup() {
        val channel =
            mockk<BisqEasyOfferbookChannel> {
                every { chatMessages } returns this@NodeOffersServiceFacadeVisibilityTest.chatMessages
                every { market } returns Market("BTC", "USD", "Bitcoin", "US Dollar")
                every { id } returns "channel"
            }
        val applicationService = mockk<AndroidApplicationService>(relaxed = true)
        every { applicationService.userService.userProfileService } returns userProfileService
        every { applicationService.userService.userIdentityService } returns userIdentityService
        every { applicationService.chatService.bisqEasyOfferbookChannelService.channels } returns ObservableSet(listOf(channel))
        every { applicationService.bisqEasyService.bisqEasyOfferbookMessageService } returns messageService
        val provider = AndroidApplicationService.Provider().apply { this.applicationService = applicationService }

        every { userProfileService.findUserProfile(any()) } returns Optional.of(mockk(relaxed = true))
        every { userProfileService.userProfileById } returns ObservableHashMap()
        mockkObject(OfferItemPresentationVOFactory)
        every { OfferItemPresentationVOFactory.create(any(), any(), any(), any(), any(), any()) } answers { dto(verdict = arg(4)) }

        facade = NodeOffersServiceFacade(provider, mockk(relaxed = true), mockk(relaxed = true))
    }

    override fun onTearDown() {
        unmockkObject(OfferItemPresentationVOFactory)
    }

    @Test
    fun `a valid offer of another maker is shown with a sufficient verdict`() =
        runTest {
            val message = offerMessage(mine = false)
            every { messageService.isValid(message) } returns true

            val offers = facade.offersByAuthor(MAKER).offers

            assertEquals(true, offers.single().hasSellerSufficientReputation)
        }

    @Test
    fun `an invalid offer of another maker is hidden and its reputation is not checked`() =
        runTest {
            val message = offerMessage(mine = false)
            every { messageService.isValid(message) } returns false

            val offers = facade.offersByAuthor(MAKER).offers

            assertTrue(offers.isEmpty())
            verify(exactly = 0) { messageService.hasSellerSufficientReputation(any()) }
        }

    @Test
    fun `my invalid offer is shown with the verdict of the service`() =
        runTest {
            val message = offerMessage(mine = true)
            every { messageService.isValid(message) } returns false
            every { messageService.hasSellerSufficientReputation(message) } returns false

            val offers = facade.offersByAuthor(MAKER).offers

            assertEquals(false, offers.single().hasSellerSufficientReputation)
        }

    /** Without the maker profile no item can be built, whatever the validity would say. */
    @Test
    fun `an offer whose maker profile is missing is hidden`() =
        runTest {
            val message = offerMessage(mine = true)
            every { userProfileService.findUserProfile(MAKER) } returns Optional.empty()
            every { messageService.isValid(message) } returns true

            val offers = facade.offersByAuthor(MAKER).offers

            assertTrue(offers.isEmpty())
        }

    /** bisq2 bumps the revision twice per price refresh (clear + putAll): one rebuild for the burst. */
    @Test
    fun `revision bumps in a row rebuild once`() =
        runTest {
            val revision = Observable(0L)
            every { messageService.offerValidityRevision } returns revision
            facade.activate()
            runCurrent()
            val before = facade.offersRevision.value

            revision.set(1L)
            revision.set(2L)
            runCurrent()

            assertEquals(before + 1, facade.offersRevision.value)
            facade.deactivate()
        }

    private fun offerMessage(mine: Boolean): BisqEasyOfferbookMessage {
        val offer = mockk<BisqEasyOffer>(relaxed = true) { every { id } returns "o1" }
        val message =
            mockk<BisqEasyOfferbookMessage> {
                every { hasBisqEasyOffer() } returns true
                every { bisqEasyOffer } returns Optional.of(offer)
                every { authorUserProfileId } returns MAKER
                every { isMyMessage(any()) } returns mine
            }
        chatMessages.add(message)
        return message
    }

    private fun dto(verdict: Boolean): OfferItemPresentationDto {
        val market = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = "key", hash = "hash", id = MAKER),
            )
        val offer =
            BisqEasyOfferVO(
                id = "o1",
                date = 0L,
                makerNetworkId = makerNetworkId,
                direction = DirectionEnum.SELL,
                market = market,
                amountSpec = QuoteSideFixedAmountSpecVO(100_00L),
                priceSpec = FixPriceSpecVO(PriceQuoteVOFactory.fromPrice(100_00L, market)),
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = emptyList(),
            )
        return OfferItemPresentationDto(
            bisqEasyOffer = offer,
            isMyOffer = true,
            userProfile = createMockUserProfile("Alice"),
            formattedDate = "",
            formattedQuoteAmount = "",
            formattedBaseAmount = "",
            formattedPrice = "",
            formattedPriceSpec = "",
            quoteSidePaymentMethods = emptyList(),
            baseSidePaymentMethods = emptyList(),
            reputationScore = ReputationScoreVO(0, 0.0, 0),
            hasSellerSufficientReputation = verdict,
        )
    }
}
