package network.bisq.mobile.presentation.offerbook

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.offerbook.MarketListItem
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfigs
import network.bisq.mobile.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.data.replicated.common.currency.MarketVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactListEntryVO
import network.bisq.mobile.data.replicated.user.contact_list.ContactReasonEnum
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.contacts.ContactsServiceFacade
import network.bisq.mobile.data.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.OfferbookFilterConfigRepository
import network.bisq.mobile.domain.service.community.CommunityHubService
import network.bisq.mobile.domain.service.community.CommunitySegment
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookPresenterContactTagsTest : PlatformPresentationKoinTestBase() {
    private class FakeOfferbookFilterConfigRepository(
        initialConfigs: OfferbookFilterConfigs = OfferbookFilterConfigs(),
    ) : OfferbookFilterConfigRepository {
        private val _data = MutableStateFlow(initialConfigs)
        override val data: StateFlow<OfferbookFilterConfigs> = _data

        override suspend fun getConfig(marketKey: String): OfferbookFilterConfig = _data.value.configsByMarket[marketKey] ?: OfferbookFilterConfig()

        override suspend fun setConfig(
            marketKey: String,
            config: OfferbookFilterConfig,
        ) {
            _data.value = _data.value.copy(configsByMarket = _data.value.configsByMarket + (marketKey to config))
        }
    }

    private fun contactEntry(
        peer: UserProfileVO,
        tag: String? = null,
    ) = ContactListEntryVO(
        userProfile = peer,
        date = 1_700_000_000_000,
        contactReason = ContactReasonEnum.MANUALLY_ADDED,
        tag = tag,
    )

    private fun buildPresenter(
        contacts: List<ContactListEntryVO>,
        liveSegments: Set<CommunitySegment>,
    ): OfferbookPresenter {
        val mainPresenter =
            MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService())
        val offersService = mockk<OffersServiceFacade>()
        every { offersService.offerbookListItems } returns MutableStateFlow(emptyList())
        every { offersService.selectedOfferbookMarket } returns
            MutableStateFlow(
                OfferbookMarket(
                    MarketVO(
                        baseCurrencyCode = "BTC",
                        quoteCurrencyCode = "USD",
                        baseCurrencyName = "Bitcoin",
                        quoteCurrencyName = "US Dollar",
                    ),
                ),
            )
        every { offersService.isOfferbookLoading } returns MutableStateFlow(false)
        val userProfileService = mockk<UserProfileServiceFacade>(relaxed = true)
        every { userProfileService.selectedUserProfile } returns MutableStateFlow(createMockUserProfile("me"))
        val marketPriceServiceFacade =
            object : MarketPriceServiceFacade(mockk(relaxed = true)) {
                override fun findMarketPriceItem(marketVO: MarketVO) = null

                override fun findUSDMarketPriceItem() = null

                override fun refreshSelectedFormattedMarketPrice() {}

                override fun selectMarket(marketListItem: MarketListItem): Result<Unit> = Result.success(Unit)
            }
        return OfferbookPresenter(
            mainPresenter,
            offersService,
            mockk<TakeOfferCoordinator>(relaxed = true),
            mockk<CreateOfferCoordinator>(relaxed = true),
            marketPriceServiceFacade,
            userProfileService,
            mockk(relaxed = true),
            mockk(relaxed = true),
            FakeOfferbookFilterConfigRepository(),
            configServiceFacade = FakeConfigServiceFacade(),
            appUpdateLinker = FakeAppUpdateLinker(),
            contactsServiceFacade =
                mockk<ContactsServiceFacade>(relaxed = true) {
                    every { this@mockk.contacts } returns MutableStateFlow(contacts)
                },
            communityHubService =
                mockk<CommunityHubService> {
                    every { this@mockk.liveSegments } returns MutableStateFlow(liveSegments)
                },
        )
    }

    @Test
    fun `contact tags stay empty while the contacts segment is not live`() =
        runTest {
            val peer = createMockUserProfile("peer-1")
            val presenter = buildPresenter(contacts = listOf(contactEntry(peer, tag = "SEPA")), liveSegments = emptySet())

            presenter.onViewAttached()
            advanceUntilIdle()

            assertTrue(presenter.contactTags.value.isEmpty())
        }

    @Test
    fun `tagged contact exposes its tag keyed by maker profile id`() =
        runTest {
            val peer = createMockUserProfile("peer-1")
            val presenter =
                buildPresenter(
                    contacts = listOf(contactEntry(peer, tag = "Reliable SEPA trader")),
                    liveSegments = setOf(CommunitySegment.CONTACTS),
                )

            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(mapOf(peer.id to "Reliable SEPA trader"), presenter.contactTags.value)
        }

    @Test
    fun `contact without a tag maps to an empty string so the card can fall back to the generic label`() =
        runTest {
            val peer = createMockUserProfile("peer-1")
            val presenter =
                buildPresenter(
                    contacts = listOf(contactEntry(peer, tag = null)),
                    liveSegments = setOf(CommunitySegment.CONTACTS),
                )

            presenter.onViewAttached()
            advanceUntilIdle()

            assertTrue(presenter.contactTags.value.containsKey(peer.id))
            assertEquals("", presenter.contactTags.value[peer.id])
        }

    @Test
    fun `padded tag is trimmed before it is published to the card`() =
        runTest {
            val peer = createMockUserProfile("peer-1")
            val presenter =
                buildPresenter(
                    contacts = listOf(contactEntry(peer, tag = "  Reliable SEPA ")),
                    liveSegments = setOf(CommunitySegment.CONTACTS),
                )

            presenter.onViewAttached()
            advanceUntilIdle()

            assertEquals(mapOf(peer.id to "Reliable SEPA"), presenter.contactTags.value)
        }
}
