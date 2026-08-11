package network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.ui

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import network.bisq.mobile.client.common.presentation.model.account.PaymentTypeVO
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.presentation.payment_accounts_list.model.CryptoAccountVO
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
class CryptoPaymentAccountCardUiTest : BisqComposeUiTestBase() {
    @Test
    fun `when card renders then shows account name and currency name`() {
        // Given
        val account = sampleAccount()

        // When
        setTestContent {
            CryptoPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("Main Monero Wallet")
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("Monero")
            .assertIsDisplayed()
    }

    @Test
    fun `when card renders then shows crypto address text`() {
        // Given
        val account = sampleAccount(address = "44AFFq5kSiGBoZ")

        // When
        setTestContent {
            CryptoPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("44AFFq5kSiGBoZ")
            .assertIsDisplayed()
    }

    @Test
    fun `when payment method has no icon then fallback overlay letter is displayed`() {
        // Given
        val account = sampleAccount(paymentMethod = PaymentTypeVO.LTC)

        // When
        setTestContent {
            CryptoPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("L")
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithContentDescription("LTC")
            .assertCountEquals(0)
    }

    @Test
    fun `when crypto address is long then address text prefix is present`() {
        // Given
        val account =
            sampleAccount(
                address = "bc1qxy2kgdygjrsqtzq2n0yrf2493p83kkfjhx0wlh",
            )

        // When
        setTestContent {
            CryptoPaymentAccountCard(account = account)
        }

        // Then
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("bc1qxy2kgdy", substring = true)
            .assertIsDisplayed()
    }

    private fun sampleAccount(
        accountName: String = "Main Monero Wallet",
        currencyName: String = "Monero",
        address: String = "44AFFq5kSiGBoZ",
        paymentMethod: PaymentTypeVO = PaymentTypeVO.XMR,
    ): CryptoAccountVO =
        CryptoAccountVO(
            accountName = accountName,
            currencyName = currencyName,
            address = address,
            paymentType = paymentMethod,
        )
}
