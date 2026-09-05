package network.bisq.mobile.presentation.trade.trade_detail

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.mockk.mockk
import io.mockk.verify
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.test.presentation.compose.PresentationKoinComposeTestBase
import kotlin.test.Test

class MediationBannerUiTest : PresentationKoinComposeTestBase() {
    private val openSupportChannel get() = "mobile.community.support.openChannel".i18n()

    @Test
    fun `mediation banner shows and opens the support channel when available`() {
        val onOpenSupportChannel = mockk<() -> Unit>(relaxed = true)

        setTestContent {
            MediationBanner(
                showSupportChannel = true,
                onOpenSupportChannel = onOpenSupportChannel,
            )
        }

        composeTestRule.onNodeWithText(openSupportChannel).assertIsDisplayed()
        composeTestRule.onNodeWithText(openSupportChannel).performClick()

        verify(exactly = 1) { onOpenSupportChannel() }
    }

    @Test
    fun `mediation banner hides the support channel when unavailable`() {
        setTestContent {
            MediationBanner(
                showSupportChannel = false,
                onOpenSupportChannel = {},
            )
        }

        composeTestRule.onNodeWithText(openSupportChannel).assertDoesNotExist()
    }
}
