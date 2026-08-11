package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.monero

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.mockk
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccount
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CryptoAccountFormUiAction
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Config(application = TestApplication::class)
class MoneroFormContentUiTest : BisqComposeUiTestBase() {
    private lateinit var mainPresenter: MainPresenter

    override fun setUpUiTest() {
        super.setUpUiTest()
        mainPresenter = mockk(relaxed = true)
    }

    private fun setFormContent(
        presenter: MoneroFormPresenter = MoneroFormPresenter(mainPresenter),
        paymentMethod: CryptoPaymentMethod = samplePaymentMethod(supportAutoConf = true),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        setTestContent {
            MoneroFormContent(
                presenter = presenter,
                paymentMethod = paymentMethod,
                onNavigateToNextScreen = onNavigateToNextScreen,
            )
        }
    }

    @Test
    fun `when rendered then direct address and instant controls are shown`() {
        setFormContent()

        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when sub address feature is gated off then switch and subaddress fields are hidden`() {
        setFormContent()

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
        setFormContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when auto conf feature is gated off then auto conf interactions are unavailable`() {
        setFormContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

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

    @Test
    fun `when direct address field typed then visible input updates through presenter`() {
        val typedAddress = "48A_TYPED_ADDRESS"
        setFormContent()

        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address.prompt".i18n())
            .performTextInput(typedAddress)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(typedAddress).assertIsDisplayed()
    }

    @Test
    fun `when instant switch clicked then direct address remains visible`() {
        setFormContent()

        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when presenter emits navigate effect then navigation callback receives account`() {
        val presenter = MoneroFormPresenter(mainPresenter)
        var navigatedAccount: CreatePaymentAccount? = null

        setFormContent(
            presenter = presenter,
            paymentMethod = samplePaymentMethod(supportAutoConf = false),
            onNavigateToNextScreen = { account -> navigatedAccount = account },
        )

        presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Monero Account"))
        presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAddressChange("48A_DIRECT_ADDRESS"))
        presenter.onCommonAction(AccountFormUiAction.OnNextClick)
        composeTestRule.waitForIdle()

        val account = assertNotNull(navigatedAccount) as CreateMoneroAccount
        assertEquals("Monero Account", account.accountName)
        assertEquals("48A_DIRECT_ADDRESS", account.accountPayload.address)
        assertEquals(false, account.accountPayload.useSubAddresses)
    }

    private fun samplePaymentMethod(supportAutoConf: Boolean): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = "XMR",
            name = "Monero",
            supportAutoConf = supportAutoConf,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
        )
}
