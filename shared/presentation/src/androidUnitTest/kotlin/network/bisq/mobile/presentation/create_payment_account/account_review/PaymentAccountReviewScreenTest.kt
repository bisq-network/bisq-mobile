package network.bisq.mobile.presentation.create_payment_account.account_review

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.data.replicated.account.payment_method.FiatPaymentRail
import network.bisq.mobile.data.service.accounts.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentMethod
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateMoneroAccountPayload
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccountPayload
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccount
import network.bisq.mobile.domain.model.account.create.fiat.CreateZelleAccountPayload
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.Country
import network.bisq.mobile.domain.model.account.fiat.FiatCurrency
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethod
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
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
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountReviewScreenTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var paymentAccountsServiceFacade: PaymentAccountsServiceFacade
    private lateinit var globalUiManager: GlobalUiManager
    private lateinit var mainPresenter: MainPresenter
    private lateinit var koinApplication: KoinApplication
    private lateinit var viewModelStore: ViewModelStore
    private lateinit var viewModelStoreOwner: ViewModelStoreOwner

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()
        paymentAccountsServiceFacade = mockk(relaxed = true)
        globalUiManager = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)
        viewModelStore = ViewModelStore()
        viewModelStoreOwner =
            object : ViewModelStoreOwner {
                override val viewModelStore: ViewModelStore = this@PaymentAccountReviewScreenTest.viewModelStore
            }

        every { globalUiManager.scheduleShowLoading() } returns Unit
        every { globalUiManager.hideLoading() } returns Unit
        runCatching { stopKoin() }
        koinApplication =
            startKoin {
                modules(
                    module {
                        single<NavigationManager> { mockk(relaxed = true) }
                        factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
                        single<GlobalUiManager> { globalUiManager }
                        factory { PaymentAccountReviewPresenter(paymentAccountsServiceFacade, mainPresenter) }
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

    private fun setTestContent(
        createPaymentAccount: CreatePaymentAccount,
        paymentMethod: PaymentMethod,
        onCloseCreateAccountFlow: () -> Unit = {},
    ) {
        composeTestRule.setContent {
            KoinIsolatedContext(koinApplication) {
                CompositionLocalProvider(
                    LocalIsTest provides true,
                    LocalExternalUrlOpener provides ExternalUrlOpener { true },
                    LocalViewModelStoreOwner provides viewModelStoreOwner,
                ) {
                    BisqTheme {
                        PaymentAccountReviewScreen(
                            createPaymentAccount = createPaymentAccount,
                            paymentMethod = paymentMethod,
                            onCloseCreateAccountFlow = onCloseCreateAccountFlow,
                        )
                    }
                }
            }
        }
    }

    @Test
    fun `when zelle account rendered then shared and zelle specific fields are shown`() =
        runTest(testDispatcher) {
            setTestContent(
                createPaymentAccount = sampleCreateZelleAccount(),
                paymentMethod = sampleZellePaymentMethod(),
            )
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("mobile.user.paymentAccounts.review".i18n()).assertIsDisplayed()
            composeTestRule
                .onNodeWithText("paymentAccounts.summary.accountNameOverlay.accountName.description".i18n())
                .assertIsDisplayed()
            composeTestRule.onNodeWithText("paymentAccounts.country".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText("paymentAccounts.holderName".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText("paymentAccounts.emailOrMobileNr".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText("paymentAccounts.createAccount.createAccount".i18n()).assertIsDisplayed()
        }

    @Test
    fun `when monero account rendered then monero specific fields are shown`() =
        runTest(testDispatcher) {
            setTestContent(
                createPaymentAccount = sampleCreateMoneroAccount(),
                paymentMethod = sampleCryptoPaymentMethod(code = "XMR", name = "Monero"),
            )
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
            composeTestRule
                .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
                .assertIsDisplayed()
        }

    @Test
    fun `when other crypto account rendered then other crypto specific fields are shown`() =
        runTest(testDispatcher) {
            setTestContent(
                createPaymentAccount = sampleCreateOtherCryptoAssetAccount(),
                paymentMethod = sampleCryptoPaymentMethod(code = "ETH", name = "Ethereum"),
            )
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
            composeTestRule.onNodeWithText("ETH").assertIsDisplayed()
            composeTestRule.onNodeWithText("Ethereum").assertIsDisplayed()
        }

    @Test
    fun `when create account does not map to review account then unsupported state is shown`() =
        runTest(testDispatcher) {
            setTestContent(
                createPaymentAccount = sampleCreateZelleAccount(),
                paymentMethod = sampleCryptoPaymentMethod(code = "ETH", name = "Ethereum"),
            )
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("mobile.user.paymentAccounts.unsupported".i18n()).assertIsDisplayed()
            composeTestRule.onAllNodesWithText("paymentAccounts.country".i18n()).assertCountEquals(0)
        }

    @Test
    fun `when create account button clicked then account is added and close callback is invoked`() =
        runTest(testDispatcher) {
            val account = sampleCreateZelleAccount()
            var closeCallbackInvoked = false
            coEvery { paymentAccountsServiceFacade.addAccount(account) } returns Result.success(Unit)
            setTestContent(
                createPaymentAccount = account,
                paymentMethod = sampleZellePaymentMethod(),
                onCloseCreateAccountFlow = { closeCallbackInvoked = true },
            )
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            composeTestRule.onNodeWithText("paymentAccounts.createAccount.createAccount".i18n()).performClick()
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            coVerify(exactly = 1) { paymentAccountsServiceFacade.addAccount(account) }
            verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 1) { globalUiManager.hideLoading() }
            assertTrue(closeCallbackInvoked)
        }

    private fun sampleCreateZelleAccount(accountName: String = "Zelle Personal"): CreateZelleAccount =
        CreateZelleAccount(
            accountName = accountName,
            accountPayload =
                CreateZelleAccountPayload(
                    holderName = "Alice",
                    emailOrMobileNr = "alice@example.com",
                ),
        )

    private fun sampleCreateMoneroAccount(): CreateMoneroAccount =
        CreateMoneroAccount(
            accountName = "Monero Main",
            accountPayload =
                CreateMoneroAccountPayload(
                    address = "48A_MAIN_ADDRESS",
                    isInstant = false,
                    useSubAddresses = false,
                ),
        )

    private fun sampleCreateOtherCryptoAssetAccount(): CreateOtherCryptoAssetAccount =
        CreateOtherCryptoAssetAccount(
            accountName = "ETH Account",
            accountPayload =
                CreateOtherCryptoAssetAccountPayload(
                    currencyCode = "ETH",
                    address = "0xABC123",
                    isInstant = false,
                ),
        )

    private fun sampleZellePaymentMethod(): FiatPaymentMethod =
        FiatPaymentMethod(
            paymentRail = FiatPaymentRail.ZELLE,
            name = "Zelle",
            supportedCurrencies = listOf(FiatCurrency(code = "USD", name = "US Dollar")),
            supportedCountries = listOf(Country(code = "US", name = "United States")),
            matchesAllCountries = false,
            chargebackRisk = FiatPaymentMethodChargebackRisk.MODERATE,
            tradeLimitInfo = "5000.00 USD",
            tradeDuration = "1 day",
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
}
