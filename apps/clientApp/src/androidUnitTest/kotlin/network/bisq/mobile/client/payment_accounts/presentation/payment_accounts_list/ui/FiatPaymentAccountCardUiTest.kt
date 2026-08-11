package network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import network.bisq.mobile.client.common.presentation.model.account.FiatPaymentMethodChargebackRiskVO
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.model.FiatAccountVO
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class FiatPaymentAccountCardUiTest : BisqComposeUiTestBase() {
    @Test
    fun `when card renders then shows account name and payment method name`() {
        // Given
        val account = sampleAccount()

        // When
        setTestContent {
            FiatPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("My SEPA Account")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Sepa")
            .assertIsDisplayed()
    }

    @Test
    fun `when card has currency then shows currency text`() {
        // Given
        val account = sampleAccount(currency = "USD (US Dollar)")

        // When
        setTestContent {
            FiatPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("USD (US Dollar)")
            .assertIsDisplayed()
    }

    @Test
    fun `when card has country then shows country next to payment method`() {
        // Given
        val account = sampleAccount(country = "United States")

        // When
        setTestContent {
            FiatPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("United States", substring = true)
            .assertCountEquals(1)
    }

    @Test
    fun `when card has empty currency then does not show currency text`() {
        // Given
        val account = sampleAccount(currency = "")

        // When
        setTestContent {
            FiatPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("USD (US Dollar)")
            .assertCountEquals(0)
    }

    @Test
    fun `when card has chargeback risk then shows localized risk badge text`() {
        // Given
        val account = sampleAccount(chargebackRisk = FiatPaymentMethodChargebackRiskVO.LOW)

        // When
        setTestContent {
            FiatPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Low")
            .assertIsDisplayed()
    }

    @Test
    fun `when card has different chargeback risk then low risk badge text is not shown`() {
        // Given
        val account = sampleAccount(chargebackRisk = FiatPaymentMethodChargebackRiskVO.VERY_LOW)

        // When
        setTestContent {
            FiatPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("Low")
            .assertCountEquals(0)
        composeTestRule
            .onNodeWithText("Very low")
            .assertIsDisplayed()
    }

    private fun sampleAccount(
        accountName: String = "My SEPA Account",
        chargebackRisk: FiatPaymentMethodChargebackRiskVO = FiatPaymentMethodChargebackRiskVO.LOW,
        paymentMethod: PaymentTypeVO = PaymentTypeVO.SEPA,
        paymentMethodName: String = "Sepa",
        country: String = "United States",
        currency: String = "USD (US Dollar)",
    ): FiatAccountVO =
        FiatAccountVO(
            accountName = accountName,
            chargebackRisk = chargebackRisk,
            paymentType = paymentMethod,
            paymentMethodName = paymentMethodName,
            country = country,
            currency = currency,
        )
}
