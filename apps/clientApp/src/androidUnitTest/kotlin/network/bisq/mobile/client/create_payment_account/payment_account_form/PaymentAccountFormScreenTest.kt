package network.bisq.mobile.client.create_payment_account.payment_account_form

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.monero.MoneroFormPresenter
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.other_crypto.OtherCryptoFormPresenter
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.revolut.RevolutFormPresenter
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.zelle.ZelleFormPresenter
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.data.replicated.common.validation.PaymentAccountValidation
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
import org.koin.compose.KoinIsolatedContext
import org.koin.core.KoinApplication
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.annotation.Config

@Config(application = TestApplication::class)
@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountFormScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mainPresenter: MainPresenter
    private lateinit var koinApplication: KoinApplication
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModelStoreOwner: ViewModelStoreOwner
    private var paymentMethodState by mutableStateOf<PaymentMethod>(UnsupportedPaymentMethod())

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
        mainPresenter = mockk(relaxed = true)
        viewModelStore = ViewModelStore()
        viewModelStoreOwner =
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = this@PaymentAccountFormScreenTest.viewModelStore
            }
        paymentMethodState = UnsupportedPaymentMethod()

        runCatching { stopKoin() }
        koinApplication =
            startKoin {
                modules(
                    module {
                        single<NavigationManager> { mockk(relaxed = true) }
                        factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                        single<GlobalUiManager> { mockk(relaxed = true) }
                        factory { ZelleFormPresenter(mainPresenter) }
                        factory { RevolutFormPresenter(mainPresenter) }
                        factory { MoneroFormPresenter(mainPresenter) }
                        factory { OtherCryptoFormPresenter(mainPresenter) }
                    },
                )
            }
    }

    @After
    fun tearDown() {
        runCatching {
            composeTestRule.setContent {}
            composeTestRule.waitForIdle()
        }
        runCatching { viewModelStore.clear() }
        runCatching { stopKoin() }
        Dispatchers.resetMain()
    }

    private fun setTestContent() {
        composeTestRule.setContent {
            KoinIsolatedContext(koinApplication) {
                CompositionLocalProvider(
                    LocalIsTest provides true,
                    LocalExternalUrlOpener provides ExternalUrlOpener { true },
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                ) {
                    BisqTheme {
                        PaymentAccountFormScreen(paymentMethod = paymentMethodState)
                    }
                }
            }
        }
    }

    @Test
    fun `when unsupported payment method rendered then unsupported state is shown`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.unsupported".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when unsupported fiat payment method rendered then method name and unsupported state are shown`() {
        paymentMethodState = sampleFiatPaymentMethod(FiatPaymentRail.SEPA, "SEPA")
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("SEPA").assertIsDisplayed()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.unsupported".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when unsupported crypto payment method rendered then unsupported state is shown`() {
        paymentMethodState = sampleCryptoPaymentMethod(code = "UNKNOWN", name = "Unknown coin")
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.unsupported".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when monero payment method rendered then monero form content is shown`() {
        paymentMethodState = sampleCryptoPaymentMethod(code = "XMR", name = "Monero")
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("XMR").assertIsDisplayed()
        composeTestRule.onNodeWithText("Monero").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `when zelle payment method rendered then form shell and metadata are shown`() {
        paymentMethodState = sampleFiatPaymentMethod(FiatPaymentRail.ZELLE, "Zelle")
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("mobile.user.paymentAccounts.details".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.summary.accountNameOverlay.accountName.description".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.user.paymentAccounts.details.accountName.helper".i18n())
            .assertIsDisplayed()
        composeTestRule.onNodeWithText("Zelle").assertIsDisplayed()
        composeTestRule.onNodeWithText("action.next".i18n()).assertIsDisplayed().assertIsEnabled()
    }

    @Test
    fun `when revolut payment method rendered then revolut form content is shown`() {
        paymentMethodState = sampleFiatPaymentMethod(FiatPaymentRail.REVOLUT, "Revolut")
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Revolut").assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.userName".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("mobile.paymentAccounts.currencyPicker.allSelected".i18n(3))
            .performScrollTo()
            .assertIsDisplayed()
    }

    @Test
    fun `when crypto payment method rendered then shows code and name`() {
        paymentMethodState = sampleCryptoPaymentMethod(code = "ETH", name = "Ethereum")
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("ETH").assertIsDisplayed()
        composeTestRule.onNodeWithText("Ethereum").assertIsDisplayed()
    }

    @Test
    fun `when account name typed then visible input updates through presenter`() {
        val accountName = "Updated"
        paymentMethodState = sampleFiatPaymentMethod(FiatPaymentRail.ZELLE, "Zelle")
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "paymentAccounts.createAccount.prompt".i18n(
                    "paymentAccounts.summary.accountNameOverlay.accountName.description".i18n().lowercase(),
                ),
            ).performTextInput(accountName)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(accountName).assertIsDisplayed()
    }

    @Test
    fun `when next clicked with empty account name then account name error is shown`() {
        paymentMethodState = sampleFiatPaymentMethod(FiatPaymentRail.ZELLE, "Zelle")
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("action.next".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText(
                "validation.tooShortOrTooLong".i18n(
                    PaymentAccountValidation.ACCOUNT_NAME_MIN_LENGTH,
                    PaymentAccountValidation.ACCOUNT_NAME_MAX_LENGTH,
                ),
            ).assertIsDisplayed()
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
