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
import network.bisq.mobile.client.common.domain.access.pairing.PairingService
import network.bisq.mobile.client.common.domain.access.pairing.Permission
import network.bisq.mobile.client.common.domain.access.pairing.qr.PairingQrCodeDecoder
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.httpclient.HttpClientService
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
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
    private lateinit var httpClientService: HttpClientService
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
        httpClientService = mockk(relaxed = true)
        pairingQrCodeDecoder = mockk(relaxed = true)

        every { sensitiveSettingsRepository.data } returns flowOf(SensitiveSettings())
        coEvery { sensitiveSettingsRepository.fetch() } returns SensitiveSettings()

        apiAccessService =
            ApiAccessService(
                pairingService,
                sensitiveSettingsRepository,
                httpClientService,
                pairingQrCodeDecoder,
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
    fun `setPairingQrCodeString with demo code sets demo mode`() =
        runTest {
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()

            assertTrue(ApplicationBootstrapFacade.isDemo)
        }

    @Test
    fun `setPairingQrCodeString with whitespace-padded demo code sets demo mode`() =
        runTest {
            // Input is trimmed before comparison, so whitespace should be handled
            apiAccessService.setPairingQrCodeString("  $DEMO_PAIRING_CODE  ")
            advanceUntilIdle()

            assertTrue(ApplicationBootstrapFacade.isDemo)
            assertEquals(DEMO_API_URL, apiAccessService.restApiUrl.value)
        }

    @Test
    fun `setPairingQrCodeString with demo code sets correct API URL`() =
        runTest {
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()

            assertEquals(DEMO_API_URL, apiAccessService.restApiUrl.value)
            assertEquals(DEMO_WS_URL, apiAccessService.webSocketUrl.value)
        }

    @Test
    fun `setPairingQrCodeString with demo code sets fake credentials`() =
        runTest {
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()

            assertEquals("demo-client-id", apiAccessService.clientId.value)
            assertEquals("demo-client-secret", apiAccessService.clientSecret.value)
            assertEquals("demo-session-id", apiAccessService.sessionId.value)
            assertEquals("demo-pairing-id", apiAccessService.pairingCodeId.value)
        }

    @Test
    fun `setPairingQrCodeString with demo code grants all permissions`() =
        runTest {
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()

            assertEquals(Permission.entries.toSet(), apiAccessService.grantedPermissions.value)
        }

    @Test
    fun `setPairingQrCodeString with demo code clears TLS fingerprint`() =
        runTest {
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()

            assertNull(apiAccessService.tlsFingerprint.value)
        }

    @Test
    fun `setPairingQrCodeString with demo code clears pairing error`() =
        runTest {
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()

            assertNull(apiAccessService.pairingCodeError.value)
        }

    @Test
    fun `setPairingQrCodeString with demo code updates sensitive settings`() =
        runTest {
            val settingsSlot = slot<suspend (SensitiveSettings) -> SensitiveSettings>()
            coEvery { sensitiveSettingsRepository.update(capture(settingsSlot)) } returns Unit

            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()

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
    fun `setPairingQrCodeString with demo code sets pairing result stored`() =
        runTest {
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()

            assertTrue(apiAccessService.pairingResultStored.value)
        }

    @Test
    fun `setPairingQrCodeString with demo code sets successful pairing result`() =
        runTest {
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()

            val result = apiAccessService.pairingResult.value
            assertTrue(result != null && result.isSuccess)
            val response = result.getOrThrow()
            assertEquals("demo-client-id", response.clientId)
            assertEquals("demo-session-id", response.sessionId)
            assertEquals("demo-client-secret", response.clientSecret)
        }

    @Test
    fun `setPairingQrCodeString with real code clears demo mode when previously in demo`() =
        runTest {
            // First, enter demo mode
            apiAccessService.setPairingQrCodeString(DEMO_PAIRING_CODE)
            advanceUntilIdle()
            assertTrue(ApplicationBootstrapFacade.isDemo)

            // Now enter an invalid (but non-demo) pairing code - this should clear demo mode
            // even though the code itself will fail to parse
            apiAccessService.setPairingQrCodeString("some-invalid-real-code")
            advanceUntilIdle()

            // Demo mode should be cleared before attempting to parse
            assertFalse(ApplicationBootstrapFacade.isDemo)
        }
}
