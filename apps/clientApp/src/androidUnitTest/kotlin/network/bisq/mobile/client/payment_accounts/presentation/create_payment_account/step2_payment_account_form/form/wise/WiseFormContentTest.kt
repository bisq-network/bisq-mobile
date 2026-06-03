package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise

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
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.wise.CreateWiseAccount
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
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
class WiseFormContentTest {
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
        presenter: WiseFormPresenter = WiseFormPresenter(mainPresenter),
        paymentMethod: FiatPaymentMethod = samplePaymentMethod(),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    WiseFormContent(
                        presenter = presenter,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                        paymentMethod = paymentMethod,
                    )
                }
            }
        }
    }

    @Test
    fun `when rendered then wise form fields and currency summary are shown`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.email".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3)).assertIsDisplayed()
    }

    @Test
    fun `when holder name field typed then visible input updates through presenter`() {
        val holderName = "Alice Doe"
        setTestContent()

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
    fun `when email field typed then visible input updates through presenter`() {
        val email = "alice@example.com"
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.email".i18n().lowercase(),
                ),
            ).performTextInput(email)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(email).assertIsDisplayed()
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
        composeTestRule.onNodeWithText("GBP (British Pound)").assertIsDisplayed()
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
        composeTestRule.onNodeWithText("GBP (British Pound)").performClick()

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
        composeTestRule.onAllNodesWithText("GBP (British Pound)").assertCountEquals(0)
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
            val presenter = WiseFormPresenter(mainPresenter)
            var navigatedAccount: CreatePaymentAccount? = null

            setTestContent(
                presenter = presenter,
                onNavigateToNextScreen = { account -> navigatedAccount = account },
            )
            composeTestRule.waitForIdle()

            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Wise Personal"))
            presenter.onAction(WiseFormUiAction.OnHolderNameChange("John Doe"))
            presenter.onAction(WiseFormUiAction.OnEmailChange("john@example.com"))
            presenter.onAction(WiseFormUiAction.OnCurrencyToggle("GBP"))
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            val account = assertNotNull(navigatedAccount) as CreateWiseAccount
            assertEquals("Wise Personal", account.accountName)
            assertEquals("John Doe", account.accountPayload.holderName)
            assertEquals("john@example.com", account.accountPayload.email)
            assertEquals(listOf("EUR", "USD"), account.accountPayload.selectedCurrencies.map { currency -> currency.code })
        }

    private fun samplePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.WISE,
            name = "Wise",
            supportedCurrencies =
                listOf(
                    FiatCurrency(code = "USD", name = "US Dollar"),
                    FiatCurrency(code = "EUR", name = "Euro"),
                    FiatCurrency(code = "GBP", name = "British Pound"),
                ),
            supportedCountries = listOf(Country(code = "US", name = "United States")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )
}
