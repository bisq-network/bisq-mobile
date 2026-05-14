package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.other_crypto

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
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.create.crypto.CreateOtherCryptoAssetAccount
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.CryptoAccountFormUiAction
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
class OtherCryptoFormContentTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mainPresenter: MainPresenter

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
        presenter: OtherCryptoFormPresenter = OtherCryptoFormPresenter(mainPresenter),
        paymentMethod: CryptoPaymentMethod = samplePaymentMethod(supportAutoConf = false),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    OtherCryptoFormContent(
                        presenter = presenter,
                        paymentMethod = paymentMethod,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                    )
                }
            }
        }
    }

    @Test
    fun `when rendered then address and instant controls are shown`() {
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when payment method supports auto conf then auto conf switch is shown`() {
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when payment method does not support auto conf then auto conf controls are hidden`() {
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when address typed then visible input updates through presenter`() {
        val typedAddress = "0xFEEDBEEF"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address.prompt".i18n())
            .performTextInput(typedAddress)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(typedAddress).assertIsDisplayed()
    }

    @Test
    fun `when auto conf supported and clicked then auto conf fields are shown`() {
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .performClick()

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.maxTradeAmount".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.explorerUrls".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when auto conf field typed then visible input updates through presenter`() {
        val confirmations = "3"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .performClick()
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.numConfirmations.prompt".i18n())
            .performTextInput(confirmations)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(confirmations).assertIsDisplayed()
    }

    @Test
    fun `when presenter emits navigate effect then navigation callback is invoked`() =
        runTest(testDispatcher) {
            val presenter = OtherCryptoFormPresenter(mainPresenter)
            val paymentMethod = samplePaymentMethod(supportAutoConf = false)
            var navigatedAccount: CreatePaymentAccount? = null

            setTestContent(
                presenter = presenter,
                paymentMethod = paymentMethod,
                onNavigateToNextScreen = { account -> navigatedAccount = account },
            )
            composeTestRule.waitForIdle()

            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("ETH Account"))
            presenter.onAction(CryptoAccountFormUiAction.OnAddressChange("0xABC123"))
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            composeTestRule.waitForIdle()
            val account = assertNotNull(navigatedAccount) as CreateOtherCryptoAssetAccount
            assertEquals("ETH Account", account.accountName)
            assertEquals("0xABC123", account.accountPayload.address)
            assertEquals("ETH", account.accountPayload.currencyCode)
        }

    private fun samplePaymentMethod(supportAutoConf: Boolean): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = "ETH",
            name = "Ethereum",
            supportAutoConf = supportAutoConf,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
        )
}
