package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.other_crypto

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.AccountFormUiAction
import network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step2_payment_account_form.form.crypto.CryptoAccountFormUiAction
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class OtherCryptoFormPresenterTest : ClientKoinIntegrationTestBase() {
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private lateinit var presenter: OtherCryptoFormPresenter

    override fun onSetup() {
        presenter = OtherCryptoFormPresenter(mainPresenter = mainPresenter)
    }

    @Test
    fun `when crypto actions are dispatched then crypto ui state updates`() =
        runTest {
            // When
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAddressChange("0xABCDEF"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnIsInstantChange(true))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnIsAutoConfChange(true))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange("2"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange("1"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange("https://explorer.eth"))

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
        runTest {
            // Given
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("ETH Account"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAddressChange("0x123456"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()
        }

    @Test
    fun `when next clicked with invalid entries then no navigation effect is emitted and errors are set`() =
        runTest {
            // Given
            presenter.initialize(samplePaymentMethod())
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("a"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAddressChange("   "))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnIsAutoConfChange(true))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange("0"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange("0"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange("x"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
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
        runTest {
            // Given
            presenter.initialize(samplePaymentMethod())
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("  ETH Account  "))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAddressChange("  0xABC123  "))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnIsInstantChange(true))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnIsAutoConfChange(true))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange(" 2 "))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange(" 1 "))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange("  https://explorer.eth  "))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
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
        runTest {
            // Given
            presenter.initialize(samplePaymentMethod())
            presenter.onCommonAction(AccountFormUiAction.OnUniqueAccountNameChange("ETH No AutoConf"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAddressChange("0xNOAUTO"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnIsAutoConfChange(false))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange("2"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange("1"))
            presenter.onCryptoCommonAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange("https://ignored.explorer"))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onCommonAction(AccountFormUiAction.OnNextClick)
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
