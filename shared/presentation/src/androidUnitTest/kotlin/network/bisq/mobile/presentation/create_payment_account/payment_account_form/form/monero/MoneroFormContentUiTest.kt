package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.CryptoAccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.CryptoAccountFormUiState
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class MoneroFormContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Before
    fun setup() {
        I18nSupport.setLanguage()
    }

    private fun setTestContent(
        uiState: MoneroFormUiState = sampleUiState(),
        paymentMethod: CryptoPaymentMethodVO = samplePaymentMethod(supportAutoConf = true),
        onAction: (AccountFormUiAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    MoneroFormContent(
                        uiState = uiState,
                        paymentMethod = paymentMethod,
                        onAction = onAction,
                    )
                }
            }
        }
    }

    @Test
    fun `when sub addresses disabled then shows direct address and hides subaddress fields`() {
        // Given
        setTestContent()

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.mainAddresses".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.privateViewKey".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when sub address feature is gated off then switch and subaddress fields are hidden`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.mainAddresses".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.privateViewKey".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.accountIndex".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.initialSubAddressIndex".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when payment method does not support auto conf then auto conf section is hidden`() {
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when direct address field typed then emits address change action`() {
        // Given
        val typedAddress = "48A_TYPED_ADDRESS"
        var capturedAction: AccountFormUiAction? = null
        setTestContent(
            onAction = { action -> capturedAction = action },
        )
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address.prompt".i18n())
            .performTextInput(typedAddress)

        // Then
        composeTestRule.waitForIdle()
        assertEquals(CryptoAccountFormUiAction.OnAddressChange(typedAddress), capturedAction)
    }

    @Test
    fun `when instant switch clicked then emits is instant change action`() {
        // Given
        var capturedAction: AccountFormUiAction? = null
        setTestContent(
            onAction = { action -> capturedAction = action },
        )
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n()).performClick()

        // Then
        composeTestRule.waitForIdle()
        assertEquals(CryptoAccountFormUiAction.OnIsInstantChange(true), capturedAction)
    }

    @Test
    fun `when sub address feature is gated off then sub-address interactions are unavailable`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.mainAddresses.prompt".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.privateViewKey.prompt".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.accountIndex.prompt".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.initialSubAddressIndex.prompt".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when auto conf feature is gated off then auto conf interactions are unavailable`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.numConfirmations.prompt".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.maxTradeAmount.prompt".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.explorerUrls.prompt".i18n())
            .assertCountEquals(0)
    }

    private fun samplePaymentMethod(supportAutoConf: Boolean): CryptoPaymentMethodVO =
        CryptoPaymentMethodVO(
            paymentType = PaymentTypeVO.XMR,
            code = "XMR",
            name = "Monero",
            supportAutoConf = supportAutoConf,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
        )

    private fun sampleUiState(): MoneroFormUiState =
        MoneroFormUiState(
            crypto =
                CryptoAccountFormUiState(
                    addressEntry = DataEntry(value = EMPTY_STRING),
                    isInstant = false,
                    isAutoConf = false,
                ),
            useSubAddresses = false,
        )
}
