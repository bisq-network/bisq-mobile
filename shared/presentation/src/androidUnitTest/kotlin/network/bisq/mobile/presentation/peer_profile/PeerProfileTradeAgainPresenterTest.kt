package network.bisq.mobile.presentation.peer_profile

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
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
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.data.service.offers.AuthorOffersSnapshot
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.trades.TradesServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.core.pagination.PaginatedResponse
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferEligibility
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The "Trade again" section of the peer profile: the relationship gate (traded-before OR contact),
 * the recency-ordered market groups, and the tap path through the shared take-offer eligibility
 * gate. The data sources themselves are covered where they live — `offersByAuthor` in the facade
 * tests, `hasTradedWith` in the domain extension test, the eligibility copy in the coordinator test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PeerProfileTradeAgainPresenterTest : PresentationKoinTestBase() {
    private val usdMarket = MarketVO("BTC", "USD", "Bitcoin", "US Dollar")
    private val eurMarket = MarketVO("BTC", "EUR", "Bitcoin", "Euro")

    private val peer = createMockUserProfile(PEER_ID)
    private val myProfile = createMockUserProfile(OWN_ID)

    private lateinit var userProfileServiceFacade: UserProfileServiceFacade
    private lateinit var offersServiceFacade: OffersServiceFacade
    private lateinit var tradesServiceFacade: TradesServiceFacade
    private lateinit var takeOfferCoordinator: TakeOfferCoordinator
    private lateinit var ignoredProfileIds: MutableStateFlow<Set<String>>

    private companion object {
        const val PEER_ID = "peer-1"
        const val OWN_ID = "my-profile"
    }

    override fun onKoinReady() {
        I18nSupport.initialize("en")
        ignoredProfileIds = MutableStateFlow(emptySet())
        userProfileServiceFacade =
            mockk(relaxed = true) {
                every { ignoredProfileIds } returns this@PeerProfileTradeAgainPresenterTest.ignoredProfileIds
                every { userProfiles } returns MutableStateFlow(listOf(myProfile))
                every { selectedUserProfile } returns MutableStateFlow(myProfile)
            }
        coEvery { userProfileServiceFacade.findUserProfile(PEER_ID) } returns peer
        offersServiceFacade =
            mockk(relaxed = true) {
                coEvery { offersByAuthor(any()) } returns AuthorOffersSnapshot(emptyList(), mayBeIncomplete = false)
            }
        tradesServiceFacade =
            mockk(relaxed = true) {
                every { openTradeItems } returns MutableStateFlow(emptyList())
                coEvery { getClosedTradesPaginated(any(), any(), any(), any(), any()) } returns
                    Result.success(PaginatedResponse(emptyList(), page = 1, pageSize = 100, totalItems = 0, totalPages = 1))
            }
        takeOfferCoordinator = mockk(relaxed = true)
    }

    private fun TestScope.startPresenter(isContact: Boolean = false): PeerProfilePresenter {
        val presenter =
            PeerProfilePresenter(
                userProfileServiceFacade = userProfileServiceFacade,
                reputationServiceFacade =
                    mockk(relaxed = true) {
                        every { scoreByUserProfileId } returns MutableStateFlow(emptyMap())
                        coEvery { getReputation(any()) } returns
                            Result.success(ReputationScoreVO(totalScore = 10_000L, fiveSystemScore = 4.0, ranking = 3))
                    },
                privateChatServiceFacade = mockk(relaxed = true) { every { isSupported } returns flowOf(false) },
                contactsServiceFacade =
                    mockk {
                        every { contacts } returns
                            MutableStateFlow(
                                if (isContact) listOf(contactEntryFor(peer)) else emptyList(),
                            )
                        every { isLoaded } returns MutableStateFlow(true)
                    },
                communityHubService = mockk { every { liveSegments } returns MutableStateFlow(emptySet()) },
                offersServiceFacade = offersServiceFacade,
                tradesServiceFacade = tradesServiceFacade,
                takeOfferCoordinator = takeOfferCoordinator,
                marketPriceServiceFacade = mockk(relaxed = true),
                configServiceFacade = FakeConfigServiceFacade(),
                mainPresenter = mockk<MainPresenter>(relaxed = true),
            )
        presenter.initialize(PEER_ID)
        advanceUntilIdle()
        return presenter
    }

    private fun contactEntryFor(profile: UserProfileVO) =
        network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO(
            userProfile = profile,
            date = 1_700_000_000_000,
            contactReason = network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum.MANUALLY_ADDED,
            trustScore = null,
            tag = null,
            notes = null,
        )

    private fun openTradeWith(profile: UserProfileVO): network.bisq.mobile.data.replicated.presentation.open_trades.TradeItemPresentationModel = mockk { every { peersUserProfile } returns profile }

    private fun peerOffer(
        id: String,
        market: MarketVO = usdMarket,
        date: Long = 0L,
        direction: DirectionEnum = DirectionEnum.SELL,
    ): OfferItemPresentationModel {
        val makerNetworkId =
            NetworkIdVO(
                AddressByTransportTypeMapVO(mapOf()),
                PubKeyVO(PublicKeyVO("pub"), keyId = PEER_ID, hash = PEER_ID, id = PEER_ID),
            )
        val offer =
            BisqEasyOfferVO(
                id = id,
                date = date,
                makerNetworkId = makerNetworkId,
                direction = direction,
                market = market,
                amountSpec = QuoteSideFixedAmountSpecVO(500_000L),
                priceSpec = FixPriceSpecVO(with(PriceQuoteVOFactory) { fromPrice(100_000_00L, market) }),
                protocolTypes = emptyList(),
                baseSidePaymentMethodSpecs = emptyList(),
                quoteSidePaymentMethodSpecs = emptyList(),
                offerOptions = emptyList(),
                supportedLanguageCodes = emptyList(),
            )
        return OfferItemPresentationModel(
            OfferItemPresentationDto(
                bisqEasyOffer = offer,
                isMyOffer = false,
                userProfile = peer,
                formattedDate = "",
                formattedQuoteAmount = "",
                formattedBaseAmount = "",
                formattedPrice = "",
                formattedPriceSpec = "",
                quoteSidePaymentMethods = listOf("SEPA"),
                baseSidePaymentMethods = listOf("MAIN_CHAIN"),
                reputationScore = ReputationScoreVO(0, 0.0, 0),
            ),
        )
    }

    private fun stubOffers(vararg offers: OfferItemPresentationModel) {
        coEvery { offersServiceFacade.offersByAuthor(PEER_ID) } returns
            AuthorOffersSnapshot(offers.toList(), mayBeIncomplete = false)
    }

    // ---- visibility gate ----

    @Test
    fun `a previously traded peer with offers gets the section`() =
        runTest {
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(listOf(openTradeWith(peer)))
            stubOffers(peerOffer("o1"))

            val presenter = startPresenter()

            assertTrue(presenter.uiState.value.hasTradedBefore)
            assertTrue(presenter.uiState.value.showPeerOffersSection)
        }

    @Test
    fun `a contact never traded with still gets the section`() =
        runTest {
            stubOffers(peerOffer("o1"))

            val presenter = startPresenter(isContact = true)

            assertFalse(presenter.uiState.value.hasTradedBefore)
            assertTrue(presenter.uiState.value.showPeerOffersSection)
        }

    @Test
    fun `a stranger does not get the section even with offers`() =
        runTest {
            stubOffers(peerOffer("o1"))

            val presenter = startPresenter()

            assertFalse(presenter.uiState.value.showPeerOffersSection)
        }

    @Test
    fun `an ignored peer does not get the section`() =
        runTest {
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(listOf(openTradeWith(peer)))
            stubOffers(peerOffer("o1"))
            ignoredProfileIds.value = setOf(PEER_ID)

            val presenter = startPresenter()

            assertFalse(presenter.uiState.value.showPeerOffersSection)
        }

    @Test
    fun `a traded peer with no offers and a complete view hides the section`() =
        runTest {
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(listOf(openTradeWith(peer)))

            val presenter = startPresenter()

            assertTrue(presenter.uiState.value.hasTradedBefore)
            assertFalse(presenter.uiState.value.showPeerOffersSection)
        }

    // ---- grouping and ordering ----

    @Test
    fun `market groups and their offers order by most recent offer first`() =
        runTest {
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(listOf(openTradeWith(peer)))
            stubOffers(
                peerOffer("usd-old", usdMarket, date = 1_000L),
                peerOffer("eur-new", eurMarket, date = 9_000L),
                peerOffer("usd-newest", usdMarket, date = 10_000L),
            )

            val presenter = startPresenter()

            val groups = presenter.uiState.value.peerOffers
            assertEquals(listOf("BTC/USD", "BTC/EUR"), groups.map { it.marketCodes })
            assertEquals(listOf("usd-newest", "usd-old"), groups.first().offers.map { it.offerId })
        }

    // ---- syncing honesty (Connect cold start) ----

    @Test
    fun `an incomplete cache shows syncing and a later complete read resolves it`() =
        runTest {
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(listOf(openTradeWith(peer)))
            coEvery { offersServiceFacade.offersByAuthor(PEER_ID) } returns
                AuthorOffersSnapshot(emptyList(), mayBeIncomplete = true) andThen
                AuthorOffersSnapshot(listOf(peerOffer("late")), mayBeIncomplete = false)

            val presenter = startPresenter()

            assertFalse(presenter.uiState.value.isPeerOffersSyncing)
            assertEquals(
                listOf("late"),
                presenter.uiState.value.peerOffers
                    .flatMap { it.offers }
                    .map { it.offerId },
            )
            assertTrue(presenter.uiState.value.showPeerOffersSection)
        }

    // ---- tap path through the shared eligibility gate ----

    @Test
    fun `tapping an eligible offer starts the wizard at the coordinator's first screen`() =
        runTest {
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(listOf(openTradeWith(peer)))
            val offer = peerOffer("o1")
            stubOffers(offer)
            coEvery { takeOfferCoordinator.checkTakeOfferEligibility(any(), any()) } returns TakeOfferEligibility.Eligible
            every { takeOfferCoordinator.firstScreen() } returns NavRoute.TakeOfferReviewTrade

            val presenter = startPresenter()
            presenter.onAction(PeerProfileUiAction.OnPeerOfferClick("o1"))
            advanceUntilIdle()

            verify(exactly = 1) { takeOfferCoordinator.selectOfferToTake(offer) }
            verify { navigationManager.navigate(NavRoute.TakeOfferReviewTrade, any(), any()) }
        }

    @Test
    fun `tapping an ineligible offer opens the reputation dialog instead of navigating`() =
        runTest {
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(listOf(openTradeWith(peer)))
            stubOffers(peerOffer("o1"))
            coEvery { takeOfferCoordinator.checkTakeOfferEligibility(any(), any()) } returns
                TakeOfferEligibility.NotEnoughReputation("headline", "message", isSellerAsTakerWarning = true)

            val presenter = startPresenter()
            presenter.onAction(PeerProfileUiAction.OnPeerOfferClick("o1"))
            advanceUntilIdle()

            val dialog = presenter.uiState.value.notEnoughReputation
            assertEquals("headline", dialog?.headline)
            assertEquals("message", dialog?.message)
            assertEquals(true, dialog?.isSellerAsTakerWarning)
            assertTrue(presenter.isTakeOfferEnabled.value)
            verify(exactly = 0) { takeOfferCoordinator.selectOfferToTake(any()) }
            verify(exactly = 0) { navigationManager.navigate(any(), any(), any()) }

            presenter.onAction(PeerProfileUiAction.OnDismissNotEnoughReputationDialog)
            assertNull(presenter.uiState.value.notEnoughReputation)
        }

    @Test
    fun `a tap on an offer that is no longer loaded is ignored`() =
        runTest {
            every { tradesServiceFacade.openTradeItems } returns MutableStateFlow(listOf(openTradeWith(peer)))
            stubOffers(peerOffer("o1"))

            val presenter = startPresenter()
            presenter.onAction(PeerProfileUiAction.OnPeerOfferClick("gone"))
            advanceUntilIdle()

            verify(exactly = 0) { navigationManager.navigate(any(), any(), any()) }
            assertTrue(presenter.isTakeOfferEnabled.value)
        }

    // ---- degradation ----

    @Test
    fun `a failing trade-history lookup leaves the gate to the contact state`() =
        runTest {
            coEvery { tradesServiceFacade.getClosedTradesPaginated(any(), any(), any(), any(), any()) } returns
                Result.failure(RuntimeException("closed-trades API unavailable"))
            stubOffers(peerOffer("o1"))

            val presenter = startPresenter(isContact = true)

            assertFalse(presenter.uiState.value.hasTradedBefore)
            assertTrue(presenter.uiState.value.showPeerOffersSection)
        }

    @Test
    fun `own profile never queries offers or trade history`() =
        runTest {
            stubOffers(peerOffer("o1"))
            val ownPresenter =
                PeerProfilePresenter(
                    userProfileServiceFacade = userProfileServiceFacade,
                    reputationServiceFacade = mockk(relaxed = true) { every { scoreByUserProfileId } returns MutableStateFlow(emptyMap()) },
                    privateChatServiceFacade = mockk(relaxed = true) { every { isSupported } returns flowOf(false) },
                    contactsServiceFacade =
                        mockk {
                            every { contacts } returns MutableStateFlow(emptyList())
                            every { isLoaded } returns MutableStateFlow(true)
                        },
                    communityHubService = mockk { every { liveSegments } returns MutableStateFlow(emptySet()) },
                    offersServiceFacade = offersServiceFacade,
                    tradesServiceFacade = tradesServiceFacade,
                    takeOfferCoordinator = takeOfferCoordinator,
                    marketPriceServiceFacade = mockk(relaxed = true),
                    configServiceFacade = FakeConfigServiceFacade(),
                    mainPresenter = mockk<MainPresenter>(relaxed = true),
                )
            ownPresenter.initialize(OWN_ID)
            advanceUntilIdle()

            assertFalse(ownPresenter.uiState.value.showPeerOffersSection)
            coVerify(exactly = 0) { offersServiceFacade.offersByAuthor(OWN_ID) }
        }
}
