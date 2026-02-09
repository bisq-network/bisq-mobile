package network.bisq.mobile.client.trusted_node_setup

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.Clock
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.client.common.domain.access.pairing.PairingCode
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.trusted_node_setup.use_case.TrustedNodeConnectionStatus
import network.bisq.mobile.client.trusted_node_setup.use_case.TrustedNodeSetupUseCase
import network.bisq.mobile.client.trusted_node_setup.use_case.TrustedNodeSetupUseCaseState
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
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
import kotlin.test.assertTrue

/**
 * Unit tests for TrustedNodeSetupPresenter.
 *
 * These tests verify the business logic of the TrustedNodeSetupPresenter,
 * including pairing code validation, connection management, and user actions.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrustedNodeSetupPresenterTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var mainPresenter: MainPresenter
    private lateinit var kmpTorService: KmpTorService
    private lateinit var trustedNodeSetupUseCase: TrustedNodeSetupUseCase
    private lateinit var apiAccessService: ApiAccessService
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var navigationManager: NavigationManager
    private lateinit var presenter: TrustedNodeSetupPresenter

    // Test data
    private val validPairingCode = "12345-ABCDE"
    private val validApiUrl = "ws://example.com:8080"
    private val validRestApiUrl = "http://example.com:8080"
    private val validPairingQrCode =
        PairingQrCode(
            version = PairingCode.VERSION,
            pairingCode =
                PairingCode(
                    id = "testId",
                    expiresAt = Clock.System.now(),
                    grantedPermissions = setOf(Permission.SETTINGS),
                ),
            webSocketUrl = validApiUrl,
            restApiUrl = validRestApiUrl,
            tlsFingerprint = null,
            torClientAuthSecret = null,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Setup i18n
        I18nSupport.setLanguage()

        // Setup mocks
        mainPresenter = mockk(relaxed = true)
        kmpTorService = mockk(relaxed = true)
        trustedNodeSetupUseCase = mockk(relaxed = true)
        apiAccessService = mockk(relaxed = true)
        sensitiveSettingsRepository = mockk(relaxed = true)
        navigationManager = mockk(relaxed = true)

        startKoin {
            modules(
                module {
                    single<NavigationManager> { navigationManager }
                    single<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
                    single<GlobalUiManager> { mockk(relaxed = true) }
                    single<SensitiveSettingsRepository> { sensitiveSettingsRepository }
                },
            )
        }

        // Default mock behaviors
        every { trustedNodeSetupUseCase.state } returns MutableStateFlow(TrustedNodeSetupUseCaseState())
        every { kmpTorService.state } returns MutableStateFlow(KmpTorService.TorState.Stopped())
        every { kmpTorService.bootstrapProgress } returns MutableStateFlow(0)
    }

    @AfterTest
    fun tearDown() {
        try {
            stopKoin()
        } finally {
            Dispatchers.resetMain()
        }
    }

    private fun createPresenter(): TrustedNodeSetupPresenter =
        TrustedNodeSetupPresenter(
            mainPresenter,
            kmpTorService,
            trustedNodeSetupUseCase,
            apiAccessService,
            sensitiveSettingsRepository,
        )

    private fun TestScope.setupPresenter() {
        presenter = createPresenter()
        presenter.onViewAttached()
        advanceUntilIdle()
    }

    // ========== Initial State Tests ==========

    @Test
    fun `when initial state then has correct default values`() =
        runTest(testDispatcher) {
            // When
            setupPresenter()

            // Then
            val state = presenter.uiState.value
            assertEquals("", state.apiUrl)
            assertEquals("", state.pairingCodeEntry.value)
            assertEquals(TrustedNodeConnectionStatus.Idle, state.status)
            assertEquals(0, state.torProgress)
            assertEquals(0L, state.timeoutCounter)
            assertFalse(state.showQrCodeView)
            assertFalse(state.showQrCodeError)
        }

    @Test
    fun `when initialize with workflow false then sets connected status`() =
        runTest(testDispatcher) {
            // Given
            coEvery { sensitiveSettingsRepository.fetch() } returns SensitiveSettings(bisqApiUrl = validRestApiUrl)
            setupPresenter()

            // When
            presenter.initialize(isWorkflow = false)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(validRestApiUrl, state.apiUrl)
            assertEquals(TrustedNodeConnectionStatus.Connected, state.status)
        }

    @Test
    fun `when initialize with workflow true then does not change status`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()

            // When
            presenter.initialize(isWorkflow = true)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(TrustedNodeConnectionStatus.Idle, state.status)
        }

    // ========== Pairing Code Validation Tests ==========

    @Test
    fun `when valid pairing code entered then updates api url`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(validPairingCode))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(validPairingCode, state.pairingCodeEntry.value)
            assertEquals(validRestApiUrl, state.apiUrl)
            assertEquals(TrustedNodeConnectionStatus.Idle, state.status)
            assertFalse(state.showQrCodeView)
        }

    @Test
    fun `when invalid pairing code entered then shows error`() =
        runTest(testDispatcher) {
            // Given
            val errorMessage = "Invalid pairing code"
            val invalidParingCode = "invalid-code"
            every { apiAccessService.getPairingCodeQr(invalidParingCode) } returns Result.failure(Exception(errorMessage))
            setupPresenter()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(invalidParingCode))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(invalidParingCode, state.pairingCodeEntry.value)
            assertEquals(errorMessage, state.pairingCodeEntry.errorMessage)
            assertEquals("", state.apiUrl)
        }

    @Test
    fun `when blank pairing code entered then clears state`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            setupPresenter()

            // First set a valid code
            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(validPairingCode))
            advanceUntilIdle()

            // When - clear the code
            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange("   "))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals("", state.pairingCodeEntry.value)
            assertEquals("", state.apiUrl)
            assertEquals(TrustedNodeConnectionStatus.Idle, state.status)
        }

    @Test
    fun `when pairing code with whitespace entered then trims correctly`() =
        runTest(testDispatcher) {
            // Given
            val codeWithWhitespace = "  $validPairingCode  "
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            setupPresenter()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(codeWithWhitespace))
            advanceUntilIdle()

            // Then
            verify { apiAccessService.getPairingCodeQr(validPairingCode) }
            val state = presenter.uiState.value
            assertEquals(validPairingCode, state.pairingCodeEntry.value)
        }

    // ========== QR Code Handling Tests ==========

    @Test
    fun `when show qr code view action then sets flag to true`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnShowQrCodeView)
            advanceUntilIdle()

            // Then
            assertTrue(presenter.uiState.value.showQrCodeView)
        }

    @Test
    fun `when qr code view dismissed then sets flag to false`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()
            presenter.onAction(TrustedNodeSetupUiAction.OnShowQrCodeView)
            advanceUntilIdle()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnQrCodeViewDismiss)
            advanceUntilIdle()

            // Then
            assertFalse(presenter.uiState.value.showQrCodeView)
        }

    @Test
    fun `when qr code view failed to open then shows error and closes view`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()
            presenter.onAction(TrustedNodeSetupUiAction.OnShowQrCodeView)
            advanceUntilIdle()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnQrCodeFail)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertFalse(state.showQrCodeView)
            assertTrue(state.showQrCodeError)
        }

    @Test
    fun `when qr code error closed then clears error flag`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()
            presenter.onAction(TrustedNodeSetupUiAction.OnQrCodeFail)
            advanceUntilIdle()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnQrCodeErrorClose)
            advanceUntilIdle()

            // Then
            assertFalse(presenter.uiState.value.showQrCodeError)
        }

    @Test
    fun `when qr code result received then processes as pairing code`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            setupPresenter()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnQrCodeResult(validPairingCode))
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(validPairingCode, state.pairingCodeEntry.value)
            assertEquals(validRestApiUrl, state.apiUrl)
        }

    // ========== Connection Flow Tests ==========

    @Test
    fun `when test and save pressed then starts use case`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            coEvery { trustedNodeSetupUseCase.execute(any()) } returns true
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(validPairingCode))
            advanceUntilIdle()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnTestAndSavePress)
            advanceUntilIdle()

            // Then
            coVerify { trustedNodeSetupUseCase.execute(validPairingQrCode) }
        }

    @Test
    fun `when test and save pressed then starts countdown timer`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            coEvery { trustedNodeSetupUseCase.execute(any()) } coAnswers {
                delay(5000)
                true
            }
            setupPresenter()

            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(validPairingCode))
            advanceUntilIdle()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnTestAndSavePress)
            advanceTimeBy(2000) // Advance by 2 second

            // Then
            val state = presenter.uiState.value
            assertTrue(state.timeoutCounter > 0)
        }

    @Test
    fun `when connection setup succeeds then navigates to splash`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            coEvery { trustedNodeSetupUseCase.execute(any()) } returns true
            setupPresenter()

            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(validPairingCode))
            advanceUntilIdle()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnTestAndSavePress)
            advanceUntilIdle()

            // Then
            // Verify use case was executed successfully
            coVerify { trustedNodeSetupUseCase.execute(validPairingQrCode) }
            // Verify navigation to splash screen occurred
            verify { navigationManager.navigate(NavRoute.Splash, any(), any()) }
        }

    @Test
    fun `when connection setup fails then does not navigate`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            coEvery { trustedNodeSetupUseCase.execute(any()) } returns false
            setupPresenter()

            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(validPairingCode))
            advanceUntilIdle()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnTestAndSavePress)
            advanceUntilIdle()

            // Then
            // Verify use case was executed
            coVerify { trustedNodeSetupUseCase.execute(validPairingQrCode) }
            // Verify navigation did NOT occur
            verify(exactly = 0) { navigationManager.navigate(any(), any(), any()) }
        }

    @Test
    fun `when test and save pressed without pairing code then does nothing`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnTestAndSavePress)
            advanceUntilIdle()

            // Then
            coVerify(exactly = 0) { trustedNodeSetupUseCase.execute(any()) }
        }

    @Test
    fun `when test and save pressed during ongoing connection then ignores request`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            coEvery { trustedNodeSetupUseCase.execute(any()) } coAnswers {
                delay(5000)
                true
            }
            setupPresenter()

            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(validPairingCode))
            advanceUntilIdle()

            // Start first connection
            presenter.onAction(TrustedNodeSetupUiAction.OnTestAndSavePress)
            advanceTimeBy(100)

            // When - try to start another connection
            presenter.onAction(TrustedNodeSetupUiAction.OnTestAndSavePress)
            advanceUntilIdle()

            // Then - use case should be called only once
            coVerify(exactly = 1) { trustedNodeSetupUseCase.execute(any()) }
        }

    // ========== Cancel Operation Tests ==========

    @Test
    fun `when cancel pressed then cancels jobs and resets state`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            coEvery { trustedNodeSetupUseCase.execute(any()) } coAnswers {
                delay(10000)
                true
            }
            setupPresenter()

            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(validPairingCode))
            advanceUntilIdle()

            // Start connection
            presenter.onAction(TrustedNodeSetupUiAction.OnTestAndSavePress)
            advanceTimeBy(1000)

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnCancelPress)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(TrustedNodeConnectionStatus.Idle, state.status)
            assertEquals(0L, state.timeoutCounter)
        }

    @Test
    fun `when cancel pressed with internal tor starting then stops tor`() =
        runTest(testDispatcher) {
            // Given
            every { apiAccessService.getPairingCodeQr(validPairingCode) } returns Result.success(validPairingQrCode)
            val torStartingState = MutableStateFlow(KmpTorService.TorState.Starting)
            every { kmpTorService.state } returns torStartingState
            coEvery { trustedNodeSetupUseCase.execute(any()) } coAnswers {
                delay(10000)
                true
            }
            coEvery { kmpTorService.stopTor() } returns Unit
            setupPresenter()

            presenter.onAction(TrustedNodeSetupUiAction.OnPairingCodeChange(validPairingCode))
            advanceUntilIdle()

            // Start connection (simulating INTERNAL_TOR scenario)
            presenter.onAction(TrustedNodeSetupUiAction.OnTestAndSavePress)
            advanceTimeBy(100)

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnCancelPress)
            advanceUntilIdle()

            // Then
            coVerify { kmpTorService.stopTor() }
        }

    @Test
    fun `when cancel pressed without connection then resets state safely`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()

            // When
            presenter.onAction(TrustedNodeSetupUiAction.OnCancelPress)
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(TrustedNodeConnectionStatus.Idle, state.status)
            assertEquals(0L, state.timeoutCounter)
        }

    // ========== Flow Observation Tests ==========

    @Test
    fun `when use case emits connection status then updates ui state`() =
        runTest(testDispatcher) {
            // Given
            val stateFlow = MutableStateFlow(TrustedNodeSetupUseCaseState())
            every { trustedNodeSetupUseCase.state } returns stateFlow
            presenter = createPresenter()
            presenter.onViewAttached()
            advanceUntilIdle()

            // When
            stateFlow.value = TrustedNodeSetupUseCaseState(connectionStatus = TrustedNodeConnectionStatus.Connecting)
            advanceUntilIdle()

            // Then
            assertEquals(TrustedNodeConnectionStatus.Connecting, presenter.uiState.value.status)
        }

    @Test
    fun `when use case emits incompatible api version then updates with server version`() =
        runTest(testDispatcher) {
            // Given
            val stateFlow = MutableStateFlow(TrustedNodeSetupUseCaseState())
            every { trustedNodeSetupUseCase.state } returns stateFlow
            setupPresenter()

            // When
            stateFlow.value =
                TrustedNodeSetupUseCaseState(
                    connectionStatus = TrustedNodeConnectionStatus.IncompatibleHttpApiVersion,
                    serverVersion = "2.0.0",
                )
            advanceUntilIdle()

            // Then
            val state = presenter.uiState.value
            assertEquals(TrustedNodeConnectionStatus.IncompatibleHttpApiVersion, state.status)
            assertEquals("2.0.0", state.serverVersion)
        }

    @Test
    fun `when tor state changes then updates ui state`() =
        runTest(testDispatcher) {
            // Given
            val torStateFlow: MutableStateFlow<KmpTorService.TorState> =
                MutableStateFlow(KmpTorService.TorState.Stopped())
            every { kmpTorService.state } returns torStateFlow
            setupPresenter()

            // When
            torStateFlow.value = KmpTorService.TorState.Starting
            advanceUntilIdle()

            // Then
            assertTrue(presenter.uiState.value.torState is KmpTorService.TorState.Starting)
        }

    @Test
    fun `when tor bootstrap progress changes then updates ui state`() =
        runTest(testDispatcher) {
            // Given
            val torProgressFlow = MutableStateFlow(0)
            every { kmpTorService.bootstrapProgress } returns torProgressFlow
            setupPresenter()

            // When
            torProgressFlow.value = 50
            advanceUntilIdle()

            // Then
            assertEquals(50, presenter.uiState.value.torProgress)
        }

    // ========== UI State Helper Tests ==========

    @Test
    fun `when status is connecting then isConnectionInProgress returns true`() =
        runTest(testDispatcher) {
            // Given
            val stateFlow = MutableStateFlow(TrustedNodeSetupUseCaseState())
            every { trustedNodeSetupUseCase.state } returns stateFlow
            setupPresenter()

            // When
            stateFlow.value = TrustedNodeSetupUseCaseState(connectionStatus = TrustedNodeConnectionStatus.Connecting)
            advanceUntilIdle()

            // Then
            assertTrue(presenter.uiState.value.isConnectionInProgress())
        }

    @Test
    fun `when status is idle then isConnectionInProgress returns false`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()

            // Then
            assertFalse(presenter.uiState.value.isConnectionInProgress())
        }

    @Test
    fun `when not connected and workflow then canScanQrCode returns true`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()

            // Then
            assertTrue(presenter.uiState.value.canScanQrCode(isWorkflow = true))
        }

    @Test
    fun `when not connected and not workflow then canScanQrCode returns false`() =
        runTest(testDispatcher) {
            // Given
            setupPresenter()

            // Then
            assertFalse(presenter.uiState.value.canScanQrCode(isWorkflow = false))
        }

    @Test
    fun `when connected then canScanQrCode returns false`() =
        runTest(testDispatcher) {
            // Given
            val stateFlow = MutableStateFlow(TrustedNodeSetupUseCaseState())
            every { trustedNodeSetupUseCase.state } returns stateFlow
            setupPresenter()

            stateFlow.value = TrustedNodeSetupUseCaseState(connectionStatus = TrustedNodeConnectionStatus.Connected)
            advanceUntilIdle()

            // Then
            assertFalse(presenter.uiState.value.canScanQrCode(isWorkflow = true))
        }
}
