package network.bisq.mobile.presentation.offer.take_offer

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakeMarketPriceServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakePayoutAddressPrepRepository
import network.bisq.mobile.presentation.common.test_utils.FakeTradesServiceFacade
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.offer.take_offer.payment_method.TakeOfferPaymentMethodPresenter
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PlatformPresentationKoinTestBase
import kotlin.test.Test

/**
 * The settlement step's exit routing: after the settlement choice commits, the wizard continues
 * to the optional payout-address step only when the committed choice makes it applicable —
 * mainchain for a first-time buyer — and straight to review otherwise.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferPaymentMethodPresenterRoutingTest : PlatformPresentationKoinTestBase() {
    @Test
    fun baseSideNext_mainchainPickedByFirstTimeBuyer_routesToTheAddressStep() =
        runTest {
            val presenter = presenterForMultiSettlementFirstTimer()

            presenter.onBaseSidePaymentMethodSelected("MAIN_CHAIN")
            presenter.onBaseSideNext()

            verify { navigationManager.navigate(NavRoute.TakeOfferBtcAddress, any(), any()) }
        }

    @Test
    fun baseSideNext_lightningPicked_skipsTheAddressStepAndRoutesToReview() =
        runTest {
            val presenter = presenterForMultiSettlementFirstTimer()

            presenter.onBaseSidePaymentMethodSelected("LN")
            presenter.onBaseSideNext()

            verify { navigationManager.navigate(NavRoute.TakeOfferReviewTrade, any(), any()) }
        }

    private suspend fun presenterForMultiSettlementFirstTimer(): TakeOfferPaymentMethodPresenter {
        val coordinator =
            TakeOfferCoordinator(
                FakeMarketPriceServiceFacade(SettingsRepositoryMock(), OfferTestFactory.usdPrices()),
                FakeTradesServiceFacade(),
                FakeConfigServiceFacade(),
                mockk(relaxed = true),
                FakePayoutAddressPrepRepository(),
            )
        coordinator.selectOfferToTake(
            OfferItemPresentationModel(
                OfferTestFactory.makeOfferDto(
                    amountSpec = QuoteSideFixedAmountSpecVO(amount = 500_000L),
                    btcMethods = listOf("MAIN_CHAIN", "LN"),
                    direction = DirectionEnum.SELL,
                ),
            ),
            takerProfileId = "profile-1",
        )
        return TakeOfferPaymentMethodPresenter(mockk(relaxed = true), coordinator)
    }
}
