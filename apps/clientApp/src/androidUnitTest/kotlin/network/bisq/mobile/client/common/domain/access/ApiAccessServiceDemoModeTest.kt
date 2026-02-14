package network.bisq.mobile.client.common.domain.access

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.domain.access.pairing.PairingResponse
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.domain.data.EnvironmentController
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ApiAccessServiceDemoModeTest {
    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var pairingService: PairingService
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository
    private lateinit var pairingQrCodeDecoder: PairingQrCodeDecoder
    private lateinit var apiAccessService: ApiAccessService

    // Test implementation of CoroutineJobsManager
    private val testJobsManager =
        object : CoroutineJobsManager {
            override suspend fun dispose() {}

            override fun getScope(): CoroutineScope = testScope

            override var coroutineExceptionHandler: ((Throwable) -> Unit)? = null
        }

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Start Koin with test module
        startKoin {
            modules(
                module {
                    single<CoroutineJobsManager> { testJobsManager }
                },
            )
        }

        pairingService = mockk(relaxed = true)
        sensitiveSettingsRepository = mockk(relaxed = true)
        pairingQrCodeDecoder = mockk(relaxed = true)

        every { sensitiveSettingsRepository.data } returns flowOf(SensitiveSettings())
        coEvery { sensitiveSettingsRepository.fetch() } returns SensitiveSettings()

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
        // Reset demo mode before each test
        ApplicationBootstrapFacade.isDemo = false
    }

    @After
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
        ApplicationBootstrapFacade.isDemo = false
    }

    @Test
    fun `demo pairing code constant is correct`() {
        assertEquals("BISQ_DEMO_PAIRING_CODE", DEMO_PAIRING_CODE)
    }

    @Test
    fun `demo API URL constant is correct`() {
        assertEquals("http://demo.bisq:21", DEMO_API_URL)
    }

    @Test
    fun `getPairingCodeQr with demo code returns demo PairingQrCode`() =
        runTest {
            val result = apiAccessService.getPairingCodeQr(DEMO_PAIRING_CODE)

            assertTrue(result.isSuccess)
            val code = result.getOrThrow()
            assertEquals(DEMO_API_URL, code.restApiUrl)
            assertEquals(DEMO_WS_URL, code.webSocketUrl)
            assertNull(code.tlsFingerprint)
            assertEquals(Permission.entries.toSet(), code.pairingCode.grantedPermissions)
        }

    @Test
    fun `getPairingCodeQr with whitespace-padded demo code returns demo PairingQrCode`() =
        runTest {
            val result = apiAccessService.getPairingCodeQr("  $DEMO_PAIRING_CODE  ")

            assertTrue(result.isSuccess)
            assertEquals(DEMO_API_URL, result.getOrThrow().restApiUrl)
        }

    @Test
    fun `requestPairing with demo PairingQrCode sets demo mode flag`() =
        runTest {
            val demoPairingQrCode = apiAccessService.getPairingCodeQr(DEMO_PAIRING_CODE).getOrThrow()

            apiAccessService.requestPairing(demoPairingQrCode)

            assertTrue(ApplicationBootstrapFacade.isDemo)
        }

    @Test
    fun `requestPairing with demo PairingQrCode returns correct response`() =
        runTest {
            val demoPairingQrCode = apiAccessService.getPairingCodeQr(DEMO_PAIRING_CODE).getOrThrow()

            val result = apiAccessService.requestPairing(demoPairingQrCode)

            assertTrue(result.isSuccess)
            val response = result.getOrThrow()
            assertEquals("demo-client-id", response.clientId)
            assertEquals("demo-session-id", response.sessionId)
            assertEquals("demo-client-secret", response.clientSecret)
            assertEquals(Long.MAX_VALUE, response.sessionExpiryDate)
        }

    @Test
    fun `requestPairing with demo PairingQrCode updates sensitive settings`() =
        runTest {
            val settingsSlot = slot<suspend (SensitiveSettings) -> SensitiveSettings>()
            coEvery { sensitiveSettingsRepository.update(capture(settingsSlot)) } returns Unit

            val demoPairingQrCode = apiAccessService.getPairingCodeQr(DEMO_PAIRING_CODE).getOrThrow()
            apiAccessService.requestPairing(demoPairingQrCode)

            coVerify { sensitiveSettingsRepository.update(any()) }

            // Verify the transform produces correct settings
            val transformedSettings = settingsSlot.captured.invoke(SensitiveSettings())
            assertEquals(DEMO_API_URL, transformedSettings.bisqApiUrl)
            assertEquals("demo-client-id", transformedSettings.clientId)
            assertEquals("demo-session-id", transformedSettings.sessionId)
            assertEquals("demo-client-secret", transformedSettings.clientSecret)
            assertEquals(BisqProxyOption.NONE, transformedSettings.selectedProxyOption)
            assertNull(transformedSettings.tlsFingerprint)
        }

    @Test
    fun `requestPairing with real code clears demo mode when previously in demo`() =
        runTest {
            // First, enter demo mode
            val demoPairingQrCode = apiAccessService.getPairingCodeQr(DEMO_PAIRING_CODE).getOrThrow()
            apiAccessService.requestPairing(demoPairingQrCode)
            assertTrue(ApplicationBootstrapFacade.isDemo)

            // Now pair with a real code - mock pairingService to return success
            val realResponse =
                PairingResponse(
                    version = 1,
                    clientId = "real-client",
                    clientSecret = "real-secret",
                    sessionId = "real-session",
                    sessionExpiryDate = 1234567890L,
                )
            coEvery { pairingService.requestPairing(any(), any()) } returns Result.success(realResponse)

            val realCode =
                apiAccessService.getPairingCodeQr("REAL_CODE").let {
                    // getPairingCodeQr will fail for non-demo code with relaxed mock,
                    // so we create a real PairingQrCode directly
                    network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCode(
                        version = 1,
                        pairingCode =
                            network.bisq.mobile.client.common.domain.access.pairing.PairingCode(
                                id = "real-id",
                                expiresAt = kotlinx.datetime.Instant.DISTANT_FUTURE,
                                grantedPermissions = setOf(Permission.OFFERBOOK),
                            ),
                        webSocketUrl = "ws://real.com:8090",
                        restApiUrl = "http://real.com:8090",
                        tlsFingerprint = null,
                        torClientAuthSecret = null,
                    )
                }

            apiAccessService.requestPairing(realCode)

            // Demo mode should be cleared
            assertFalse(ApplicationBootstrapFacade.isDemo)
        }
}
