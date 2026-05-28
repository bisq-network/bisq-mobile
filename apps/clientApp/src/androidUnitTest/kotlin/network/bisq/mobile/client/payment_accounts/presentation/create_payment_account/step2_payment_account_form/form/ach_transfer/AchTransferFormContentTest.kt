package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.ach_transfer

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.ach_transfer.CreateAchTransferAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.bank.BankAccountType
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.AchTransferFormUiAction
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class AchTransferFormContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mainPresenter: MainPresenter

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
        mainPresenter = mockk(relaxed = true)

        runCatching { stopKoin() }
        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun setTestContent(
        presenter: AchTransferFormPresenter = AchTransferFormPresenter(mainPresenter),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    AchTransferFormContent(
                        presenter = presenter,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                        modifier = Modifier.verticalScroll(rememberScrollState()),
                    )
                }
            }
        }
    }

    @Test
    fun `when rendered then ach transfer form fields are shown`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.holderAddress".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.bank.bankName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.achTransfer.routingNr".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.accountNr".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.accountData.bank.bankAccountType.prompt".i18n())
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when text fields typed then visible inputs update through presenter`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.holderName".i18n().lowercase()))
            .performTextInput("Alice Doe")
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.holderAddress".i18n().lowercase()))
            .performTextInput("123 Main St")
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.bank.bankName".i18n().lowercase()))
            .performTextInput("Bisq Bank")
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.achTransfer.routingNr".i18n().lowercase()))
            .performTextInput("021000021")
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.prompt".i18n("paymentAccounts.accountNr".i18n().lowercase()))
            .performTextInput("123456789")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("Alice Doe").assertIsDisplayed()
        composeTestRule.onNodeWithText("123 Main St").assertIsDisplayed()
        composeTestRule.onNodeWithText("Bisq Bank").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("021000021").assertCountEquals(1)
        composeTestRule.onAllNodesWithText("123456789").assertCountEquals(1)
    }

    @Test
    fun `when bank account type selected then dropdown value updates through presenter`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.accountData.bank.bankAccountType.prompt".i18n())
            .performScrollTo()
            .performClick()
        composeTestRule.onNodeWithText("paymentAccounts.bank.bankAccountType.CHECKINGS".i18n()).performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithText("paymentAccounts.bank.bankAccountType.CHECKINGS".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when next clicked without bank account type then validation error is shown`() =
        runTest(testDispatcher) {
            val presenter = AchTransferFormPresenter(mainPresenter)
            setTestContent(presenter = presenter)
            composeTestRule.waitForIdle()

            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            composeTestRule
                .onNodeWithText("paymentAccounts.createAccount.accountData.bank.bankAccountType.error.noneSelected".i18n())
                .performScrollTo()
                .assertIsDisplayed()
        }

    @Test
    fun `when presenter emits navigate effect then navigation callback receives account`() =
        runTest(testDispatcher) {
            val presenter = AchTransferFormPresenter(mainPresenter)
            var navigatedAccount: CreatePaymentAccount? = null

            setTestContent(
                presenter = presenter,
                onNavigateToNextScreen = { account -> navigatedAccount = account },
            )
            composeTestRule.waitForIdle()

            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("ACH Main"))
            presenter.onAction(AchTransferFormUiAction.OnHolderNameChange(" Alice Doe "))
            presenter.onAction(AchTransferFormUiAction.OnHolderAddressChange(" 123 Main St "))
            presenter.onAction(AchTransferFormUiAction.OnBankNameChange(" Bisq Bank "))
            presenter.onAction(AchTransferFormUiAction.OnRoutingNrChange(" 021000021 "))
            presenter.onAction(AchTransferFormUiAction.OnAccountNrChange(" 123456789 "))
            presenter.onAction(AchTransferFormUiAction.OnBankAccountTypeSelect(BankAccountType.CHECKING))
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()
            composeTestRule.waitForIdle()

            val account = assertNotNull(navigatedAccount) as CreateAchTransferAccount
            assertEquals("ACH Main", account.accountName)
            assertEquals("Alice Doe", account.accountPayload.holderName)
            assertEquals("123 Main St", account.accountPayload.holderAddress)
            assertEquals("Bisq Bank", account.accountPayload.bankName)
            assertEquals("021000021", account.accountPayload.routingNr)
            assertEquals("123456789", account.accountPayload.accountNr)
            assertEquals(BankAccountType.CHECKING, account.accountPayload.bankAccountType)
        }
}
