package network.bisq.mobile.client.payment_accounts.presentation.payment_account_detail

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.country.Country
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.common.currency.FiatCurrency
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.ZelleAccount
import network.bisq.mobile.client.payment_accounts.domain.model.fiat.zelle.ZelleAccountPayload
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.domain.model.account.PaymentAccount
import network.bisq.mobile.domain.model.account.fiat.FiatPaymentMethodChargebackRisk
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.components.organisms.SnackbarType
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.main.MainPresenter
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PaymentAccountMusigDetailPresenterTest : ClientKoinIntegrationTestBase() {
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val globalUiManager: GlobalUiManager = mockk(relaxed = true)
    private val navigationManager: NavigationManager = mockk(relaxed = true)
    private val accountsByNameFlow = MutableStateFlow<Map<String, PaymentAccount>>(emptyMap())

    private lateinit var presenter: PaymentAccountMusigDetailPresenter

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<NavigationManager> { navigationManager }
                single<GlobalUiManager> { globalUiManager }
            },
        )

    override fun onSetup() {
        every { paymentAccountsServiceFacade.accountsByName } returns accountsByNameFlow
        every { globalUiManager.scheduleShowLoading() } returns Unit
        every { globalUiManager.scheduleHideLoading() } returns Unit
        every { navigationManager.navigateBack(any()) } returns Unit
    }

    private fun createPresenter(): PaymentAccountMusigDetailPresenter =
        PaymentAccountMusigDetailPresenter(
            paymentAccountsServiceFacade = paymentAccountsServiceFacade,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when initialize with matching account then sets payment account and clears missing flag`() =
        runTest {
            // Given
            val account = sampleZelleAccount()
            accountsByNameFlow.value = mapOf(account.accountName to account)
            presenter = createPresenter()

            // When
            presenter.initialize(account.accountName)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(account, state.paymentAccount)
            assertFalse(state.isAccountMissing)
        }

    @Test
    fun `when initialize with unknown account then marks account as missing`() =
        runTest {
            // Given
            val existingAccount = sampleZelleAccount()
            accountsByNameFlow.value = mapOf(existingAccount.accountName to existingAccount)
            presenter = createPresenter()

            // When
            presenter.initialize("Bob")
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(null, state.paymentAccount)
            assertTrue(state.isAccountMissing)
        }

    @Test
    fun `when delete action triggered then shows delete confirmation dialog`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // Then
            assertTrue(presenter.uiState.value.showDeleteConfirmationDialog)
        }

    @Test
    fun `when cancel delete action triggered then hides delete confirmation dialog`() =
        runTest {
            // Given
            presenter = createPresenter()
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnCancelDeleteAccountClick)
            advanceUntilIdle()

            // Then
            assertFalse(presenter.uiState.value.showDeleteConfirmationDialog)
        }

    @Test
    fun `when confirm delete without selected account then does not call delete or navigation`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnConfirmDeleteAccountClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { paymentAccountsServiceFacade.deleteAccount(any()) }
            verify(exactly = 0) { navigationManager.navigateBack(any()) }
            verify(exactly = 0) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 0) { globalUiManager.scheduleHideLoading() }
        }

    @Test
    fun `when confirm delete succeeds then hides dialog shows loading lifecycle and navigates back`() =
        runTest {
            // Given
            val account = sampleZelleAccount()
            accountsByNameFlow.value = mapOf(account.accountName to account)
            coEvery { paymentAccountsServiceFacade.deleteAccount(account.accountName) } returns Result.success(Unit)
            presenter = createPresenter()
            presenter.initialize(account.accountName)
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnConfirmDeleteAccountClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.deleteAccount(account.accountName) }
            verify(exactly = 2) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 1) { globalUiManager.scheduleHideLoading() }
            verify(exactly = 1) { navigationManager.navigateBack(any()) }
            assertFalse(presenter.uiState.value.showDeleteConfirmationDialog)
        }

    @Test
    fun `when confirm delete fails then hides dialog loading and shows error snackbar without navigation`() =
        runTest {
            // Given
            val account = sampleZelleAccount()
            accountsByNameFlow.value = mapOf(account.accountName to account)
            coEvery { paymentAccountsServiceFacade.deleteAccount(account.accountName) } returns Result.failure(IllegalStateException("delete failed"))
            presenter = createPresenter()
            presenter.initialize(account.accountName)
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnDeleteAccountClick)
            advanceUntilIdle()

            // When
            presenter.onAction(PaymentAccountMusigDetailUiAction.OnConfirmDeleteAccountClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.deleteAccount(account.accountName) }
            verify(exactly = 1) { globalUiManager.scheduleShowLoading() }
            verify(exactly = 1) { globalUiManager.scheduleHideLoading() }
            verify(exactly = 0) { navigationManager.navigateBack(any()) }
            verify {
                globalUiManager.showSnackbar(
                    any(),
                    SnackbarType.ERROR,
                    any(),
                    any(),
                )
            }
            assertFalse(presenter.uiState.value.showDeleteConfirmationDialog)
        }

    private fun sampleZelleAccount(): ZelleAccount =
        ZelleAccount(
            accountName = "Alice",
            accountPayload =
                ZelleAccountPayload(
                    holderName = "Alice",
                    emailOrMobileNr = "alice@example.com",
                    chargebackRisk = FiatPaymentMethodChargebackRisk.LOW,
                    paymentMethodName = "Zelle",
                    currency = FiatCurrency(code = "USD", name = "US Dollar"),
                    country = Country(code = "US", name = "United States"),
                ),
            creationDate = null,
            tradeLimitInfo = null,
            tradeDuration = null,
        )
}
