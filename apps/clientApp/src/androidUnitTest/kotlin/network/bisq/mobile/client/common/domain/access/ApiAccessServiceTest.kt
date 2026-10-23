package network.bisq.mobile.client.common.domain.access

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import network.bisq.mobile.client.common.domain.access.pairing.PairingCode
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.client.common.domain.service.push_notification.TrustedNodePushRelease
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.utils.EnvironmentController
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

@OptIn(ExperimentalCoroutinesApi::class)
class ApiAccessServiceTest : ClientKoinIntegrationTestBase() {
    private val pairingService: PairingService = mockk(relaxed = true)
    private val pairingQrCodeDecoder: PairingQrCodeDecoder = mockk(relaxed = true)
    private val environmentController: EnvironmentController =
        mockk {
            every { isSimulator() } returns false
        }

    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepositoryMock
    private lateinit var pushRelease: RecordingPushRelease
    private lateinit var apiAccessService: ApiAccessService

    override fun onSetup() {
        sensitiveSettingsRepository = SensitiveSettingsRepositoryMock()
        pushRelease = RecordingPushRelease(sensitiveSettingsRepository)
        apiAccessService =
            ApiAccessService(
                pairingService,
                sensitiveSettingsRepository,
                pairingQrCodeDecoder,
                environmentController,
                pushRelease,
            )
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
                            expiresAt = Instant.DISTANT_FUTURE,
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
                            expiresAt = Instant.DISTANT_FUTURE,
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
                            expiresAt = Instant.DISTANT_FUTURE,
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
                            expiresAt = Instant.DISTANT_FUTURE,
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
            sensitiveSettingsRepository.update {
                SensitiveSettings(
                    clientId = "old-client-id",
                    sessionId = "old-session-id",
                    clientSecret = "old-secret",
                )
            }

            val pairingQrCode =
                PairingQrCode(
                    version = 1,
                    pairingCode =
                        PairingCode(
                            id = "test-id",
                            expiresAt = Instant.DISTANT_FUTURE,
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
                            expiresAt = Instant.DISTANT_FUTURE,
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

    // ========== Forgetting the paired node ==========

    private val pairedSettings = SensitiveSettings(bisqApiUrl = "http://old-node.onion:8090", clientId = "old-client", sessionId = "old-session")

    @Test
    fun `forgetTrustedNode releases push delivery before wiping the pairing`() =
        runTest {
            sensitiveSettingsRepository.update { pairedSettings }
            apiAccessService.activate()

            apiAccessService.forgetTrustedNode()
            advanceUntilIdle()

            assertEquals(listOf<String?>("old-client"), pushRelease.clientIdsAtRelease)
            assertEquals(SensitiveSettings(), sensitiveSettingsRepository.fetch())
        }

    @Test
    fun `forgetTrustedNode returns without waiting for the release`() =
        runTest {
            sensitiveSettingsRepository.update { pairedSettings }
            pushRelease.blockUntil = CompletableDeferred()
            apiAccessService.activate()

            apiAccessService.forgetTrustedNode()
            advanceUntilIdle()

            assertEquals(pairedSettings, sensitiveSettingsRepository.fetch(), "the pairing stays until the node was told")
            pushRelease.blockUntil?.complete(Unit)
            advanceUntilIdle()
            assertEquals(SensitiveSettings(), sensitiveSettingsRepository.fetch())
        }

    @Test
    fun `a forget cancelled by deactivation still wipes the pairing`() =
        runTest {
            sensitiveSettingsRepository.update { pairedSettings }
            pushRelease.blockUntil = CompletableDeferred()
            apiAccessService.activate()

            apiAccessService.forgetTrustedNode()
            advanceUntilIdle()
            apiAccessService.deactivate()
            advanceUntilIdle()

            assertEquals(SensitiveSettings(), sensitiveSettingsRepository.fetch())
        }

    @Test
    fun `releasePreviousPairing waits for a pending forget instead of releasing again`() =
        runTest {
            sensitiveSettingsRepository.update { pairedSettings }
            pushRelease.blockUntil = CompletableDeferred()
            apiAccessService.activate()
            apiAccessService.forgetTrustedNode()
            advanceUntilIdle()

            val release = launch { apiAccessService.releasePreviousPairing() }
            advanceUntilIdle()
            assertFalse(release.isCompleted, "pairing must not start before the forget has wiped the old pairing")

            pushRelease.blockUntil?.complete(Unit)
            advanceUntilIdle()

            assertTrue(release.isCompleted)
            assertEquals(1, pushRelease.clientIdsAtRelease.size)
            assertEquals(SensitiveSettings(), sensitiveSettingsRepository.fetch())
        }

    @Test
    fun `releasePreviousPairing releases a pairing that is about to be replaced`() =
        runTest {
            // "Pair with a new trusted node" from the setup dialogs pairs over the stored pairing
            // without forgetting it first.
            sensitiveSettingsRepository.update { pairedSettings }
            apiAccessService.activate()

            apiAccessService.releasePreviousPairing()

            assertEquals(listOf<String?>("old-client"), pushRelease.clientIdsAtRelease)
        }

    @Test
    fun `releasePreviousPairing does nothing when no node is paired`() =
        runTest {
            sensitiveSettingsRepository.update { SensitiveSettings(bisqApiUrl = "http://new-node.onion:8090") }
            apiAccessService.activate()

            apiAccessService.releasePreviousPairing()

            assertEquals(emptyList(), pushRelease.clientIdsAtRelease)
        }

    /** Records which pairing was still stored when the release ran. */
    private class RecordingPushRelease(
        private val sensitiveSettingsRepository: SensitiveSettingsRepositoryMock,
    ) : TrustedNodePushRelease {
        val clientIdsAtRelease = mutableListOf<String?>()
        var blockUntil: CompletableDeferred<Unit>? = null
        override val releaseFailures: SharedFlow<Unit> = MutableSharedFlow()

        override suspend fun releaseCurrentNode() {
            clientIdsAtRelease.add(sensitiveSettingsRepository.fetch().clientId)
            blockUntil?.await()
        }
    }
}
