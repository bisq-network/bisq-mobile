package network.bisq.mobile.client.trusted_node_setup.use_case

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.withTimeout
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.client.common.domain.access.pairing.PairingCode
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.httpclient.HttpClientSettings
import network.bisq.mobile.client.common.domain.httpclient.exception.UnauthorizedApiAccessException
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.exception.IncompatibleHttpApiVersionException
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Unit tests for TrustedNodeSetupUseCase.
 *
 * These tests verify the business logic of connection setup including
 * proxy detection, Tor management, pairing, connection, and error handling.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrustedNodeSetupUseCaseTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var kmpTorService: KmpTorService
    private lateinit var httpClientService: HttpClientService
    private lateinit var apiAccessService: ApiAccessService
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var wsClientService: WebSocketClientService
    private lateinit var applicationBootstrapFacade: ApplicationBootstrapFacade
    private lateinit var useCase: TrustedNodeSetupUseCase

    // Test data
    private val testExpiresAt: Instant = Clock.System.now().plus(1.seconds)

    private val onionPairingQrCode =
        PairingQrCode(
            version = PairingCode.VERSION,
            pairingCode =
                PairingCode(
                    id = "testId",
                    expiresAt = testExpiresAt,
                    grantedPermissions = setOf(Permission.SETTINGS),
                ),
            webSocketUrl = "ws://test1234567890123456789012345678901234567890123456.onion:8080",
            restApiUrl = "http://test1234567890123456789012345678901234567890123456.onion:8080",
            tlsFingerprint = null,
            torClientAuthSecret = null,
        )

    private val clearnetPairingQrCode =
        PairingQrCode(
            version = PairingCode.VERSION,
            pairingCode =
                PairingCode(
                    id = "testId",
                    expiresAt = testExpiresAt,
                    grantedPermissions = setOf(Permission.SETTINGS),
                ),
            webSocketUrl = "ws://example.com:8080",
            restApiUrl = "http://example.com:8080",
            tlsFingerprint = null,
            torClientAuthSecret = null,
        )

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        I18nSupport.setLanguage()

        // Setup mocks
        kmpTorService = mockk(relaxed = true)
        httpClientService = mockk(relaxed = true)
        apiAccessService = mockk(relaxed = true)
        wsClientService = mockk(relaxed = true)
        applicationBootstrapFacade = mockk(relaxed = true)

        // Fake repository
        sensitiveSettingsRepository =
            object : SensitiveSettingsRepository {
                private val _data = MutableStateFlow(SensitiveSettings())
                override val data = _data

                override suspend fun fetch() = _data.value

                override suspend fun update(transform: suspend (SensitiveSettings) -> SensitiveSettings) {
                    _data.value = transform(_data.value)
                }

                override suspend fun clear() {
                    _data.value = SensitiveSettings()
                }
            }

        // Default successful mock behaviors
        every { kmpTorService.state } returns MutableStateFlow(KmpTorService.TorState.Stopped())
        every { kmpTorService.bootstrapProgress } returns MutableStateFlow(0)
        coEvery { apiAccessService.updateSettings(any()) } coAnswers {
            delay(10) // Small delay to make state changes observable
        }
        every { wsClientService.connectionState } returns MutableStateFlow(ConnectionState.Disconnected())
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createUseCase(): TrustedNodeSetupUseCase =
        TrustedNodeSetupUseCase(
            kmpTorService,
            httpClientService,
            apiAccessService,
            sensitiveSettingsRepository,
            wsClientService,
            applicationBootstrapFacade,
        )

    // ========== URL Validation Tests ==========

    @Test
    fun `when invalid URL format then returns false with invalid format status`() =
        runTest(testDispatcher) {
            // Given
            // Use a truly invalid URL that cannot be parsed (e.g., contains invalid characters)
            val invalidQrCode = clearnetPairingQrCode.copy(restApiUrl = "ht!tp://invalid url with spaces")
            useCase = createUseCase()

            // When
            val result = useCase.execute(invalidQrCode)
            advanceUntilIdle()

            // Then
            assertFalse(result)
            val status = useCase.state.value.connectionStatus
            assertTrue(status is TrustedNodeConnectionStatus.Failed)
            assertEquals(
                "mobile.trustedNodeSetup.apiUrl.invalid.format".i18n(),
                status.displayString,
            )
        }

    // ========== Proxy Detection Tests ==========

    @Test
    fun `when onion URL then uses INTERNAL_TOR proxy`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulTorConnection()
            setupSuccessfulPairing(onionPairingQrCode)
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(onionPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify { kmpTorService.startTor() }
            coVerify { kmpTorService.awaitSocksPort() }
        }

    @Test
    fun `when clearnet URL then uses NONE proxy`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify(exactly = 0) { kmpTorService.startTor() }
        }

    // ========== Tor Service Management Tests ==========

    @Test
    fun `when INTERNAL_TOR and Tor not started then starts and bootstraps Tor`() =
        runTest(testDispatcher) {
            // Given
            val torStateFlow = MutableStateFlow<KmpTorService.TorState>(KmpTorService.TorState.Stopped())
            val bootstrapFlow = MutableStateFlow(0)
            every { kmpTorService.state } returns torStateFlow
            every { kmpTorService.bootstrapProgress } returns bootstrapFlow
            coEvery { kmpTorService.startTor() } coAnswers {
                torStateFlow.value = KmpTorService.TorState.Starting
                bootstrapFlow.value = 50
                torStateFlow.value = KmpTorService.TorState.Started
                bootstrapFlow.value = 100
                true
            }
            coEvery { kmpTorService.awaitSocksPort() } returns 9050
            val httpClientFlow = MutableSharedFlow<HttpClientSettings>(replay = 1)
            every { httpClientService.httpClientChangedFlow } returns httpClientFlow
            httpClientFlow.tryEmit(
                HttpClientSettings(
                    bisqApiUrl = null,
                    tlsFingerprint = null,
                    selectedProxyOption = BisqProxyOption.INTERNAL_TOR,
                ),
            )
            setupSuccessfulPairing(onionPairingQrCode)
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(onionPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify { kmpTorService.startTor() }
            assertEquals(TrustedNodeConnectionStatus.Connected, useCase.state.value.connectionStatus)
        }

    @Test
    fun `when INTERNAL_TOR and Tor already started then reuses existing connection`() =
        runTest(testDispatcher) {
            // Given
            every { kmpTorService.state } returns MutableStateFlow(KmpTorService.TorState.Started)
            coEvery { kmpTorService.awaitSocksPort() } returns 9050
            val httpClientFlow = MutableSharedFlow<HttpClientSettings>(replay = 1)
            every { httpClientService.httpClientChangedFlow } returns httpClientFlow
            httpClientFlow.tryEmit(
                HttpClientSettings(
                    bisqApiUrl = null,
                    tlsFingerprint = null,
                    selectedProxyOption = BisqProxyOption.INTERNAL_TOR,
                ),
            )
            setupSuccessfulPairing(onionPairingQrCode)
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(onionPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify(exactly = 0) { kmpTorService.startTor() }
            coVerify { kmpTorService.awaitSocksPort() }
        }

    @Test
    fun `when Tor start fails then returns false with error`() =
        runTest(testDispatcher) {
            // Given
            val error = IllegalStateException("Tor failed to start")
            every { kmpTorService.state } returns MutableStateFlow(KmpTorService.TorState.Stopped(error))
            coEvery { kmpTorService.startTor() } returns false
            useCase = createUseCase()

            // When
            val result = useCase.execute(onionPairingQrCode)
            advanceUntilIdle()

            // Then
            assertFalse(result)
            val status = useCase.state.value.connectionStatus
            assertTrue(status is TrustedNodeConnectionStatus.Failed)
            assertEquals(
                "mobile.trustedNodeSetup.connectionJob.messages.connectionError".i18n("Tor failed to start"),
                status.displayString,
            )
        }

    @Test
    fun `when switching from Tor to clearnet then stops Tor after connection`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            setupSuccessfulWebSocketConnection()
            coEvery { kmpTorService.stopTor() } returns Unit
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify { kmpTorService.stopTor() }
        }

    @Test
    fun `when connection succeeds with Tor then keeps Tor running`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulTorConnection()
            setupSuccessfulPairing(onionPairingQrCode)
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(onionPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify(exactly = 0) { kmpTorService.stopTor() }
        }

    // ========== Pairing Credentials Tests ==========

    @Test
    fun `when clientId and sessionId exist then uses existing credentials`() =
        runTest(testDispatcher) {
            // Given
            sensitiveSettingsRepository.update {
                it.copy(
                    clientId = "existing-client",
                    sessionId = "existing-session",
                    bisqApiUrl = clearnetPairingQrCode.restApiUrl,
                )
            }
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify(exactly = 0) { apiAccessService.requestPairing(any()) }
        }

    @Test
    fun `when clientId missing then requests new pairing`() =
        runTest(testDispatcher) {
            // Given
            sensitiveSettingsRepository.update {
                it.copy(clientId = null, sessionId = "session")
            }
            coEvery { apiAccessService.requestPairing(any()) } returns
                Result.success(
                    PairingResponse(
                        version = PairingCode.VERSION,
                        clientId = "new-client",
                        clientSecret = "new-secret",
                        sessionId = "new-session",
                        sessionExpiryDate = Clock.System.now().toEpochMilliseconds(),
                    ),
                )
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify { apiAccessService.requestPairing(clearnetPairingQrCode) }
        }

    @Test
    fun `when sessionId missing then requests new pairing`() =
        runTest(testDispatcher) {
            // Given
            sensitiveSettingsRepository.update {
                it.copy(clientId = "client", sessionId = null)
            }
            coEvery { apiAccessService.requestPairing(any()) } returns
                Result.success(
                    PairingResponse(
                        version = PairingCode.VERSION,
                        clientId = "new-client",
                        clientSecret = "new-secret",
                        sessionId = "new-session",
                        sessionExpiryDate = Clock.System.now().toEpochMilliseconds(),
                    ),
                )
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify { apiAccessService.requestPairing(clearnetPairingQrCode) }
        }

    @Test
    fun `when API URL changed then requests new pairing`() =
        runTest(testDispatcher) {
            // Given
            sensitiveSettingsRepository.update {
                it.copy(
                    clientId = "client",
                    sessionId = "session",
                    bisqApiUrl = "http://different-url.com:8080",
                )
            }
            coEvery { apiAccessService.requestPairing(any()) } returns
                Result.success(
                    PairingResponse(
                        version = PairingCode.VERSION,
                        clientId = "new-client",
                        clientSecret = "new-secret",
                        sessionId = "new-session",
                        sessionExpiryDate = Clock.System.now().toEpochMilliseconds(),
                    ),
                )
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            coVerify { apiAccessService.requestPairing(clearnetPairingQrCode) }
        }

    @Test
    fun `when pairing request fails then returns false with error status`() =
        runTest(testDispatcher) {
            // Given
            sensitiveSettingsRepository.update {
                it.copy(clientId = null, sessionId = null)
            }
            coEvery { apiAccessService.requestPairing(any()) } returns
                Result.failure(Exception("Pairing failed"))
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertFalse(result)
            val status = useCase.state.value.connectionStatus
            assertTrue(status is TrustedNodeConnectionStatus.Failed)
            assertEquals(
                "mobile.trustedNodeSetup.status.pairingRequestFailed".i18n(),
                status.displayString,
            )
        }

    // ========== Connection Flow Tests ==========

    @Test
    fun `when all steps succeed then returns true and sets Connected status`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertTrue(result)
            assertEquals(TrustedNodeConnectionStatus.Connected, useCase.state.value.connectionStatus)
            coVerify { wsClientService.testConnection(any(), any(), any(), any(), any(), any(), any()) }
            coVerify { wsClientService.connect() }
        }

    @Test
    fun `when testConnection fails then returns false with error`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            coEvery {
                wsClientService.testConnection(any(), any(), any(), any(), any(), any(), any())
            } returns Exception("Connection test failed")
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertFalse(result)
            val status = useCase.state.value.connectionStatus
            assertTrue(status is TrustedNodeConnectionStatus.Failed)
            assertEquals(
                "mobile.trustedNodeSetup.connectionJob.messages.connectionError".i18n("Connection test failed"),
                status.displayString,
            )
            coVerify(exactly = 0) { wsClientService.connect() }
        }

    @Test
    fun `when connect fails then returns false with error`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            coEvery {
                wsClientService.testConnection(any(), any(), any(), any(), any(), any(), any())
            } returns null
            coEvery { wsClientService.connect() } returns Exception("Connect failed")
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertFalse(result)
            val status = useCase.state.value.connectionStatus
            assertTrue(status is TrustedNodeConnectionStatus.Failed)
            assertEquals(
                "mobile.trustedNodeSetup.connectionJob.messages.connectionError".i18n("Connect failed"),
                status.displayString,
            )
        }

    // ========== Error Handling Tests ==========

    @Test
    fun `when TimeoutCancellationException then sets timeout failed status`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            val timeoutException =
                try {
                    withTimeout(1) { delay(100) }
                    null
                } catch (e: TimeoutCancellationException) {
                    e
                }
            coEvery {
                wsClientService.testConnection(any(), any(), any(), any(), any(), any(), any())
            } returns timeoutException
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertFalse(result)
            val status = useCase.state.value.connectionStatus
            assertTrue(status is TrustedNodeConnectionStatus.Failed)
            assertEquals(
                "mobile.trustedNodeSetup.connectionJob.messages.connectionTimedOut".i18n(),
                status.displayString,
            )
        }

    @Test
    fun `when IncompatibleHttpApiVersionException then sets incompatible version status with server version`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            coEvery {
                wsClientService.testConnection(any(), any(), any(), any(), any(), any(), any())
            } returns IncompatibleHttpApiVersionException("2.0.0")
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertFalse(result)
            assertEquals(
                TrustedNodeConnectionStatus.IncompatibleHttpApiVersion,
                useCase.state.value.connectionStatus,
            )
            assertEquals("2.0.0", useCase.state.value.serverVersion)
        }

    @Test
    fun `when UnauthorizedApiAccessException then sets password incorrect status`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            coEvery {
                wsClientService.testConnection(any(), any(), any(), any(), any(), any(), any())
            } returns UnauthorizedApiAccessException()
            useCase = createUseCase()

            // When
            val result = useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertFalse(result)
            val status = useCase.state.value.connectionStatus
            assertTrue(status is TrustedNodeConnectionStatus.Failed)
            assertEquals(
                "mobile.trustedNodeSetup.status.passwordIncorrectOrMissing".i18n(),
                status.displayString,
            )
        }

    // ========== State Update Tests ==========

    @Test
    fun `when execute starts then sets SettingUpConnection status`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // Collect state changes in background
            val stateHistory = mutableListOf<TrustedNodeConnectionStatus>()
            val job =
                backgroundScope.launch {
                    useCase.state.collect { state ->
                        stateHistory.add(state.connectionStatus)
                    }
                }

            // When
            useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()
            job.cancel()

            // Then
            assertTrue(stateHistory.contains(TrustedNodeConnectionStatus.SettingUpConnection))
        }

    @Test
    fun `when Tor starts then sets StartingTor status`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulTorConnection()
            setupSuccessfulPairing(onionPairingQrCode)
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // Collect state changes in background
            val stateHistory = mutableListOf<TrustedNodeConnectionStatus>()
            val job =
                backgroundScope.launch {
                    useCase.state.collect { state ->
                        stateHistory.add(state.connectionStatus)
                    }
                }

            // When
            useCase.execute(onionPairingQrCode)
            advanceUntilIdle()
            job.cancel()

            // Then
            assertTrue(stateHistory.contains(TrustedNodeConnectionStatus.StartingTor))
            coVerify { kmpTorService.startTor() }
        }

    @Test
    fun `when requesting pairing then sets RequestingPairing status`() =
        runTest(testDispatcher) {
            // Given
            sensitiveSettingsRepository.update { it.copy(clientId = null, sessionId = null) }
            coEvery { apiAccessService.requestPairing(any()) } coAnswers {
                delay(10) // Small delay to make state changes observable
                Result.success(
                    PairingResponse(
                        version = PairingCode.VERSION,
                        clientId = "client",
                        clientSecret = "secret",
                        sessionId = "session",
                        sessionExpiryDate = Clock.System.now().toEpochMilliseconds(),
                    ),
                )
            }
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // Collect state changes in background
            val stateHistory = mutableListOf<TrustedNodeConnectionStatus>()
            val job =
                backgroundScope.launch {
                    useCase.state.collect { state ->
                        stateHistory.add(state.connectionStatus)
                    }
                }

            // When
            useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()
            job.cancel()

            // Then
            assertTrue(stateHistory.contains(TrustedNodeConnectionStatus.RequestingPairing))
            coVerify { apiAccessService.requestPairing(clearnetPairingQrCode) }
        }

    @Test
    fun `when testing connection then sets Connecting status`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // Collect state changes in background
            val stateHistory = mutableListOf<TrustedNodeConnectionStatus>()
            val job =
                backgroundScope.launch {
                    useCase.state.collect { state ->
                        stateHistory.add(state.connectionStatus)
                    }
                }

            // When
            useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()
            job.cancel()

            // Then
            assertTrue(stateHistory.contains(TrustedNodeConnectionStatus.Connecting))
            coVerify { wsClientService.testConnection(any(), any(), any(), any(), any(), any(), any()) }
        }

    @Test
    fun `when connection succeeds then sets Connected status`() =
        runTest(testDispatcher) {
            // Given
            setupSuccessfulPairing()
            setupSuccessfulWebSocketConnection()
            useCase = createUseCase()

            // When
            useCase.execute(clearnetPairingQrCode)
            advanceUntilIdle()

            // Then
            assertEquals(TrustedNodeConnectionStatus.Connected, useCase.state.value.connectionStatus)
        }

    // ========== Helper Methods ==========

    private fun setupSuccessfulTorConnection() {
        val bootstrapFlow = MutableStateFlow(0)
        coEvery { kmpTorService.startTor() } coAnswers {
            delay(10) // Small delay to make state changes observable
            bootstrapFlow.value = 100
            true
        }
        every { kmpTorService.bootstrapProgress } returns bootstrapFlow
        coEvery { kmpTorService.awaitSocksPort() } coAnswers {
            delay(10) // Small delay to make state changes observable
            9050
        }
        val httpClientFlow = MutableSharedFlow<HttpClientSettings>(replay = 1)
        every { httpClientService.httpClientChangedFlow } returns httpClientFlow
        httpClientFlow.tryEmit(
            HttpClientSettings(
                bisqApiUrl = null,
                tlsFingerprint = null,
                selectedProxyOption = BisqProxyOption.INTERNAL_TOR,
            ),
        )
    }

    private suspend fun setupSuccessfulPairing(pairingQrCode: PairingQrCode = clearnetPairingQrCode) {
        sensitiveSettingsRepository.update {
            it.copy(
                clientId = "test-client",
                sessionId = "test-session",
                bisqApiUrl = pairingQrCode.restApiUrl,
            )
        }
    }

    private fun setupSuccessfulWebSocketConnection() {
        val connectionStateFlow = MutableStateFlow<ConnectionState>(ConnectionState.Disconnected())
        coEvery {
            wsClientService.testConnection(any(), any(), any(), any(), any(), any(), any())
        } coAnswers {
            delay(10) // Small delay to make state changes observable
            connectionStateFlow.value = ConnectionState.Connecting
            null
        }
        coEvery { wsClientService.connect() } coAnswers {
            delay(10) // Small delay to make state changes observable
            connectionStateFlow.value = ConnectionState.Connected
            null
        }
        every { wsClientService.connectionState } returns connectionStateFlow
    }
}
