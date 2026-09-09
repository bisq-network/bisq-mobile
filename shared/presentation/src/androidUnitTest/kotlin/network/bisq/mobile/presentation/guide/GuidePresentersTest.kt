package network.bisq.mobile.presentation.guide

import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideOverviewPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideProcessPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideSecurityPresenter
import network.bisq.mobile.presentation.guide.trade_guide.TradeGuideTradeRulesPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideDownloadPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideIntroPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideNewPresenter
import network.bisq.mobile.presentation.guide.wallet_guide.WalletGuideReceivingPresenter
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.coroutines.PresentationKoinTestBase
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * The guide wizards' whole contract is navigation, so one class covers all eight step
 * presenters: every step's [GuideUiAction.OnNextClick] goes to the declared next route (or pops
 * the wizard at its end), every [GuideUiAction.OnPrevClick] goes back, and the trade-rules step
 * confirms the rules exactly when they were not confirmed before.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class GuidePresentersTest : PresentationKoinTestBase() {
    private lateinit var mainPresenter: MainPresenter
    private lateinit var settingsServiceFacade: SettingsServiceFacade
    private val tradeRulesConfirmed = MutableStateFlow(false)

    override fun onKoinReady() {
        mainPresenter = mockk(relaxed = true)
        settingsServiceFacade = mockk(relaxed = true)
        tradeRulesConfirmed.value = false
        every { settingsServiceFacade.tradeRulesConfirmed } returns tradeRulesConfirmed
    }

    @Test
    fun `every step's next goes to its declared next route`() =
        runTest {
            val forwardSteps: List<Pair<(GuideUiAction) -> Unit, NavRoute>> =
                listOf(
                    TradeGuideOverviewPresenter(mainPresenter)::onAction to NavRoute.TradeGuideSecurity,
                    TradeGuideSecurityPresenter(mainPresenter)::onAction to NavRoute.TradeGuideProcess,
                    TradeGuideProcessPresenter(mainPresenter)::onAction to NavRoute.TradeGuideTradeRules,
                    WalletGuideIntroPresenter(mainPresenter)::onAction to NavRoute.WalletGuideDownload,
                    WalletGuideDownloadPresenter(mainPresenter)::onAction to NavRoute.WalletGuideNewWallet,
                    WalletGuideNewPresenter(mainPresenter)::onAction to NavRoute.WalletGuideReceiving,
                )

            forwardSteps.forEach { (onAction, expectedRoute) ->
                onAction(GuideUiAction.OnNextClick)
                advanceUntilIdle()
                verify { navigationManager.navigate(expectedRoute, any(), any()) }
            }
        }

    @Test
    fun `every step's prev navigates back`() =
        runTest {
            val steps: List<(GuideUiAction) -> Unit> =
                listOf(
                    TradeGuideOverviewPresenter(mainPresenter)::onAction,
                    TradeGuideSecurityPresenter(mainPresenter)::onAction,
                    TradeGuideProcessPresenter(mainPresenter)::onAction,
                    TradeGuideTradeRulesPresenter(mainPresenter, settingsServiceFacade)::onAction,
                    WalletGuideIntroPresenter(mainPresenter)::onAction,
                    WalletGuideDownloadPresenter(mainPresenter)::onAction,
                    WalletGuideNewPresenter(mainPresenter)::onAction,
                    WalletGuideReceivingPresenter(mainPresenter)::onAction,
                )

            steps.forEach { it(GuideUiAction.OnPrevClick) }
            advanceUntilIdle()

            verify(exactly = steps.size) { navigationManager.navigateBack(any()) }
        }

    @Test
    fun `the wallet guide's last step pops the wizard instead of stacking a page`() =
        runTest {
            WalletGuideReceivingPresenter(mainPresenter).onAction(GuideUiAction.OnNextClick)
            advanceUntilIdle()

            verify { navigationManager.navigateBackTo(NavRoute.WalletGuideIntro, true, false) }
            verify(exactly = 0) { navigationManager.navigate(any(), any(), any()) }
        }

    @Test
    fun `trade rules next confirms the rules when not yet confirmed and leaves the guide`() =
        runTest {
            val presenter = TradeGuideTradeRulesPresenter(mainPresenter, settingsServiceFacade)
            assertFalse(presenter.uiState.value.tradeRulesConfirmed)

            presenter.onAction(GuideUiAction.OnNextClick)
            advanceUntilIdle()

            coVerify(exactly = 1) { settingsServiceFacade.confirmTradeRules(true) }
            verify { navigationManager.navigateBackTo(NavRoute.TradeGuideSecurity, true, false) }
        }

    @Test
    fun `trade rules next does not re-confirm already confirmed rules`() =
        runTest {
            tradeRulesConfirmed.value = true
            val presenter = TradeGuideTradeRulesPresenter(mainPresenter, settingsServiceFacade)
            assertTrue(presenter.uiState.value.tradeRulesConfirmed)

            presenter.onAction(GuideUiAction.OnNextClick)
            advanceUntilIdle()

            coVerify(exactly = 0) { settingsServiceFacade.confirmTradeRules(any()) }
            verify { navigationManager.navigateBackTo(NavRoute.TradeGuideSecurity, true, false) }
        }
}
