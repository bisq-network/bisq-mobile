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
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.model.account.create.CreatePaymentAccount
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.DataEntry
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.CryptoAccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.crypto.CryptoAccountFormUiState
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

@RunWith(AndroidJUnit4::class)
@OptIn(ExperimentalCoroutinesApi::class)
class OtherCryptoFormContentUiTest {
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
        uiState: OtherCryptoFormUiState,
        paymentMethod: CryptoPaymentMethod,
        onAction: (AccountFormUiAction) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    OtherCryptoFormContent(
                        uiState = uiState,
                        paymentMethod = paymentMethod,
                        onAction = onAction,
                    )
                }
            }
        }
    }

    private fun setWrapperTestContent(
        presenter: OtherCryptoFormPresenter = OtherCryptoFormPresenter(mainPresenter),
        paymentMethod: CryptoPaymentMethod = samplePaymentMethod(supportAutoConf = false),
        onNavigateToNextScreen: (CreatePaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    OtherCryptoPaymentAccountFormContent(
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
        setTestContent(
            uiState = sampleUiState(),
            paymentMethod = samplePaymentMethod(supportAutoConf = false),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when wrapper rendered then delegates to other crypto form content`() {
        setWrapperTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when wrapper payment method supports auto conf then auto conf switch is shown`() {
        setWrapperTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n()).assertIsDisplayed()
    }

    @Test
    fun `when payment method supports auto conf then auto conf switch is shown`() {
        setTestContent(
            uiState = sampleUiState(),
            paymentMethod = samplePaymentMethod(supportAutoConf = true),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertIsDisplayed()
    }

    @Test
    fun `when payment method does not support auto conf then auto conf controls are hidden`() {
        setTestContent(
            uiState = sampleUiState(),
            paymentMethod = samplePaymentMethod(supportAutoConf = false),
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.autoConf.numConfirmations".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when address typed then emits address change action`() {
        val typedAddress = "0xFEEDBEEF"
        var capturedAction: AccountFormUiAction? = null

        setTestContent(
            uiState = sampleUiState(),
            paymentMethod = samplePaymentMethod(supportAutoConf = false),
            onAction = { action -> capturedAction = action },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address.prompt".i18n())
            .performTextInput(typedAddress)

        composeTestRule.waitForIdle()
        assertEquals(CryptoAccountFormUiAction.OnAddressChange(typedAddress), capturedAction)
    }

    @Test
    fun `when wrapper address typed then visible input updates through presenter`() {
        val typedAddress = "0xFEEDBEEF"
        setWrapperTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address.prompt".i18n())
            .performTextInput(typedAddress)

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText(typedAddress).assertIsDisplayed()
    }

    @Test
    fun `when instant clicked then emits instant change action`() {
        var capturedAction: AccountFormUiAction? = null

        setTestContent(
            uiState = sampleUiState(isInstant = false),
            paymentMethod = samplePaymentMethod(supportAutoConf = false),
            onAction = { action -> capturedAction = action },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n())
            .performClick()

        composeTestRule.waitForIdle()
        assertEquals(CryptoAccountFormUiAction.OnIsInstantChange(true), capturedAction)
    }

    @Test
    fun `when auto conf supported and clicked then emits auto conf change action`() {
        var capturedAction: AccountFormUiAction? = null

        setTestContent(
            uiState = sampleUiState(isAutoConf = false),
            paymentMethod = samplePaymentMethod(supportAutoConf = true),
            onAction = { action -> capturedAction = action },
        )

        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n())
            .performClick()

        composeTestRule.waitForIdle()
        assertEquals(CryptoAccountFormUiAction.OnIsAutoConfChange(true), capturedAction)
    }

    private fun sampleUiState(
        isInstant: Boolean = false,
        isAutoConf: Boolean = false,
    ): OtherCryptoFormUiState =
        OtherCryptoFormUiState(
            crypto =
                CryptoAccountFormUiState(
                    addressEntry = DataEntry(value = ""),
                    isInstant = isInstant,
                    isAutoConf = isAutoConf,
                    autoConfNumConfirmationsEntry = DataEntry(value = "2"),
                    autoConfMaxTradeAmountEntry = DataEntry(value = "1"),
                    autoConfExplorerUrlsEntry = DataEntry(value = "https://explorer.example.com"),
                ),
        )

    private fun samplePaymentMethod(supportAutoConf: Boolean): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = "ETH",
            name = "Ethereum",
            supportAutoConf = supportAutoConf,
            tradeLimitInfo = EMPTY_STRING,
            tradeDuration = EMPTY_STRING,
        )
}
