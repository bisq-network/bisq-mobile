package network.bisq.mobile.presentation.offerbook

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.MutableStateFlow
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class OfferbookScreenDeleteConfirmationUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setUp() {
        I18nSupport.setLanguage()
    }

    @Test
    fun `delete confirmation dialog reflects delete guard loading state`() {
        val presenter = mockk<OfferbookPresenter>(relaxed = true)
        every { presenter.showDeleteConfirmation } returns MutableStateFlow(true)
        every { presenter.isDeleteOfferEnabled } returns MutableStateFlow(false)
        every { presenter.isDemo() } returns false
        every { presenter.onConfirmedDeleteOffer() } returns Unit
        every { presenter.onDismissDeleteOffer() } returns Unit

        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    val showDeleteConfirmation by presenter.showDeleteConfirmation.collectAsState()
                    val isDeleteOfferEnabled by presenter.isDeleteOfferEnabled.collectAsState()
                    if (showDeleteConfirmation) {
                        network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ConfirmationDialog(
                            headline = "bisqEasy.offerbook.chatMessage.deleteOffer.confirmation".i18n(),
                            onConfirm = { presenter.onConfirmedDeleteOffer() },
                            onDismiss = { presenter.onDismissDeleteOffer() },
                            confirmButtonLoading = !isDeleteOfferEnabled,
                        )
                    }
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("bisqEasy.offerbook.chatMessage.deleteOffer.confirmation".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithContentDescription("dialog_confirm_yes")
            .assertIsNotEnabled()

        verify(exactly = 0) { presenter.onConfirmedDeleteOffer() }
    }
}
