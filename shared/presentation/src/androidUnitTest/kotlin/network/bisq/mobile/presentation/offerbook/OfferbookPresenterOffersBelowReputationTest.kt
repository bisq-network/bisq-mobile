package network.bisq.mobile.presentation.offerbook

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfig
import network.bisq.mobile.data.model.offerbook.OfferbookMarket
import network.bisq.mobile.data.replicated.common.currency.MarketVOFactory
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.service.alert.TradeRestrictingAlertServiceFacade
import network.bisq.mobile.data.service.offers.OffersServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.OfferbookFilterConfigRepository
import network.bisq.mobile.domain.service.offers.OffendingOffer
import network.bisq.mobile.domain.service.offers.OffersBelowReputationService
import network.bisq.mobile.domain.service.offers.OffersBelowReputationState
import network.bisq.mobile.presentation.common.test_utils.FakeAppUpdateLinker
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakeMarketPriceServiceFacade
import network.bisq.mobile.presentation.common.test_utils.MainPresenterTestFactory
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.presentation.common.test_utils.TestApplicationLifecycleService
import network.bisq.mobile.presentation.common.test_utils.testOffersBelowReputationService
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals

/** The offerbook's part of the warning: which cards carry the badge, and deletions from a card. */
@OptIn(ExperimentalCoroutinesApi::class)
class OfferbookPresenterOffersBelowReputationTest : PlatformPresentationKoinTestBase() {
    private val serviceState = MutableStateFlow(OffersBelowReputationState())
    private val service = testOffersBelowReputationService(serviceState)
    private val offersService =
        mockk<OffersServiceFacade> {
            every { offerbookListItems } returns MutableStateFlow(emptyList())
            every { selectedOfferbookMarket } returns MutableStateFlow(OfferbookMarket(MarketVOFactory.USD))
            every { isOfferbookLoading } returns MutableStateFlow(false)
            coEvery { deleteOffer(any()) } returns Result.success(true)
        }

    @Test
    fun `offending offer ids follow the service`() =
        runTest {
            val presenter = attachPresenter()

            serviceState.value =
                OffersBelowReputationState(
                    offendingOffers = listOf(OffendingOffer(offerId = "a", market = MarketVOFactory.USD, formattedAmount = "50 USD")),
                )
            advanceUntilIdle()

            assertEquals(setOf("a"), presenter.offendingOfferIds.value)
        }

    @Test
    fun `deleting my offer from its card drops it from the warning`() =
        runTest {
            val presenter = attachPresenter()
            val offer = myOffer()

            presenter.onOfferSelected(offer)
            presenter.onConfirmedDeleteOffer()
            advanceUntilIdle()

            verify { service.markRemoved(offer.offerId) }
        }

    private fun myOffer(): OfferItemPresentationModel {
        val dto = OfferTestFactory.makeOfferDto()
        return OfferItemPresentationModel(dto.copy(isMyOffer = true))
    }

    private fun TestScope.attachPresenter(): OfferbookPresenter {
        val userProfileService =
            mockk<UserProfileServiceFacade>(relaxed = true) {
                every { selectedUserProfile } returns MutableStateFlow(createMockUserProfile("me"))
            }
        val filterConfigRepository =
            mockk<OfferbookFilterConfigRepository>(relaxed = true) {
                coEvery { getConfig(any()) } returns OfferbookFilterConfig()
            }
        val presenter =
            OfferbookPresenter(
                MainPresenterTestFactory.create(applicationLifecycleService = TestApplicationLifecycleService()),
                offersService,
                mockk(relaxed = true),
                mockk(relaxed = true),
                FakeMarketPriceServiceFacade(SettingsRepositoryMock(), OfferTestFactory.usdPrices()),
                userProfileService,
                mockk(relaxed = true),
                mockk<TradeRestrictingAlertServiceFacade>(relaxed = true) {
                    every { alert } returns MutableStateFlow(null)
                },
                filterConfigRepository,
                configServiceFacade = FakeConfigServiceFacade(),
                appUpdateLinker = FakeAppUpdateLinker(),
                contactsServiceFacade = mockk(relaxed = true),
                communityHubService = mockk(relaxed = true),
                offersBelowReputationService = service,
                computationDispatcher = testDispatcher,
            )
        presenter.onViewAttached()
        advanceUntilIdle()
        return presenter
    }
}
