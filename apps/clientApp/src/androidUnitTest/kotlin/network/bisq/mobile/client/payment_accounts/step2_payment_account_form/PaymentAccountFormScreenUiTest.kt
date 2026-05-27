package network.bisq.mobile.client.payment_accounts.step2_payment_account_form

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.PaymentAccountFormScreen
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.monero.MoneroFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto.OtherCryptoFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.revolut.RevolutFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.wise.WiseFormPresenter
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.zelle.ZelleFormPresenter
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.context.ExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
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

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountFormScreenUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mainPresenter: MainPresenter
    private var paymentMethodState by mutableStateOf<PaymentMethod>(UnsupportedPaymentMethod())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
        mainPresenter = mockk(relaxed = true)
        paymentMethodState = UnsupportedPaymentMethod()

        runCatching { stopKoin() }
        startKoin {
            modules(
                module {
                    single<NavigationManager> { mockk(relaxed = true) }
                    factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                    factory { ZelleFormPresenter(mainPresenter) }
                    factory { WiseFormPresenter(mainPresenter) }
                    factory { RevolutFormPresenter(mainPresenter) }
                    factory { MoneroFormPresenter(mainPresenter) }
                    factory { OtherCryptoFormPresenter(mainPresenter) }
                },
            )
        }
    }

    @After
    fun tearDown() {
        runCatching { composeTestRule.setContent {} }
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun setTestContent() {
        composeTestRule.setContent {
            CompositionLocalProvider(
                LocalIsTest provides true,
                LocalExternalUrlOpener provides ExternalUrlOpener { true },
            ) {
                BisqTheme {
                    PaymentAccountFormScreen(paymentMethod = paymentMethodState)
                }
            }
        }
    }

    @Test
    fun `when payment method changes then screen renders supported and unsupported branches`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.unsupported".i18n()).assertIsDisplayed()

        paymentMethodState = sampleFiatPaymentMethod(FiatPaymentRail.ZELLE, "Zelle")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Zelle").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.emailOrMobileNr".i18n()).performScrollTo().assertIsDisplayed()

        paymentMethodState = sampleFiatPaymentMethod(FiatPaymentRail.WISE, "Wise")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Wise").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.email".i18n()).performScrollTo().assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performScrollTo()
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performScrollTo()
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.title".i18n()).assertIsDisplayed()

        paymentMethodState = sampleFiatPaymentMethod(FiatPaymentRail.REVOLUT, "Revolut")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Revolut").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.userName".i18n()).performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3)).performScrollTo().assertIsDisplayed()

        paymentMethodState = sampleFiatPaymentMethod(FiatPaymentRail.SEPA, "SEPA")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SEPA").assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.unsupported".i18n()).assertIsDisplayed()

        paymentMethodState = sampleCryptoPaymentMethod(code = "XMR", name = "Monero")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("XMR").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monero").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).performScrollTo().assertIsDisplayed()

        paymentMethodState = sampleCryptoPaymentMethod(code = "ETH", name = "Ethereum")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ETH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ethereum").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).performScrollTo().assertIsDisplayed()

        paymentMethodState = sampleCryptoPaymentMethod(code = "UNKNOWN", name = "Unknown coin")
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.unsupported".i18n()).assertIsDisplayed()
    }

    private fun sampleFiatPaymentMethod(
        rail: FiatPaymentRail,
        name: String,
    ): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = rail,
            name = name,
            supportedCurrencies =
                listOf(
                    FiatCurrency(code = "USD", name = "US Dollar"),
                    FiatCurrency(code = "EUR", name = "Euro"),
                    FiatCurrency(code = "GBP", name = "Pound Sterling"),
                ),
            supportedCountries = listOf(Country(code = "US", name = "United States")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )

    private fun sampleCryptoPaymentMethod(
        code: String,
        name: String,
    ): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = code,
            name = name,
            supportAutoConf = false,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
        )

    private class UnsupportedPaymentMethod : PaymentMethod {
        override val name: String = "Unsupported"
        override val tradeLimitInfo: String = EMPTY_STRING
        override val tradeDuration: String = EMPTY_STRING
    }
}
