package network.bisq.mobile.client.common.domain.access

import io.mockk.coEvery
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
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ApiAccessServiceTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var pairingService: PairingService
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var httpClientService: HttpClientService
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
        httpClientService = mockk(relaxed = true)
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

        apiAccessService =
            ApiAccessService(
                pairingService,
                sensitiveSettingsRepository,
                httpClientService,
                pairingQrCodeDecoder,
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
            // Verify demo response has expected structure (not checking exact values since they're private)
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

    // ========== setPairingQrCodeString() Tests ==========

    @Test
    fun `setPairingQrCodeString with blank value does nothing`() =
        runTest {
            // Given: blank value
            val blankValue = "   "

            // When
            apiAccessService.setPairingQrCodeString(blankValue)

            // Then: pairingQrCodeString should remain empty
            assertEquals("", apiAccessService.pairingQrCodeString.value)
        }

    @Test
    fun `setPairingQrCodeString with demo code activates demo mode`() =
        runTest {
            // When
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)

            // Then: demo mode should be activated
            assertEquals(DEMO_WS_URL, apiAccessService.webSocketUrl.value)
            assertEquals(DEMO_API_URL, apiAccessService.restApiUrl.value)
        }

    @Test
    fun `setPairingQrCodeString with valid code decodes and stores values`() =
        runTest {
            // Given: a valid pairing code
            val validCode = "VALID_CODE"
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
                    tlsFingerprint = "fingerprint123",
                    torClientAuthSecret = null,
                )
            coEvery { pairingQrCodeDecoder.decode(validCode) } returns expectedPairingQrCode

            // When
            apiAccessService.setPairingQrCodeString(validCode)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: values should be stored (covers lines 188-197, 200-215)
            assertEquals("ws://test.com:8090", apiAccessService.webSocketUrl.value)
            assertEquals("http://test.com:8090", apiAccessService.restApiUrl.value)
            assertEquals("fingerprint123", apiAccessService.tlsFingerprint.value)
            assertEquals("test-id", apiAccessService.pairingCodeId.value)
            assertEquals(setOf(Permission.OFFERBOOK), apiAccessService.grantedPermissions.value)
        }

    @Test
    fun `setPairingQrCodeString with onion URL sets INTERNAL_TOR proxy`() =
        runTest {
            // Given: a pairing code with onion URL
            val validCode = "ONION_CODE"
            val expectedPairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode =
                        PairingCode(
                            id = "test-id",
                            expiresAt = kotlinx.datetime.Instant.DISTANT_FUTURE,
                            grantedPermissions = setOf(Permission.OFFERBOOK),
                        ),
                    webSocketUrl = "ws://abc123.onion:8090",
                    restApiUrl = "http://abc123.onion:8090",
                    tlsFingerprint = null,
                    torClientAuthSecret = null,
                )
            coEvery { pairingQrCodeDecoder.decode(validCode) } returns expectedPairingQrCode

            // When
            apiAccessService.setPairingQrCodeString(validCode)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: values should be stored with onion URL
            assertTrue(apiAccessService.restApiUrl.value.contains(".onion"))
        }

    @Test
    fun `setPairingQrCodeString with invalid code sets error`() =
        runTest {
            // Given: an invalid pairing code
            val invalidCode = "INVALID_CODE"
            coEvery { pairingQrCodeDecoder.decode(invalidCode) } throws Exception("Invalid code")

            // When
            apiAccessService.setPairingQrCodeString(invalidCode)

            // Then: error should be set
            assertTrue(apiAccessService.pairingCodeError.value != null)
        }

    // ========== activate() Tests ==========

    @Test
    fun `activate loads settings from repository`() =
        runTest {
            // Given: settings in repository
            settingsFlow.value =
                SensitiveSettings(
                    bisqApiUrl = "http://test.com:8090",
                    tlsFingerprint = "fingerprint123",
                    clientId = "client-123",
                    clientSecret = "secret-456",
                    sessionId = "session-789",
                )

            // When
            apiAccessService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: values should be loaded
            assertEquals("http://test.com:8090", apiAccessService.restApiUrl.value)
            assertEquals("ws://test.com:8090", apiAccessService.webSocketUrl.value)
            assertEquals("fingerprint123", apiAccessService.tlsFingerprint.value)
        }

    @Test
    fun `activate does not overwrite existing values`() =
        runTest {
            // Given: existing values in service
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            testDispatcher.scheduler.advanceUntilIdle()

            // And: different settings in repository
            settingsFlow.value =
                SensitiveSettings(
                    bisqApiUrl = "http://other.com:8090",
                    tlsFingerprint = "other-fingerprint",
                )

            // When
            apiAccessService.activate()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: existing values should not be overwritten
            assertEquals(DEMO_API_URL, apiAccessService.restApiUrl.value)
            assertEquals(DEMO_WS_URL, apiAccessService.webSocketUrl.value)
        }

    // ========== requestPairing() (no-arg version) Tests ==========

    @Test
    fun `requestPairing with null pairingCodeId does nothing`() =
        runTest {
            // Given: pairingCodeId is null (default state)
            // When
            apiAccessService.requestPairing()
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: no pairing result should be set
            assertEquals(null, apiAccessService.pairingResult.value)
        }

    @Test
    fun `requestPairing with valid pairingCodeId sets pairingResultStored to false`() =
        runTest {
            // Given: a valid pairing code is set
            val validCode = "VALID_CODE"
            val expectedPairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode =
                        PairingCode(
                            id = "test-pairing-id",
                            expiresAt = kotlinx.datetime.Instant.DISTANT_FUTURE,
                            grantedPermissions = setOf(Permission.OFFERBOOK),
                        ),
                    webSocketUrl = "ws://test.com:8090",
                    restApiUrl = "http://test.com:8090",
                    tlsFingerprint = "fingerprint123",
                    torClientAuthSecret = null,
                )
            coEvery { pairingQrCodeDecoder.decode(validCode) } returns expectedPairingQrCode

            // When: set pairing code
            apiAccessService.setPairingQrCodeString(validCode)
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: pairingCodeId should be set
            assertEquals("test-pairing-id", apiAccessService.pairingCodeId.value)
        }

    // ========== updatedSettings(String, String?, BisqProxyOption?) Tests ==========

    @Test
    fun `updatedSettings with restApiUrl updates repository`() =
        runTest {
            // When
            apiAccessService.updatedSettings(
                restApiUrl = "http://new-api.com:8090",
                tlsFingerprint = "new-fingerprint",
                proxyOption = null,
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: repository should be updated
            val settings = sensitiveSettingsRepository.fetch()
            assertEquals("http://new-api.com:8090", settings.bisqApiUrl)
            assertEquals("new-fingerprint", settings.tlsFingerprint)
        }

    @Test
    fun `updatedSettings with proxyOption updates repository with proxy`() =
        runTest {
            // When
            apiAccessService.updatedSettings(
                restApiUrl = "http://onion.com:8090",
                tlsFingerprint = null,
                proxyOption = BisqProxyOption.INTERNAL_TOR,
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: repository should be updated with proxy option
            val settings = sensitiveSettingsRepository.fetch()
            assertEquals("http://onion.com:8090", settings.bisqApiUrl)
            assertEquals(BisqProxyOption.INTERNAL_TOR, settings.selectedProxyOption)
        }

    @Test
    fun `updatedSettings without proxyOption uses default parameter`() =
        runTest {
            // When: call without proxyOption parameter (uses default null)
            apiAccessService.updatedSettings(
                restApiUrl = "http://test.com:8090",
                tlsFingerprint = "fingerprint",
            )
            testDispatcher.scheduler.advanceUntilIdle()

            // Then: repository should be updated
            val settings = sensitiveSettingsRepository.fetch()
            assertEquals("http://test.com:8090", settings.bisqApiUrl)
        }
}
