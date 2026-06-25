package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.monero

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
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.monero.CreateMoneroAccount
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CryptoAccountFormUiAction
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
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
class MoneroFormContentTest {
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
        presenter: MoneroFormPresenter = MoneroFormPresenter(mainPresenter),
        paymentMethod: CryptoPaymentMethod = samplePaymentMethod(supportAutoConf = true),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    MoneroFormContent(
                        presenter = presenter,
                        paymentMethod = paymentMethod,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                    )
                }
            }
        }
    }

    @Test
    fun `when rendered then direct address and instant controls are shown`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when sub address feature is gated off then switch and subaddress fields are hidden`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.mainAddresses".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.privateViewKey".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.accountIndex".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.initialSubAddressIndex".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when payment method does not support auto conf then auto conf section is hidden`() {
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when auto conf feature is gated off then auto conf interactions are unavailable`() {
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.numConfirmations.prompt".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.maxTradeAmount.prompt".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.explorerUrls.prompt".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when direct address field typed then visible input updates through presenter`() {
        val typedAddress = "48A_TYPED_ADDRESS"
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address.prompt".i18n())
            .performTextInput(typedAddress)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(typedAddress).assertIsDisplayed()
    }

    @Test
    fun `when instant switch clicked then direct address remains visible`() {
        setTestContent()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n()).performClick()

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when presenter emits navigate effect then navigation callback receives account`() =
        runTest(testDispatcher) {
            val presenter = MoneroFormPresenter(mainPresenter)
            var navigatedAccount: CreatePaymentAccount? = null

            setTestContent(
                presenter = presenter,
                paymentMethod = samplePaymentMethod(supportAutoConf = false),
                onNavigateToNextScreen = { account -> navigatedAccount = account },
            )
            composeTestRule.waitForIdle()

            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("Monero Account"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAddressChange("48A_DIRECT_ADDRESS"))
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            val account = assertNotNull(navigatedAccount) as CreateMoneroAccount
            assertEquals("Monero Account", account.accountName)
            assertEquals("48A_DIRECT_ADDRESS", account.accountPayload.address)
            assertEquals(false, account.accountPayload.useSubAddresses)
        }

    private fun samplePaymentMethod(supportAutoConf: Boolean): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = "XMR",
            name = "Monero",
            supportAutoConf = supportAutoConf,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
        )
}
