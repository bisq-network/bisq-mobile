package network.bisq.mobile.presentation.settings.payment_accounts_musig.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.settings.payment_accounts_musig.model.CryptoAccountVO
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CryptoPaymentAccountCardUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

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
