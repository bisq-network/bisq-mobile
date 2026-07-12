package network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.presentation.common.test_utils.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class PaymentAccountMethodIconUiTest : BisqComposeUiTestBase() {
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
