package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.mockk
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.other_crypto.CreateOtherCryptoAssetAccount
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
class OtherCryptoFormContentUiTest : BisqComposeUiTestBase() {
    private lateinit var mainPresenter: MainPresenter

    override fun setUpUiTest() {
        super.setUpUiTest()
        mainPresenter = mockk(relaxed = true)
    }

    private fun setFormContent(
        presenter: OtherCryptoFormPresenter = OtherCryptoFormPresenter(mainPresenter),
        paymentMethod: CryptoPaymentMethod = samplePaymentMethod(supportAutoConf = false),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        setTestContent {
            OtherCryptoFormContent(
                presenter = presenter,
                paymentMethod = paymentMethod,
                onNavigateToNextScreen = onNavigateToNextScreen,
            )
        }
    }

    @Test
    fun `when rendered then address and instant controls are shown`() {
        setFormContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when payment method supports auto conf then auto conf switch is shown`() {
        setFormContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when payment method does not support auto conf then auto conf controls are hidden`() {
        setFormContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when address typed then visible input updates through presenter`() {
        val typedAddress = "0xFEEDBEEF"
        setFormContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address.prompt".i18n())
            .performTextInput(typedAddress)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(typedAddress).assertIsDisplayed()
    }

    @Test
    fun `when auto conf supported and clicked then auto conf fields are shown`() {
        setFormContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .performClick()

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
    fun `when auto conf field typed then visible input updates through presenter`() {
        val confirmations = "3"
        setFormContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.numConfirmations.prompt".i18n())
            .performTextInput(confirmations)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(confirmations).assertIsDisplayed()
    }

    @Test
    fun `when presenter emits navigate effect then navigation callback is invoked`() {
        val presenter = OtherCryptoFormPresenter(mainPresenter)
        val paymentMethod = samplePaymentMethod(supportAutoConf = false)
        var navigatedAccount: CreatePaymentAccount? = null

        setFormContent(
            presenter = presenter,
            paymentMethod = paymentMethod,
            onNavigateToNextScreen = { account -> navigatedAccount = account },
        )

        presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("ETH Account"))
        presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAddressChange("0xABC123"))
        presenter.onCommonAction(AccountFormUiAction.OnNextClick)
        composeTestRule.waitForIdle()

        val account = assertNotNull(navigatedAccount) as CreateOtherCryptoAssetAccount
        assertEquals("ETH Account", account.accountName)
        assertEquals("0xABC123", account.accountPayload.address)
        assertEquals("ETH", account.accountPayload.currencyCode)
    }

    private fun samplePaymentMethod(supportAutoConf: Boolean): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = "ETH",
            name = "Ethereum",
            supportAutoConf = supportAutoConf,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
        )
}
