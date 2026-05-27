package network.bisq.mobile.client.payment_accounts.step2_payment_account_form.form.crypto

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.CryptoAccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CommonCryptoFormSection
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CryptoAccountFormUiState
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
class CommonCryptoFormSectionUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(
        cryptoUiState: CryptoAccountFormUiState,
        showAddress: Boolean,
        showAutoConf: Boolean,
        onAction: (AccountFormUiAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    CommonCryptoFormSection(
                        cryptoUiState = cryptoUiState,
                        onAction = onAction,
                        showAddress = showAddress,
                        showAutoConf = showAutoConf,
                    )
                }
            }
        }
    }

    @Test
    fun `when address enabled then address field is shown`() {
        setTestContent(
            cryptoUiState = sampleCryptoUiState(),
            showAddress = true,
            showAutoConf = false,
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when address hidden then address field is not rendered`() {
        setTestContent(
            cryptoUiState = sampleCryptoUiState(),
            showAddress = false,
            showAutoConf = false,
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.address".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when auto conf enabled and state true then auto conf fields are shown`() {
        setTestContent(
            cryptoUiState = sampleCryptoUiState(isAutoConf = true),
            showAddress = true,
            showAutoConf = true,
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.maxTradeAmount".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.explorerUrls".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when auto conf hidden then auto conf controls are not rendered`() {
        setTestContent(
            cryptoUiState = sampleCryptoUiState(isAutoConf = true),
            showAddress = true,
            showAutoConf = false,
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.explorerUrls".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when address typed then emits address change action`() {
        val typedAddress = "0xABCDEF"
        var capturedAction: AccountFormUiAction? = null

        setTestContent(
            cryptoUiState = sampleCryptoUiState(),
            showAddress = true,
            showAutoConf = false,
            onAction = { action -> capturedAction = action },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address.prompt".i18n())
            .performTextInput(typedAddress)

        composeTestRule.waitForIdle()
        assertEquals(CryptoAccountFormUiAction.OnAddressChange(typedAddress), capturedAction)
    }

    @Test
    fun `when instant clicked then emits instant toggle action`() {
        var capturedAction: AccountFormUiAction? = null

        setTestContent(
            cryptoUiState = sampleCryptoUiState(isInstant = false),
            showAddress = true,
            showAutoConf = false,
            onAction = { action -> capturedAction = action },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n())
            .performClick()

        composeTestRule.waitForIdle()
        assertEquals(CryptoAccountFormUiAction.OnIsInstantChange(true), capturedAction)
    }

    @Test
    fun `when auto conf clicked then emits auto conf toggle action`() {
        var capturedAction: AccountFormUiAction? = null

        setTestContent(
            cryptoUiState = sampleCryptoUiState(isAutoConf = false),
            showAddress = true,
            showAutoConf = true,
            onAction = { action -> capturedAction = action },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .performClick()

        composeTestRule.waitForIdle()
        assertEquals(CryptoAccountFormUiAction.OnIsAutoConfChange(true), capturedAction)
    }

    private fun sampleCryptoUiState(
        isInstant: Boolean = false,
        isAutoConf: Boolean = false,
    ): CryptoAccountFormUiState =
        CryptoAccountFormUiState(
            addressEntry = DataEntry(value = ""),
            isInstant = isInstant,
            isAutoConf = isAutoConf,
            autoConfNumConfirmationsEntry = DataEntry(value = "2"),
            autoConfMaxTradeAmountEntry = DataEntry(value = "1"),
            autoConfExplorerUrlsEntry = DataEntry(value = "https://explorer.example.com"),
        )
}
