package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.revolut

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
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.FiatPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.revolut.CreateRevolutAccount
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.action.RevolutFormUiAction
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
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
class RevolutFormContentTest {
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
        presenter: RevolutFormPresenter = RevolutFormPresenter(mainPresenter),
        paymentMethod: FiatPaymentMethod = samplePaymentMethod(),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    RevolutFormContent(
                        presenter = presenter,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                        paymentMethod = paymentMethod,
                    )
                }
            }
        }
    }

    @Test
    fun `when rendered then revolut form fields and currency summary are shown`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.userName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3)).assertIsDisplayed()
    }

    @Test
    fun `when username field typed then visible input updates through presenter`() {
        val userName = "satoshi"
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.userName".i18n().lowercase(),
                ),
            ).performTextInput(userName)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(userName).assertIsDisplayed()
    }

    @Test
    fun `when currency summary clicked then picker controls and currencies are shown`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.picker.selectAll".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.picker.clearAll".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.searchHint".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR (Euro)").assertIsDisplayed()
        composeTestRule.onNodeWithText("GBP (Pound Sterling)").assertIsDisplayed()
        composeTestRule.onNodeWithText("USD (US Dollar)").assertIsDisplayed()
    }

    @Test
    fun `when picker clear all clicked then currency summary updates through presenter`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.picker.clearAll".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("mobile.paymentAccounts.currencyPicker.summary".i18n(0, 3))
            .assertCountEquals(2)
    }

    @Test
    fun `when picker currency clicked then currency summary updates through presenter`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()
        composeTestRule.onNodeWithText("GBP (Pound Sterling)").performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("mobile.paymentAccounts.currencyPicker.summary".i18n(2, 3))
            .assertCountEquals(2)
    }

    @Test
    fun `when picker search typed then list filters through presenter`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.searchHint".i18n()).performTextInput("eur")

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("EUR (Euro)").assertIsDisplayed()
        composeTestRule.onAllNodesWithText("GBP (Pound Sterling)").assertCountEquals(0)
        composeTestRule.onAllNodesWithText("USD (US Dollar)").assertCountEquals(0)
    }

    @Test
    fun `when picker search has no results then empty state is shown`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performClick()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.searchHint".i18n()).performTextInput("xyz")

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.noResults".i18n()).assertIsDisplayed()
        composeTestRule.onAllNodesWithText("EUR (Euro)").assertCountEquals(0)
    }

    @Test
    fun `when presenter emits navigate effect then navigation callback receives account`() =
        runTest(testDispatcher) {
            val presenter = RevolutFormPresenter(mainPresenter)
            var navigatedAccount: CreatePaymentAccount? = null

            setTestContent(
                presenter = presenter,
                onNavigateToNextScreen = { account -> navigatedAccount = account },
            )
            composeTestRule.waitForIdle()

            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Revolut Personal"))
            presenter.onAction(RevolutFormUiAction.OnUserNameChange("satoshi"))
            presenter.onAction(RevolutFormUiAction.OnCurrencyToggle("GBP"))
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            val account = assertNotNull(navigatedAccount) as CreateRevolutAccount
            assertEquals("Revolut Personal", account.accountName)
            assertEquals("satoshi", account.accountPayload.userName)
            assertEquals(listOf("EUR", "USD"), account.accountPayload.selectedCurrencies.map { currency -> currency.code })
        }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.REVOLUT,
            name = "Revolut",
            supportedCurrencies =
                listOf(
                    FiatCurrency(code = "USD", name = "US Dollar"),
                    FiatCurrency(code = "EUR", name = "Euro"),
                    FiatCurrency(code = "GBP", name = "Pound Sterling"),
                ),
            supportedCountries = listOf(Country(code = "GB", name = "United Kingdom")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )
}
