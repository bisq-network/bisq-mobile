package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero

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
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.LocalIsTest
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.CryptoAccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero.MoneroFormPresenter
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero.MoneroPaymentAccountFormContent
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
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
class MoneroFormContentUiTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: MoneroFormPresenter

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

        presenter =
            MoneroFormPresenter(
                mainPresenter,
            )
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
        paymentMethod: CryptoPaymentMethodVO,
        onNavigateToNextScreen: (PaymentAccount) -> Unit = {},
    ) {
        composeTestRule.setContent {
            CompositionLocalProvider(LocalIsTest provides true) {
                BisqTheme {
                    MoneroPaymentAccountFormContent(
                        presenter = presenter,
                        paymentMethod = paymentMethod,
                        onNavigateToNextScreen = onNavigateToNextScreen,
                    )
                }
            }
        }
    }

    @Test
    fun `when sub addresses disabled then shows direct address and hides subaddress fields`() {
        // Given
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.address".i18n()).assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.mainAddresses".i18n())
            .assertCountEquals(0)
        composeTestRule
            .onAllNodesWithText("paymentAccounts.crypto.address.xmr.privateViewKey".i18n())
            .assertCountEquals(0)
    }

    @Test
    fun `when use sub addresses toggled on then shows subaddress fields and hides direct address`() {
        // Given
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .performClick()

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("paymentAccounts.crypto.address.address".i18n()).assertCountEquals(0)
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.xmr.mainAddresses".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.xmr.privateViewKey".i18n()).assertIsDisplayed()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.xmr.accountIndex".i18n()).assertIsDisplayed()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.initialSubAddressIndex".i18n())
            .assertIsDisplayed()
        composeTestRule
            .onAllNodesWithText("TODO: SubAddress creation not implemented yet", substring = true)
            .assertCountEquals(1)
    }

    @Test
    fun `when payment method does not support auto conf then auto conf section is hidden`() {
        // Given
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = false))

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onAllNodesWithText("paymentAccounts.crypto.address.autoConf.use".i18n()).assertCountEquals(0)
    }

    @Test
    fun `when auto conf enabled and supported then auto conf input fields are shown`() {
        // Given
        presenter.onAction(
            CryptoAccountFormUiAction.OnIsAutoConfChange(
                true,
            ),
        )
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))

        // Then
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n()).assertIsDisplayed()
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
    fun `when direct address field typed then presenter state updates`() {
        // Given
        val typedAddress = "48A_TYPED_ADDRESS"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.address.prompt".i18n())
            .performTextInput(typedAddress)

        // Then
        composeTestRule.waitForIdle()
        assertEquals(typedAddress, presenter.uiState.value.crypto.addressEntry.value)
    }

    @Test
    fun `when instant switch clicked then presenter state updates`() {
        // Given
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.isInstant".i18n()).performClick()

        // Then
        composeTestRule.waitForIdle()
        assertEquals(true, presenter.uiState.value.crypto.isInstant)
    }

    @Test
    fun `when use sub addresses switch clicked then presenter state updates`() {
        // Given
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .performClick()

        // Then
        composeTestRule.waitForIdle()
        assertEquals(true, presenter.uiState.value.useSubAddresses)
    }

    @Test
    fun `when main address field typed then presenter state updates`() {
        // Given
        val mainAddress = "48A_MAIN"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.mainAddresses.prompt".i18n())
            .performTextInput(mainAddress)

        // Then
        composeTestRule.waitForIdle()
        assertEquals(mainAddress, presenter.uiState.value.mainAddressEntry.value)
    }

    @Test
    fun `when private view key field typed then presenter state updates`() {
        // Given
        val privateViewKey = "abcdef0123456789"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.privateViewKey.prompt".i18n())
            .performTextInput(privateViewKey)

        // Then
        composeTestRule.waitForIdle()
        assertEquals(privateViewKey, presenter.uiState.value.privateViewKeyEntry.value)
    }

    @Test
    fun `when account index field typed then presenter state updates`() {
        // Given
        val accountIndex = "4"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.accountIndex.prompt".i18n())
            .performTextInput(accountIndex)

        // Then
        composeTestRule.waitForIdle()
        assertEquals(accountIndex, presenter.uiState.value.accountIndexEntry.value)
    }

    @Test
    fun `when initial sub address index field typed then presenter state updates`() {
        // Given
        val initialSubIndex = "2"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.useSubAddresses.switch".i18n())
            .performClick()
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.xmr.initialSubAddressIndex.prompt".i18n())
            .performTextInput(initialSubIndex)

        // Then
        composeTestRule.waitForIdle()
        assertEquals(initialSubIndex, presenter.uiState.value.initialSubAddressIndexEntry.value)
    }

    @Test
    fun `when auto conf switch clicked then presenter state updates`() {
        // Given
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()

        // When
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n()).performClick()

        // Then
        composeTestRule.waitForIdle()
        assertEquals(true, presenter.uiState.value.crypto.isAutoConf)
    }

    @Test
    fun `when auto conf num confirmations field typed then presenter state updates`() {
        // Given
        val numConfirmations = "3"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n()).performClick()
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.numConfirmations.prompt".i18n())
            .performTextInput(numConfirmations)

        // Then
        composeTestRule.waitForIdle()
        assertEquals(numConfirmations, presenter.uiState.value.crypto.autoConfNumConfirmationsEntry.value)
    }

    @Test
    fun `when auto conf max trade amount field typed then presenter state updates`() {
        // Given
        val maxTradeAmount = "1.5"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n()).performClick()
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.maxTradeAmount.prompt".i18n())
            .performTextInput(maxTradeAmount)

        // Then
        composeTestRule.waitForIdle()
        assertEquals(maxTradeAmount, presenter.uiState.value.crypto.autoConfMaxTradeAmountEntry.value)
    }

    @Test
    fun `when auto conf explorer urls field typed then presenter state updates`() {
        // Given
        val explorerUrls = "https://explorer.example"
        setTestContent(paymentMethod = samplePaymentMethod(supportAutoConf = true))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("paymentAccounts.crypto.address.autoConf.use".i18n()).performClick()
        composeTestRule.waitForIdle()

        // When
        composeTestRule
            .onNodeWithText("paymentAccounts.crypto.address.autoConf.explorerUrls.prompt".i18n())
            .performTextInput(explorerUrls)

        // Then
        composeTestRule.waitForIdle()
        assertEquals(explorerUrls, presenter.uiState.value.crypto.autoConfExplorerUrlsEntry.value)
    }

    @Test
    fun `when presenter emits next effect then navigates with account`() {
        // Given
        var navigatedAccount: PaymentAccount? = null
        setTestContent(
            paymentMethod = samplePaymentMethod(supportAutoConf = true),
            onNavigateToNextScreen = { navigatedAccount = it },
        )
        composeTestRule.waitForIdle()

        presenter.onAction(
            AccountFormUiAction.OnUniqueAccountNameChange(
                "Monero",
            ),
        )
        presenter.onAction(
            CryptoAccountFormUiAction.OnAddressChange(
                "48A_VALID_ADDRESS",
            ),
        )
        presenter.onAction(
            CryptoAccountFormUiAction.OnIsAutoConfChange(
                true,
            ),
        )
        presenter.onAction(
            CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange(
                "1",
            ),
        )
        presenter.onAction(
            CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange(
                "1",
            ),
        )

        // When
        composeTestRule.runOnIdle {
            presenter.onAction(AccountFormUiAction.OnNextClick)
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Then
        composeTestRule.waitUntil(timeoutMillis = 5_000) { navigatedAccount != null }
        assertNotNull(navigatedAccount)
        assertEquals("Monero", navigatedAccount.accountName)
    }

    private fun samplePaymentMethod(supportAutoConf: Boolean): CryptoPaymentMethodVO =
        CryptoPaymentMethodVO(
            paymentType = PaymentTypeVO.XMR,
            code = "XMR",
            name = "Monero",
            supportAutoConf = supportAutoConf,
        )
}
