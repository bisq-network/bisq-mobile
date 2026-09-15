package network.bisq.mobile.presentation.offer.take_offer.btc_address

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performTextInput
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import network.bisq.mobile.data.replicated.offer.DirectionEnum
import network.bisq.mobile.data.replicated.offer.amount.spec.QuoteSideFixedAmountSpecVO
import network.bisq.mobile.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.FakeConfigServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakeMarketPriceServiceFacade
import network.bisq.mobile.presentation.common.test_utils.FakePayoutAddressPrepRepository
import network.bisq.mobile.presentation.common.test_utils.FakeTradesServiceFacade
import network.bisq.mobile.presentation.common.test_utils.OfferTestFactory
import network.bisq.mobile.presentation.common.ui.components.molecules.ITopBarPresenter
import network.bisq.mobile.presentation.common.ui.components.molecules.PreviewTopBarPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.offer.take_offer.TakeOfferCoordinator
import network.bisq.mobile.test.mocks.SettingsRepositoryMock
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * The optional payout-address wizard step, rendered with a real presenter and coordinator so the
 * Next/Skip contract is exercised end to end: Skip while blank, Continue for content, disabled
 * only on invalid input. Gating and commit semantics live in the presenter and coordinator tests.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferBtcAddressScreenUiTest : PresentationKoinComposeTestBase() {
    private companion object {
        const val VALID_ADDRESS = "bc1qw508d6qejxtdg4y5r3zarvary0c5xw7kv8f3t4"
    }

    private lateinit var mainPresenter: MainPresenter
    private lateinit var coordinator: TakeOfferCoordinator

    private val skipLabel get() = "mobile.takeOffer.addressStep.next.skip".i18n()
    private val continueLabel get() = "mobile.takeOffer.addressStep.next.continue".i18n()

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<ITopBarPresenter> { PreviewTopBarPresenter() }
                single { coordinator }
                factory { TakeOfferBtcAddressPresenter(mainPresenter, coordinator) }
            },
        )

    override fun onKoinReady() {
        mainPresenter = mockk(relaxed = true)
        coordinator =
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
    }

    @Test
    fun `a blank field offers skip and it is enabled`() {
        setTestContent { TakeOfferBtcAddressScreen() }

        composeTestRule.onNodeWithText("mobile.takeOffer.addressStep.headline".i18n()).assertExists()
        composeTestRule.onNodeWithText(skipLabel).assertIsEnabled()
        composeTestRule.onNodeWithText(continueLabel).assertDoesNotExist()
    }

    @Test
    fun `a valid address flips the button to an enabled continue`() {
        setTestContent { TakeOfferBtcAddressScreen() }

        composeTestRule.onNode(hasSetTextAction()).performTextInput(VALID_ADDRESS)
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(continueLabel).assertIsEnabled()
        composeTestRule.onNodeWithText(skipLabel).assertDoesNotExist()
    }

    @Test
    fun `invalid input disables continue instead of blocking with a dialog`() {
        setTestContent { TakeOfferBtcAddressScreen() }

        composeTestRule.onNode(hasSetTextAction()).performTextInput("not-an-address")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText(continueLabel).assertIsNotEnabled()
    }
}
