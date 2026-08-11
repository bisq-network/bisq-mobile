package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.mockk.mockk
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.CreateZelleAccount
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.presentation.compose.BisqComposeUiTestBase
import org.junit.Test
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Config(application = TestApplication::class)
class ZelleFormContentUiTest : BisqComposeUiTestBase() {
    private lateinit var mainPresenter: MainPresenter

    override fun setUpUiTest() {
        super.setUpUiTest()
        mainPresenter = mockk(relaxed = true)
    }

    private fun setFormContent(
        presenter: ZelleFormPresenter = ZelleFormPresenter(mainPresenter),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        setTestContent {
            CompositionLocalProvider(LocalExternalUrlOpener provides ExternalUrlOpener { true }) {
                ZelleFormContent(
                    presenter = presenter,
                    onNavigateToNextScreen = onNavigateToNextScreen,
                )
            }
        }
    }

    @Test
    fun `when rendered then zelle form fields and background dialog are shown`() {
        setFormContent()

        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.emailOrMobileNr".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.accountData.backgroundOverlay.headline".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when dismissing background dialog then it is hidden`() {
        setFormContent()

        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("action.iUnderstand".i18n())
            .assertCountEquals(0)
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when holder name field typed then visible input updates through presenter`() {
        val holderName = "Alice Doe"
        setFormContent()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.holderName".i18n().lowercase(),
                ),
            ).performTextInput(holderName)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(holderName).assertIsDisplayed()
    }

    @Test
    fun `when email mobile field typed then visible input updates through presenter`() {
        val emailOrMobile = "alice@example.com"
        setFormContent()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.emailOrMobileNr".i18n().lowercase(),
                ),
            ).performTextInput(emailOrMobile)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(emailOrMobile).assertIsDisplayed()
    }

    @Test
    fun `when presenter emits navigate effect then navigation callback receives account`() {
        val presenter = ZelleFormPresenter(mainPresenter)
        var navigatedAccount: CreatePaymentAccount? = null

        setFormContent(
            presenter = presenter,
            onNavigateToNextScreen = { account -> navigatedAccount = account },
        )

        presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Zelle Personal"))
        presenter.onAction(ZelleFormUiAction.OnHolderNameChange("John Doe"))
        presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("john@example.com"))
        presenter.onCommonAction(AccountFormUiAction.OnNextClick)
        composeTestRule.waitForIdle()

        val account = assertNotNull(navigatedAccount) as CreateZelleAccount
        assertEquals("Zelle Personal", account.accountName)
        assertEquals("John Doe", account.accountPayload.holderName)
        assertEquals("john@example.com", account.accountPayload.emailOrMobileNr)
    }
}
