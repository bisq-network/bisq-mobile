package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals

class InterruptedTradePaneUiTest : PresentationKoinComposeTestBase() {
    private lateinit var presenter: InterruptedTradePresenter
    private lateinit var onOpenSupportChannel: () -> Unit

    private val openSupportChannel get() = "mobile.community.support.openChannel".i18n()

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                factory<InterruptedTradePresenter> { presenter }
            },
        )

    override fun onKoinReady() {
        presenter = mockk(relaxed = true)
        onOpenSupportChannel = mockk(relaxed = true)
        every { presenter.interruptionInfoVisible } returns MutableStateFlow(false)
        every { presenter.interruptedTradeInfo } returns MutableStateFlow("")
        every { presenter.errorMessageVisible } returns MutableStateFlow(true)
        every { presenter.isInMediation } returns MutableStateFlow(false)
        every { presenter.reportToMediatorButtonVisible } returns MutableStateFlow(false)
        every { presenter.showNoMediatorAvailableWarningDialog } returns MutableStateFlow(false)
        every { presenter.showMediationRequestedDialog } returns MutableStateFlow(false)
        every { presenter.isCloseTradeEnabled } returns MutableStateFlow(true)
        every { presenter.isReportToMediatorEnabled } returns MutableStateFlow(true)
        every { presenter.errorMessage } returns "simulated error"
    }

    private fun renderPane(showSupportChannel: Boolean) {
        setTestContent {
            InterruptedTradePane(
                showSupportChannel = showSupportChannel,
                onOpenSupportChannel = onOpenSupportChannel,
            )
        }
    }

    @Test
    fun `error state shows and opens the support channel`() {
        renderPane(showSupportChannel = true)

        composeTestRule.onNodeWithText(openSupportChannel).assertIsDisplayed()
        composeTestRule.onNodeWithText("action.close".i18n()).assertIsDisplayed()
        assertEquals(
            composeTestRule.onNodeWithText(openSupportChannel).getUnclippedBoundsInRoot().top,
            composeTestRule.onNodeWithText("action.close".i18n()).getUnclippedBoundsInRoot().top,
        )
        composeTestRule.onNodeWithText(openSupportChannel).performClick()

        verify(exactly = 1) { onOpenSupportChannel() }
    }

    @Test
    fun `error state hides the support channel when unavailable`() {
        renderPane(showSupportChannel = false)

        composeTestRule.onNodeWithText(openSupportChannel).assertDoesNotExist()
    }

    @Test
    fun `cancelled state does not show the support channel`() {
        every { presenter.interruptionInfoVisible } returns MutableStateFlow(true)
        every { presenter.interruptedTradeInfo } returns MutableStateFlow("cancelled")
        every { presenter.errorMessageVisible } returns MutableStateFlow(false)

        renderPane(showSupportChannel = true)

        composeTestRule.onNodeWithText(openSupportChannel).assertDoesNotExist()
    }
}
