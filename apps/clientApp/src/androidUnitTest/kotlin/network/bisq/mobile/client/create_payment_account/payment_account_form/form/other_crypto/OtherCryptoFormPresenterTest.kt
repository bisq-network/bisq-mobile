package network.bisq.mobile.client.create_payment_account.payment_account_form.form.other_crypto

import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.client.create_payment_account.payment_account_form.form.action.CryptoAccountFormUiAction
import network.bisq.mobile.client.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.domain.model.account.crypto.CryptoPaymentMethod
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OtherCryptoFormPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: OtherCryptoFormPresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

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

        presenter = OtherCryptoFormPresenter(mainPresenter = mainPresenter)
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `when crypto actions are dispatched then crypto ui state updates`() =
        runTest(testDispatcher) {
            // When
            presenter.onAction(CryptoAccountFormUiAction.OnAddressChange("0xABCDEF"))
            presenter.onAction(CryptoAccountFormUiAction.OnIsInstantChange(true))
            presenter.onAction(CryptoAccountFormUiAction.OnIsAutoConfChange(true))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange("2"))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange("1"))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange("https://explorer.eth"))

            // Then
            val state = presenter.uiState.value.crypto
            assertEquals("0xABCDEF", state.addressEntry.value)
            assertTrue(state.isInstant)
            assertTrue(state.isAutoConf)
            assertEquals("2", state.autoConfNumConfirmationsEntry.value)
            assertEquals("1", state.autoConfMaxTradeAmountEntry.value)
            assertEquals("https://explorer.eth", state.autoConfExplorerUrlsEntry.value)
        }

    @Test
    fun `when next clicked before initialize then no effect is emitted`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("ETH Account"))
            presenter.onAction(CryptoAccountFormUiAction.OnAddressChange("0x123456"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
        }

    @Test
    fun `when next clicked with invalid entries then no navigation effect is emitted and errors are set`() =
        runTest(testDispatcher) {
            // Given
            presenter.initialize(samplePaymentMethod())
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("a"))
            presenter.onAction(CryptoAccountFormUiAction.OnAddressChange("   "))
            presenter.onAction(CryptoAccountFormUiAction.OnIsAutoConfChange(true))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange("0"))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange("0"))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange("x"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()

            val state = presenter.uiState.value
            assertTrue(presenter.uniqueAccountNameEntry.value.errorMessage != null)
            assertTrue(state.crypto.addressEntry.errorMessage != null)
            assertTrue(state.crypto.autoConfNumConfirmationsEntry.errorMessage != null)
            assertTrue(state.crypto.autoConfMaxTradeAmountEntry.errorMessage != null)
            assertTrue(state.crypto.autoConfExplorerUrlsEntry.errorMessage != null)
        }

    @Test
    fun `when next clicked with valid auto conf enabled entries then emits account payload`() =
        runTest(testDispatcher) {
            // Given
            presenter.initialize(samplePaymentMethod())
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("  ETH Account  "))
            presenter.onAction(CryptoAccountFormUiAction.OnAddressChange("  0xABC123  "))
            presenter.onAction(CryptoAccountFormUiAction.OnIsInstantChange(true))
            presenter.onAction(CryptoAccountFormUiAction.OnIsAutoConfChange(true))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange(" 2 "))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange(" 1 "))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange("  https://explorer.eth  "))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is OtherCryptoFormEffect.NavigateToNextScreen)

            val account = effect.account
            assertEquals("ETH Account", account.accountName)
            val payload = account.accountPayload
            assertEquals("0xABC123", payload.address)
            assertTrue(payload.isInstant)
            assertEquals(true, payload.isAutoConf)
            assertEquals(2, payload.autoConfNumConfirmations)
            assertEquals(1L, payload.autoConfMaxTradeAmount)
            assertEquals("https://explorer.eth", payload.autoConfExplorerUrls)
            assertEquals("ETH", payload.currencyCode)
        }

    @Test
    fun `when next clicked with auto conf disabled then auto conf payload fields are null`() =
        runTest(testDispatcher) {
            // Given
            presenter.initialize(samplePaymentMethod())
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("ETH No AutoConf"))
            presenter.onAction(CryptoAccountFormUiAction.OnAddressChange("0xNOAUTO"))
            presenter.onAction(CryptoAccountFormUiAction.OnIsAutoConfChange(false))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange("2"))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange("1"))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange("https://ignored.explorer"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is OtherCryptoFormEffect.NavigateToNextScreen)

            val payload = effect.account.accountPayload
            assertEquals(false, payload.isAutoConf)
            assertNull(payload.autoConfNumConfirmations)
            assertNull(payload.autoConfMaxTradeAmount)
            assertNull(payload.autoConfExplorerUrls)
        }

    private fun samplePaymentMethod(): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = "ETH",
            name = "Ethereum",
            supportAutoConf = true,
            tradeLimitInfo = "5000.00",
            tradeDuration = "4 days",
        )
}
