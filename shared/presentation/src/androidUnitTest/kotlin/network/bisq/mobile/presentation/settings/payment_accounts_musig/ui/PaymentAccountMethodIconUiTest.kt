package network.bisq.mobile.presentation.settings.payment_accounts_musig.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PaymentAccountMethodIconUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private fun setTestContent(content: @Composable () -> Unit) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    content()
                }
            }
        }
    }

    @Test
    fun `when payment method has icon then image with content description is displayed`() {
        // Given
        val paymentMethod = PaymentTypeVO.WISE

        // When
        setTestContent {
            PaymentAccountTypeIcon(paymentType = paymentMethod)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithContentDescription("WISE")
            .assertIsDisplayed()
    }

    @Test
    fun `when payment method has no icon then fallback overlay letter is displayed`() {
        // Given
        val paymentMethod = PaymentTypeVO.CUSTOM

        // When
        setTestContent {
            PaymentAccountTypeIcon(paymentType = paymentMethod)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("C")
            .assertIsDisplayed()
    }

    @Test
    fun `when payment method has no icon then fallback is shown and no icon description is rendered`() {
        // Given
        val paymentMethod = PaymentTypeVO.CUSTOM

        // When
        setTestContent {
            PaymentAccountTypeIcon(paymentType = paymentMethod)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("C")
            .assertIsDisplayed()
    }
}
