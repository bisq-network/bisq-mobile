package network.bisq.mobile.client.payment_accounts.presentation.create_payment_account.step1_select_payment_method.crypto

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.client.payment_accounts.domain.model.crypto.CryptoPaymentMethod
import network.bisq.mobile.client.payment_accounts.domain.service.PaymentAccountsServiceFacade
import network.bisq.mobile.presentation.common.ui.utils.EMPTY_STRING
import network.bisq.mobile.presentation.main.MainPresenter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class SelectCryptoPaymentMethodPresenterTest : ClientKoinIntegrationTestBase() {
    private val paymentAccountsServiceFacade: PaymentAccountsServiceFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)

    private lateinit var presenter: SelectCryptoPaymentMethodPresenter

    private fun createPresenter(): SelectCryptoPaymentMethodPresenter =
        SelectCryptoPaymentMethodPresenter(
            paymentAccountsServiceFacade = paymentAccountsServiceFacade,
            mainPresenter = mainPresenter,
        )

    @Test
    fun `when initial state then has expected defaults`() =
        runTest {
            // Given
            presenter = createPresenter()

            // When
            val state = presenter.uiState.value

            // Then
            assertTrue(state.paymentMethods.isEmpty())
            assertNull(state.selectedPaymentMethod)
            assertFalse(state.isLoading)
            assertFalse(state.isError)
            assertEquals("", state.searchQuery)
        }

    @Test
    fun `when view is attached then crypto methods are loaded`() =
        runTest {
            // Given
            val methods =
                listOf(
                    sampleCryptoMethod(code = "XMR", name = "Monero"),
                    sampleCryptoMethod(code = "LTC", name = "Litecoin"),
                )
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.getCryptoPaymentMethods() }
            val state = presenter.uiState.value
            assertEquals(2, state.paymentMethods.size)
            assertFalse(state.isLoading)
            assertFalse(state.isError)
        }

    @Test
    fun `when crypto methods load fails then error state is set`() =
        runTest {
            // Given
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns
                Result.failure(
                    IllegalStateException("crypto fail"),
                )
            presenter = createPresenter()

            // When
            presenter.onViewAttached()
            advanceUntilIdle()

            // Then
            coVerify(exactly = 1) { paymentAccountsServiceFacade.getCryptoPaymentMethods() }
            val state = presenter.uiState.value
            assertFalse(state.isLoading)
            assertTrue(state.isError)
            assertTrue(state.paymentMethods.isEmpty())
        }

    @Test
    fun `when retry is clicked after failure then methods are requested again and error clears on success`() =
        runTest {
            // Given
            val methods = listOf(sampleCryptoMethod(code = "XMR", name = "Monero"))
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returnsMany
                listOf(
                    Result.failure(IllegalStateException("first fail")),
                    Result.success(methods),
                )
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SelectCryptoPaymentMethodUiAction.OnRetryLoadPaymentMethodsClick)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 2) { paymentAccountsServiceFacade.getCryptoPaymentMethods() }
            val state = presenter.uiState.value
            assertFalse(state.isLoading)
            assertFalse(state.isError)
            assertEquals(1, state.paymentMethods.size)
        }

    @Test
    fun `when search query changes then list is filtered by name or code`() =
        runTest {
            // Given
            val methods =
                listOf(
                    sampleCryptoMethod(code = "XMR", name = "Monero"),
                    sampleCryptoMethod(code = "LTC", name = "Litecoin"),
                )
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SelectCryptoPaymentMethodUiAction.OnSearchQueryChange("ltc"))

            // Then
            val state = presenter.uiState.value
            assertEquals("ltc", state.searchQuery)
            assertEquals(1, state.paymentMethods.size)
            assertEquals("LTC", state.paymentMethods.first().code)
        }

    @Test
    fun `when search query has surrounding whitespace then query is trimmed before filtering`() =
        runTest {
            // Given
            val methods =
                listOf(
                    sampleCryptoMethod(code = "XMR", name = "Monero"),
                    sampleCryptoMethod(code = "LTC", name = "Litecoin"),
                )
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(SelectCryptoPaymentMethodUiAction.OnSearchQueryChange("  xmr  "))

            // Then
            val state = presenter.uiState.value
            assertEquals("xmr", state.searchQuery)
            assertEquals(1, state.paymentMethods.size)
            assertEquals("XMR", state.paymentMethods.first().code)
        }

    @Test
    fun `when selected method is filtered out by search then selected method is cleared`() =
        runTest {
            // Given
            val methods =
                listOf(
                    sampleCryptoMethod(code = "XMR", name = "Monero"),
                    sampleCryptoMethod(code = "LTC", name = "Litecoin"),
                )
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            val monero =
                presenter.uiState.value.paymentMethods
                    .first { it.code == "XMR" }
            presenter.onAction(SelectCryptoPaymentMethodUiAction.OnPaymentMethodClick(monero))

            // When
            presenter.onAction(SelectCryptoPaymentMethodUiAction.OnSearchQueryChange("ltc"))

            // Then
            val state = presenter.uiState.value
            assertEquals(1, state.paymentMethods.size)
            assertEquals("LTC", state.paymentMethods.first().code)
            assertNull(state.selectedPaymentMethod)
        }

    @Test
    fun `when method is selected then selected payment method is updated`() =
        runTest {
            // Given
            val methods = listOf(sampleCryptoMethod(code = "XMR", name = "Monero"))
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            val selected =
                presenter.uiState.value.paymentMethods
                    .first()

            // When
            presenter.onAction(SelectCryptoPaymentMethodUiAction.OnPaymentMethodClick(selected))

            // Then
            val state = presenter.uiState.value
            assertEquals("XMR", state.selectedPaymentMethod?.code)
        }

    @Test
    fun `when next is clicked with a selected method then navigation effect is emitted`() =
        runTest {
            // Given
            val methods = listOf(sampleCryptoMethod(code = "XMR", name = "Monero"))
            coEvery { paymentAccountsServiceFacade.getCryptoPaymentMethods() } returns Result.success(methods)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()
            val selected =
                presenter.uiState.value.paymentMethods
                    .first()
            presenter.onAction(SelectCryptoPaymentMethodUiAction.OnPaymentMethodClick(selected))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(SelectCryptoPaymentMethodUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is SelectCryptoPaymentMethodEffect.NavigateToNextScreen)
            assertEquals("XMR", effect.selectedPaymentMethod.code)
        }

    private fun sampleCryptoMethod(
        code: String,
        name: String,
        supportAutoConf: Boolean = false,
    ): CryptoPaymentMethod =
        CryptoPaymentMethod(
            code = code,
            name = name,
            supportAutoConf = supportAutoConf,
            tradeDuration = EMPTY_STRING,
            tradeLimitInfo = EMPTY_STRING,
        )
}
