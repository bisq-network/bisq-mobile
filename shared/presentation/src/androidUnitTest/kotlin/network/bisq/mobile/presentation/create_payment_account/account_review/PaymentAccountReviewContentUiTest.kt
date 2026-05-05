package network.bisq.mobile.presentation.create_payment_account.account_review

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.PaymentAccountPayload
import network.bisq.mobile.domain.model.account.crypto.MoneroAccount
import network.bisq.mobile.domain.model.account.crypto.MoneroAccountPayload
import network.bisq.mobile.domain.model.account.fiat.ZelleAccount
import network.bisq.mobile.domain.model.account.fiat.ZelleAccountPayload
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class PaymentAccountReviewContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(
        paymentAccount: PaymentAccount,
        onCreateAccountClick: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    PaymentAccountReviewContent(
                        paymentAccount = paymentAccount,
                        onCreateAccountClick = onCreateAccountClick,
                    )
                }
            }
        }
    }

    @Test
    fun `when review content renders then shows shared title account name and create button`() {
        setTestContent(
            paymentAccount = sampleZelleAccount(),
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.review".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.summary.accountNameOverlay.accountName.description".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.createAccount".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when zelle account and fiat method provided then zelle specific fields render`() {
        setTestContent(
            paymentAccount = sampleZelleAccount(),
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.country".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.emailOrMobileNr".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when monero account and crypto method provided then monero specific fields render`() {
        setTestContent(
            paymentAccount = sampleMoneroAccount(),
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when unsupported account type provided then branch specific fields are not rendered`() {
        setTestContent(
            paymentAccount = sampleUnsupportedAccount(),
        )

        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("paymentAccounts.country".i18n()).assertCountEquals(0)
        composeTestRule.onAllNodesWithText("paymentAccounts.crypto.address.address".i18n()).assertCountEquals(0)
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.createAccount".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when create account button clicked then onCreateAccountClick callback is invoked`() {
        var clicked = false

        setTestContent(
            paymentAccount = sampleZelleAccount(),
            onCreateAccountClick = { clicked = true },
        )

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.createAccount.createAccount".i18n()).performClick()

        assertTrue(clicked)
    }

    private fun sampleZelleAccount(): ZelleAccount =
        ZelleAccount(
            accountName = "Alice Doe",
            accountPayload =
                ZelleAccountPayload(
                    holderName = "Alice Doe",
                    emailOrMobileNr = "alice@example.com",
                    country = "United States",
                    currency = "USD",
                    paymentMethodName = "Zelle",
                ),
        )

    private fun sampleUnsupportedAccount(): PaymentAccount =
        object : PaymentAccount {
            override val accountName: String = "Unsupported"
            override val accountPayload: PaymentAccountPayload = object : PaymentAccountPayload {}
            override val creationDate: String? = null
            override val tradeLimitInfo: String? = null
            override val tradeDuration: String? = null
        }

    private fun sampleMoneroAccount(): MoneroAccount =
        MoneroAccount(
            accountName = "Monero Main",
            accountPayload =
                MoneroAccountPayload(
                    address = "48A_MAIN_ADDRESS",
                    isInstant = false,
                    useSubAddresses = false,
                    currencyName = "Monero",
                    currencyCode = "XMR",
                    supportAutoConf = true,
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )
}
