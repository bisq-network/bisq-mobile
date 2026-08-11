package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config
import kotlin.test.assertEquals

@Config(application = TestApplication::class)
class CommonCryptoFormSectionUiTest : BisqComposeUiTestBase() {
    private fun setFormContent(
        cryptoUiState: CryptoAccountFormUiState,
        showAddress: Boolean,
        showAutoConf: Boolean,
        onAction: (CryptoAccountFormUiAction) -> Unit = {},
    ) {
        setTestContent {
            CommonCryptoFormSection(
                cryptoUiState = cryptoUiState,
                onAction = onAction,
                showAddress = showAddress,
                showAutoConf = showAutoConf,
            )
        }
    }

    @Test
    fun `when address enabled then address field is shown`() {
        setFormContent(
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
        setFormContent(
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
        setFormContent(
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
        setFormContent(
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
        var capturedAction: CryptoAccountFormUiAction? = null

        setFormContent(
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
        var capturedAction: CryptoAccountFormUiAction? = null

        setFormContent(
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
        var capturedAction: CryptoAccountFormUiAction? = null

        setFormContent(
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
