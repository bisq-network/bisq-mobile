package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.ZelleFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle.ZelleFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.zelle.ZellePaymentAccountFormContent
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class ZelleFormContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: ZelleFormPresenter

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
        mainPresenter = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                },
            )
        }

        presenter = ZelleFormPresenter(mainPresenter)
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
        onNavigateToNextScreen: (PaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    ZellePaymentAccountFormContent(
                        presenter = presenter,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                    )
                }
            }
        }
    }

    @Test
    fun `renders zelle form fields and background dialog by default`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.emailOrMobileNr".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.createAccount.accountData.backgroundOverlay.headline".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when dismissing background dialog then it is hidden`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText(
                (
                    "action." +
                        "iUnderstand"
                ).i18n(),
            ).assertCountEquals(0)
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when holder name field typed then presenter state updates`() {
        val holderName = "Alice Doe"
        setTestContent()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.holderName".i18n().lowercase(),
                ),
            ).performTextInput(holderName)

        composeTestRule.waitForIdle()
        assertEquals(holderName, presenter.uiState.value.holderNameEntry.value)
    }

    @Test
    fun `when email mobile field typed then presenter state updates`() {
        val emailOrMobile = "alice@example.com"
        setTestContent()
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.emailOrMobileNr".i18n().lowercase(),
                ),
            ).performTextInput(emailOrMobile)

        composeTestRule.waitForIdle()
        assertEquals(emailOrMobile, presenter.uiState.value.emailOrMobileNrEntry.value)
    }

    @Test
    fun `when presenter emits next effect then navigates with account`() {
        var navigatedAccount: PaymentAccount? = null
        setTestContent(onNavigateToNextScreen = { navigatedAccount = it })
        composeTestRule.onNodeWithText("action.iUnderstand".i18n()).performClick()

        composeTestRule.waitForIdle()
        presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Zelle Personal"))
        presenter.onAction(ZelleFormUiAction.OnHolderNameChange("John Doe"))
        presenter.onAction(ZelleFormUiAction.OnEmailOrMobileNrChange("john@example.com"))

        composeTestRule.runOnIdle {
            presenter.onAction(AccountFormUiAction.OnNextClick)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        composeTestRule.waitUntil(timeoutMillis = 5_000) { navigatedAccount != null }
        assertNotNull(navigatedAccount)
        assertEquals("Zelle Personal", navigatedAccount.accountName)
    }
}
