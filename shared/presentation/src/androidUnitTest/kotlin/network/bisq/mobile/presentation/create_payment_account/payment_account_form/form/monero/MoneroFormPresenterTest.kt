package network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.monero

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
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.model.account.PaymentTypeVO
import network.bisq.mobile.presentation.common.test_utils.TestCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.AccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.CryptoAccountFormUiAction
import network.bisq.mobile.presentation.create_payment_account.payment_account_form.form.action.MoneroFormUiAction
import network.bisq.mobile.presentation.create_payment_account.select_payment_method.model.CryptoPaymentMethodVO
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
import kotlin.test.assertSame
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class MoneroFormPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var presenter: MoneroFormPresenter

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

        presenter =
            MoneroFormPresenter(
                mainPresenter = mainPresenter,
            )
        presenter.initialize(
            CryptoPaymentMethodVO(
                paymentType = PaymentTypeVO.XMR,
                code = "XMR",
                name = "Monero",
                supportAutoConf = true,
                tradeLimitInfo = "5000.00",
                tradeDuration = "4 days",
            ),
        )
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
    fun `when use sub addresses is enabled then flag is updated and placeholder is set`() =
        runTest(testDispatcher) {
            // When
            presenter.onAction(
                MoneroFormUiAction.OnUseSubAddressesChange(
                    true,
                ),
            )

            // Then
            val state = presenter.uiState.value
            assertTrue(state.useSubAddresses)
            assertEquals("TODO: SubAddress creation not implemented yet", state.subAddressEntry.value)
        }

    @Test
    fun `when monero custom fields change and use sub addresses enabled then values update and placeholder stays synced`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(
                MoneroFormUiAction.OnUseSubAddressesChange(
                    true,
                ),
            )

            // When
            presenter.onAction(
                MoneroFormUiAction.OnMainAddressChange(
                    "main-address",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnPrivateViewKeyChange(
                    "private-view-key",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnAccountIndexChange(
                    "7",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnInitialSubAddressIndexChange(
                    "8",
                ),
            )

            // Then
            val state = presenter.uiState.value
            assertEquals("main-address", state.mainAddressEntry.value)
            assertEquals("private-view-key", state.privateViewKeyEntry.value)
            assertEquals("7", state.accountIndexEntry.value)
            assertEquals("8", state.initialSubAddressIndexEntry.value)
            assertEquals("TODO: SubAddress creation not implemented yet", state.subAddressEntry.value)
        }

    @Test
    fun `when monero custom fields change and use sub addresses disabled then keeps existing sub address entry`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(
                MoneroFormUiAction.OnUseSubAddressesChange(
                    false,
                ),
            )
            val initialSubAddressEntry = presenter.uiState.value.subAddressEntry

            // When
            presenter.onAction(
                MoneroFormUiAction.OnMainAddressChange(
                    "main-address-plain",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnPrivateViewKeyChange(
                    "private-view-key-plain",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnAccountIndexChange(
                    "11",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnInitialSubAddressIndexChange(
                    "12",
                ),
            )

            // Then
            val state = presenter.uiState.value
            assertFalse(state.useSubAddresses)
            assertEquals("main-address-plain", state.mainAddressEntry.value)
            assertEquals("private-view-key-plain", state.privateViewKeyEntry.value)
            assertEquals("11", state.accountIndexEntry.value)
            assertEquals("12", state.initialSubAddressIndexEntry.value)
            assertSame(initialSubAddressEntry, state.subAddressEntry)
            assertEquals("TODO: SubAddress creation not implemented yet", state.subAddressEntry.value)
        }

    @Test
    fun `when next clicked with invalid entries then no navigation effect is emitted`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(
                AccountFormUiAction.OnUniqueAccountNameChange(
                    "a",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnUseSubAddressesChange(
                    true,
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnMainAddressChange(
                    "short",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnPrivateViewKeyChange(
                    "short",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnAccountIndexChange(
                    "-1",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnInitialSubAddressIndexChange(
                    "100001",
                ),
            )

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            assertFalse(effectDeferred.isCompleted)
            effectDeferred.cancel()

            val state = presenter.uiState.value
            assertTrue(presenter.uniqueAccountNameEntry.value.errorMessage != null)
            assertTrue(state.mainAddressEntry.errorMessage != null)
            assertTrue(state.privateViewKeyEntry.errorMessage != null)
            assertTrue(state.accountIndexEntry.errorMessage != null)
            assertTrue(state.initialSubAddressIndexEntry.errorMessage != null)
        }

    @Test
    fun `when next clicked with valid non subaddress form then emits account with trimmed direct address`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(
                AccountFormUiAction.OnUniqueAccountNameChange(
                    "  Monero Account  ",
                ),
            )
            presenter.onAction(
                MoneroFormUiAction.OnUseSubAddressesChange(
                    false,
                ),
            )
            presenter.onAction(
                CryptoAccountFormUiAction.OnIsInstantChange(
                    true,
                ),
            )
            presenter.onAction(
                CryptoAccountFormUiAction.OnAddressChange(
                    "  48A_DIRECT_ADDRESS  ",
                ),
            )

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is MoneroFormEffect.NavigateToNextScreen)

            val account = effect.account
            assertEquals("Monero Account", account.accountName)
            val payload = account.accountPayload
            assertEquals("Monero", payload.currencyName)
            assertEquals("48A_DIRECT_ADDRESS", payload.address)
            assertEquals(false, payload.useSubAddresses)
            assertNull(payload.mainAddress)
            assertNull(payload.privateViewKey)
            assertNull(payload.subAddress)
            assertNull(payload.accountIndex)
            assertNull(payload.initialSubAddressIndex)
            assertEquals(true, payload.isInstant)
            assertEquals(false, payload.isAutoConf)
            assertNull(payload.autoConfNumConfirmations)
            assertNull(payload.autoConfMaxTradeAmount)
            assertNull(payload.autoConfExplorerUrls)
        }

    @Test
    fun `when next clicked with auto conf enabled then emits parsed auto conf payload fields`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Monero AutoConf"))
            presenter.onAction(MoneroFormUiAction.OnUseSubAddressesChange(false))
            presenter.onAction(CryptoAccountFormUiAction.OnAddressChange("48A_AUTOCONF_ADDRESS"))
            presenter.onAction(CryptoAccountFormUiAction.OnIsAutoConfChange(true))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfNumConfirmationsChange(" 2 "))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfMaxTradeAmountChange(" 1 "))
            presenter.onAction(CryptoAccountFormUiAction.OnAutoConfExplorerUrlsChange("  https://xmr.explorer  "))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is MoneroFormEffect.NavigateToNextScreen)

            val payload = effect.account.accountPayload
            assertEquals("48A_AUTOCONF_ADDRESS", payload.address)
            assertEquals(false, payload.useSubAddresses)
            assertEquals(true, payload.isAutoConf)
            assertEquals(2, payload.autoConfNumConfirmations)
            assertEquals(1L, payload.autoConfMaxTradeAmount)
            assertEquals("https://xmr.explorer", payload.autoConfExplorerUrls)
        }

    @Test
    fun `when next clicked with valid subaddress form then emits account using main address and parsed indexes`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Monero Subaddresses"))
            presenter.onAction(MoneroFormUiAction.OnUseSubAddressesChange(true))
            presenter.onAction(MoneroFormUiAction.OnMainAddressChange("  48A_MAIN_ADDRESS_123  "))
            presenter.onAction(MoneroFormUiAction.OnPrivateViewKeyChange("  PRIVATE_VIEW_KEY_123  "))
            presenter.onAction(MoneroFormUiAction.OnAccountIndexChange(" 42 "))
            presenter.onAction(MoneroFormUiAction.OnInitialSubAddressIndexChange(" 7 "))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is MoneroFormEffect.NavigateToNextScreen)

            val account = effect.account
            val payload = account.accountPayload
            assertEquals("Monero", payload.currencyName)
            assertEquals("48A_MAIN_ADDRESS_123", payload.address)
            assertTrue(payload.useSubAddresses)
            assertEquals("48A_MAIN_ADDRESS_123", payload.mainAddress)
            assertEquals("PRIVATE_VIEW_KEY_123", payload.privateViewKey)
            assertNull(payload.subAddress)
            assertEquals(42, payload.accountIndex)
            assertEquals(7, payload.initialSubAddressIndex)
        }

    @Test
    fun `when toggling from subaddress mode to direct mode then stale subaddress mode fields are not included in payload`() =
        runTest(testDispatcher) {
            // Given
            presenter.onAction(AccountFormUiAction.OnUniqueAccountNameChange("Monero Toggle"))
            presenter.onAction(MoneroFormUiAction.OnUseSubAddressesChange(true))
            presenter.onAction(MoneroFormUiAction.OnMainAddressChange("  48A_STALE_MAIN_ADDRESS  "))
            presenter.onAction(MoneroFormUiAction.OnPrivateViewKeyChange("  STALE_PRIVATE_VIEW_KEY  "))
            presenter.onAction(MoneroFormUiAction.OnAccountIndexChange(" 17 "))
            presenter.onAction(MoneroFormUiAction.OnInitialSubAddressIndexChange(" 19 "))
            presenter.onAction(MoneroFormUiAction.OnUseSubAddressesChange(false))
            presenter.onAction(CryptoAccountFormUiAction.OnAddressChange("  48A_DIRECT_AFTER_TOGGLE  "))

            // When
            val effectDeferred = async { presenter.effect.first() }
            presenter.onAction(AccountFormUiAction.OnNextClick)
            advanceUntilIdle()

            // Then
            val effect = effectDeferred.await()
            assertTrue(effect is MoneroFormEffect.NavigateToNextScreen)

            val payload = effect.account.accountPayload
            assertEquals("48A_DIRECT_AFTER_TOGGLE", payload.address)
            assertFalse(payload.useSubAddresses)
            assertNull(payload.mainAddress)
            assertNull(payload.privateViewKey)
            assertNull(payload.accountIndex)
            assertNull(payload.initialSubAddressIndex)
            assertNull(payload.subAddress)
        }

    @Test
    fun `validateTextMinMaxLength validates trimmed boundaries`() {
        assertNull(validateTextMinMaxLength(" ${"a".repeat(10)} "))
        assertNull(validateTextMinMaxLength(" ${"a".repeat(200)} "))
        assertTrue(validateTextMinMaxLength("a".repeat(9)) != null)
        assertTrue(validateTextMinMaxLength("a".repeat(201)) != null)
    }

    @Test
    fun `validateAccountIndex and validateInitialSubAddressIndex accept empty and range values and reject invalid ones`() {
        assertNull(validateAccountIndex(""))
        assertNull(validateAccountIndex("0"))
        assertNull(validateAccountIndex("100000"))
        assertTrue(validateAccountIndex("-1") != null)
        assertTrue(validateAccountIndex("100001") != null)

        assertNull(validateInitialSubAddressIndex(""))
        assertNull(validateInitialSubAddressIndex("0"))
        assertNull(validateInitialSubAddressIndex("100000"))
        assertTrue(validateInitialSubAddressIndex("-1") != null)
        assertTrue(validateInitialSubAddressIndex("100001") != null)
    }

    @Test
    fun `validateMainAddress and validatePrivateViewKey delegate to text min max validation`() {
        assertNull(validateMainAddress(" ${"x".repeat(10)} "))
        assertNull(validatePrivateViewKey(" ${"x".repeat(10)} "))
        assertTrue(validateMainAddress("short") != null)
        assertTrue(validatePrivateViewKey("short") != null)
    }
}
