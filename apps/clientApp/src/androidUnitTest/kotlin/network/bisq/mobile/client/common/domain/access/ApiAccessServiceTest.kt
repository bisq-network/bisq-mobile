package network.bisq.mobile.client.common.domain.access

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.commonTestModule
import network.bisq.mobile.client.common.domain.access.pairing.PairingCode
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.domain.data.EnvironmentController
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ApiAccessServiceTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var pairingService: PairingService
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var pairingQrCodeDecoder: PairingQrCodeDecoder
    private lateinit var apiAccessService: ApiAccessService

    // Fake repository for testing
    private val settingsFlow = MutableStateFlow(SensitiveSettings())

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        // Start Koin with test module for ServiceFacade dependencies
        startKoin {
            modules(commonTestModule)
        }

        pairingService = mockk(relaxed = true)
        pairingQrCodeDecoder = mockk(relaxed = true)

        // Create a fake repository
        sensitiveSettingsRepository =
            object : SensitiveSettingsRepository {
                override val data = settingsFlow

                override suspend fun update(transform: suspend (SensitiveSettings) -> SensitiveSettings) {
                    settingsFlow.value = transform(settingsFlow.value)
                }

                override suspend fun clear() {
                    settingsFlow.value = SensitiveSettings()
                }
            }

        val environmentController =
            mockk<EnvironmentController> {
                every { isSimulator() } returns false
            }
        apiAccessService =
            ApiAccessService(
                pairingService,
                sensitiveSettingsRepository,
                pairingQrCodeDecoder,
                environmentController,
            )
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // ========== getPairingCodeQr() Tests ==========

    @Test
    fun `getPairingCodeQr with demo pairing code returns demo PairingQrCode`() =
        runTest {
            val result = apiAccessService.getPairingCodeQr(DEMO_PAIRING_CODE)

            assertTrue(result.isSuccess)
            val pairingQrCode = result.getOrThrow()
            assertEquals(DEMO_WS_URL, pairingQrCode.webSocketUrl)
            assertEquals(DEMO_API_URL, pairingQrCode.restApiUrl)
            assertEquals(Permission.entries.toSet(), pairingQrCode.pairingCode.grantedPermissions)
        }

    @Test
    fun `getPairingCodeQr with demo pairing code with whitespace returns demo PairingQrCode`() =
        runTest {
            val result = apiAccessService.getPairingCodeQr("  $DEMO_PAIRING_CODE  ")

            assertTrue(result.isSuccess)
            val pairingQrCode = result.getOrThrow()
            assertEquals(DEMO_API_URL, pairingQrCode.restApiUrl)
        }

    @Test
    fun `getPairingCodeQr with valid code uses decoder`() =
        runTest {
            val validCode = "VALID_PAIRING_CODE"
            val expectedPairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode =
                        PairingCode(
                            id = "test-id",
                            expiresAt = kotlinx.datetime.Instant.DISTANT_FUTURE,
                            grantedPermissions = setOf(Permission.OFFERBOOK),
                        ),
                    webSocketUrl = "ws://test.com:8090",
                    restApiUrl = "http://test.com:8090",
                    tlsFingerprint = null,
                    torClientAuthSecret = null,
                )
            coEvery { pairingQrCodeDecoder.decode(validCode) } returns expectedPairingQrCode

            val result = apiAccessService.getPairingCodeQr(validCode)

            assertTrue(result.isSuccess)
            assertEquals(expectedPairingQrCode, result.getOrThrow())
        }

    @Test
    fun `getPairingCodeQr with invalid code returns failure`() =
        runTest {
            val invalidCode = "INVALID_CODE"
            coEvery { pairingQrCodeDecoder.decode(invalidCode) } throws Exception("Invalid code")

            val result = apiAccessService.getPairingCodeQr(invalidCode)

            assertTrue(result.isFailure)
        }

    // ========== requestPairing() Tests ==========

    @Test
    fun `requestPairing with demo PairingQrCode returns demo response`() =
        runTest {
            // Get demo PairingQrCode using the public API
            val demoPairingQrCodeResult = apiAccessService.getPairingCodeQr(DEMO_PAIRING_CODE)
            assertTrue(demoPairingQrCodeResult.isSuccess)
            val demoPairingQrCode = demoPairingQrCodeResult.getOrThrow()

            val result = apiAccessService.requestPairing(demoPairingQrCode)

            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertTrue(response.clientId.isNotBlank())
            assertTrue(response.clientSecret.isNotBlank())
            assertTrue(response.sessionId.isNotBlank())
            assertEquals(Long.MAX_VALUE, response.sessionExpiryDate)
        }

    @Test
    fun `requestPairing with regular PairingQrCode calls pairingService`() =
        runTest {
            val regularPairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode =
                        PairingCode(
                            id = "regular-id",
                            expiresAt = kotlinx.datetime.Instant.DISTANT_FUTURE,
                            grantedPermissions = setOf(Permission.OFFERBOOK),
                        ),
                    webSocketUrl = "ws://test.com:8090",
                    restApiUrl = "http://test.com:8090",
                    tlsFingerprint = null,
                    torClientAuthSecret = null,
                )
            val expectedResponse =
                PairingResponse(
                    version = 1,
                    clientId = "client-123",
                    clientSecret = "secret-456",
                    sessionId = "session-789",
                    sessionExpiryDate = 1234567890L,
                )
            coEvery { pairingService.requestPairing(any(), any()) } returns Result.success(expectedResponse)

            val result = apiAccessService.requestPairing(regularPairingQrCode)

            assertTrue(result.isSuccess)
            assertEquals(expectedResponse, result.getOrThrow())
        }

    @Test
    fun `requestPairing with failing pairingService returns failure`() =
        runTest {
            val regularPairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode =
                        PairingCode(
                            id = "regular-id",
                            expiresAt = kotlinx.datetime.Instant.DISTANT_FUTURE,
                            grantedPermissions = setOf(Permission.OFFERBOOK),
                        ),
                    webSocketUrl = "ws://test.com:8090",
                    restApiUrl = "http://test.com:8090",
                    tlsFingerprint = null,
                    torClientAuthSecret = null,
                )
            val expectedError = Exception("Pairing service failed")
            coEvery { pairingService.requestPairing(any(), any()) } returns Result.failure(expectedError)

            val result = apiAccessService.requestPairing(regularPairingQrCode)

            assertTrue(result.isFailure)
            assertEquals(expectedError, result.exceptionOrNull())
        }

    // ========== updateSettings() Tests ==========

    @Test
    fun `updateSettings updates repository with PairingQrCode data`() =
        runTest {
            val pairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode =
                        PairingCode(
                            id = "test-id",
                            expiresAt = kotlinx.datetime.Instant.DISTANT_FUTURE,
                            grantedPermissions = setOf(Permission.OFFERBOOK),
                        ),
                    webSocketUrl = "ws://test.com:8090",
                    restApiUrl = "http://test.com:8090",
                    tlsFingerprint = "fingerprint123",
                    torClientAuthSecret = null,
                )

            apiAccessService.updateSettings(pairingQrCode)

            val settings = sensitiveSettingsRepository.fetch()
            assertEquals("http://test.com:8090", settings.bisqApiUrl)
            assertEquals("fingerprint123", settings.tlsFingerprint)
        }

    @Test
    fun `updateSettings clears old credentials`() =
        runTest {
            // Given: existing credentials in settings
            settingsFlow.value =
                SensitiveSettings(
                    clientId = "old-client-id",
                    sessionId = "old-session-id",
                    clientSecret = "old-secret",
                )

            val pairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode =
                        PairingCode(
                            id = "test-id",
                            expiresAt = kotlinx.datetime.Instant.DISTANT_FUTURE,
                            grantedPermissions = emptySet(),
                        ),
                    webSocketUrl = "ws://test.com:8090",
                    restApiUrl = "http://test.com:8090",
                    tlsFingerprint = null,
                    torClientAuthSecret = null,
                )

            apiAccessService.updateSettings(pairingQrCode)

            val settings = sensitiveSettingsRepository.fetch()
            assertNull(settings.clientId)
            assertNull(settings.sessionId)
            assertNull(settings.clientSecret)
        }

    @Test
    fun `updateSettings with blank URL does not update repository`() =
        runTest {
            val pairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode =
                        PairingCode(
                            id = "test-id",
                            expiresAt = kotlinx.datetime.Instant.DISTANT_FUTURE,
                            grantedPermissions = emptySet(),
                        ),
                    webSocketUrl = "",
                    restApiUrl = "",
                    tlsFingerprint = null,
                    torClientAuthSecret = null,
                )
            val originalSettings = sensitiveSettingsRepository.fetch()

            apiAccessService.updateSettings(pairingQrCode)

            val settings = sensitiveSettingsRepository.fetch()
            assertEquals(originalSettings.bisqApiUrl, settings.bisqApiUrl)
        }
}
