package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import kotlin.test.Test

class TradeOutOfSyncPaneUiTest : PresentationKoinComposeTestBase() {
    private val headline get() = "mobile.bisqEasy.openTrades.outOfSync.headline".i18n()
    private val openChat get() = "mobile.bisqEasy.openTrades.outOfSync.openChat".i18n()
    private val reportToMediator get() = "bisqEasy.openTrades.reportToMediator".i18n()
    private val openSupportChannel get() = "mobile.community.support.openChannel".i18n()

    private fun renderPane(
        isTradeOutOfSync: Boolean,
        isInMediation: Boolean = false,
        showSupportChannel: Boolean = true,
    ): Pair<OpenTradePresenter, TradeDetailsHeaderPresenter> {
        val presenter = mockk<OpenTradePresenter>(relaxed = true)
        val headerPresenter = mockk<TradeDetailsHeaderPresenter>(relaxed = true)
        every { presenter.isTradeOutOfSync } returns MutableStateFlow(isTradeOutOfSync)
        every { presenter.isInMediation } returns MutableStateFlow(isInMediation)
        every { presenter.isSupportChannelAvailable } returns MutableStateFlow(showSupportChannel)

        setTestContent {
            TradeOutOfSyncPane(presenter = presenter, headerPresenter = headerPresenter)
        }
        return presenter to headerPresenter
    }

    @Test
    fun `an out of sync trade shows the pane with both actions`() {
        val (presenter, headerPresenter) = renderPane(isTradeOutOfSync = true)

        composeTestRule.onNodeWithText(headline).assertExists()

        composeTestRule.onNodeWithText(openChat).performClick()
        verify { presenter.onOpenChat() }

        composeTestRule.onNodeWithText(reportToMediator).performClick()
        verify { headerPresenter.onOpenMediationConfirmationDialog() }
    }

    @Test
    fun `an out of sync trade opens the support channel`() {
        val (presenter, _) = renderPane(isTradeOutOfSync = true)

        composeTestRule.onNodeWithText(openSupportChannel).performClick()
        verify { presenter.onOpenSupportChannel() }
    }

    @Test
    fun `a trade in sync renders nothing`() {
        renderPane(isTradeOutOfSync = false)

        composeTestRule.onNodeWithText(headline).assertDoesNotExist()
    }

    @Test
    fun `the support channel is hidden when it is unavailable`() {
        renderPane(isTradeOutOfSync = true, showSupportChannel = false)

        composeTestRule.onNodeWithText(headline).assertExists()
        composeTestRule.onNodeWithText(openSupportChannel).assertDoesNotExist()
    }

    @Test
    fun `the mediator action is hidden once mediation is already active`() {
        renderPane(isTradeOutOfSync = true, isInMediation = true)

        composeTestRule.onNodeWithText(headline).assertExists()
        composeTestRule.onNodeWithText(reportToMediator).assertDoesNotExist()
        composeTestRule.onNodeWithText(openSupportChannel).assertDoesNotExist()
    }
}
