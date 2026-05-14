package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.wise

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
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

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WisePaymentAccountFormContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: WiseFormPresenter

    private val samplePaymentMethod: FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.WISE,
            name = "Wise",
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

        presenter = WiseFormPresenter(mainPresenter)
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
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    WisePaymentAccountFormContent(
                        presenter = presenter,
                        paymentMethod = samplePaymentMethod,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                    )
                }
            }
        }
    }

    @Test
    fun `when wrapper composed then renders initialized currency summary`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.currencies.allSelected".i18n(3)).assertIsDisplayed()
    }

    @Test
    fun `when initialized summary clicked then renders picker currencies`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.currencies.allSelected".i18n(3)).performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.wise.picker.title".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("USD (US Dollar)").assertIsDisplayed()
        composeTestRule.onNodeWithText("EUR (Euro)").assertIsDisplayed()
        composeTestRule.onNodeWithText("GBP (Pound Sterling)").assertIsDisplayed()
    }
}
