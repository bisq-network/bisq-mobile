package network.bisq.mobile.presentation.offer.take_offer.btc_address

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakeMarketPriceServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakePayoutAddressPrepRepository
import network.bisq.mobile.presentation.common.test_utils.FakeTradesServiceFacade
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferBtcAddressPresenterTest : PresentationKoinTestBase() {
    private companion object {
        const val VALID_ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
    }

    private lateinit var mainPresenter: MainPresenter

    override fun onKoinReady() {
        mainPresenter = mockk(relaxed = true)
    }

    @Test
    fun `starts blank when no address was committed before`() =
        runTest {
            val (presenter, _) = createPresenter()

            assertEquals("", presenter.uiState.value.address)
            assertFalse(presenter.uiState.value.isValid)
        }

    @Test
    fun `re-entry restores the committed address as valid`() =
        runTest {
            val (presenter, _) = createPresenter(committedAddress = VALID_ADDRESS)

            assertEquals(VALID_ADDRESS, presenter.uiState.value.address)
            assertTrue(presenter.uiState.value.isValid)
        }

    @Test
    fun `next with a valid address commits it to the wizard model`() =
        runTest {
            val (presenter, coordinator) = createPresenter()

            presenter.onAction(TakeOfferBtcAddressUiAction.OnAddressInput(VALID_ADDRESS, isValid = true))
            presenter.onAction(TakeOfferBtcAddressUiAction.OnNext)

            assertEquals(VALID_ADDRESS, coordinator.takeOfferModel.btcAddress)
        }

    @Test
    fun `next with a blank field skips and clears a previously committed address`() =
        runTest {
            val (presenter, coordinator) = createPresenter(committedAddress = VALID_ADDRESS)

            presenter.onAction(TakeOfferBtcAddressUiAction.OnAddressInput("", isValid = false))
            presenter.onAction(TakeOfferBtcAddressUiAction.OnNext)

            assertEquals("", coordinator.takeOfferModel.btcAddress)
        }

    @Test
    fun `next with invalid input commits nothing`() =
        runTest {
            val (presenter, coordinator) = createPresenter(committedAddress = VALID_ADDRESS)

            presenter.onAction(TakeOfferBtcAddressUiAction.OnAddressInput("not-an-address", isValid = false))
            presenter.onAction(TakeOfferBtcAddressUiAction.OnNext)

            assertEquals(VALID_ADDRESS, coordinator.takeOfferModel.btcAddress)
        }

    @Test
    fun `back commits a valid address so review shows what was typed`() =
        runTest {
            val (presenter, coordinator) = createPresenter()

            presenter.onAction(TakeOfferBtcAddressUiAction.OnAddressInput(VALID_ADDRESS, isValid = true))
            presenter.onAction(TakeOfferBtcAddressUiAction.OnBack)

            assertEquals(VALID_ADDRESS, coordinator.takeOfferModel.btcAddress)
        }

    @Test
    fun `a scanned valid address is recognized as valid`() =
        runTest {
            val (presenter, _) = createPresenter()

            presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeClick)
            assertTrue(presenter.uiState.value.showBarcodeView)

            // Scheme prefix is stripped by the shared normalization before validating.
            presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeResult("bitcoin:$VALID_ADDRESS"))

            assertEquals(VALID_ADDRESS, presenter.uiState.value.address)
            assertTrue(presenter.uiState.value.isValid)
            assertFalse(presenter.uiState.value.showBarcodeView)
        }

    @Test
    fun `a failed scan surfaces the barcode error dialog`() =
        runTest {
            val (presenter, _) = createPresenter()

            presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeClick)
            presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeFail)

            assertFalse(presenter.uiState.value.showBarcodeView)
            assertTrue(presenter.uiState.value.showBarcodeError)

            presenter.onAction(TakeOfferBtcAddressUiAction.OnBarcodeErrorClose)
            assertFalse(presenter.uiState.value.showBarcodeError)
        }

    private fun createPresenter(committedAddress: String = ""): Pair<TakeOfferBtcAddressPresenter, TakeOfferCoordinator> {
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
                    btcMethods = listOf("MAIN_CHAIN"),
                    direction = DirectionEnum.SELL,
                ),
            ),
        )
        coordinator.commitBtcAddress(committedAddress)
        val presenter = TakeOfferBtcAddressPresenter(mainPresenter, coordinator)
        return presenter to coordinator
    }
}
