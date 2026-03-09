package network.bisq.mobile.client.common.domain.httpclient

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.domain.utils.VersionProvider
import org.koin.core.module.Module
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class HttpClientServiceAwaitClientReadyTest : KoinIntegrationTestBase() {
    private lateinit var kmpTorService: KmpTorService
    private lateinit var settingsRepository: SensitiveSettingsRepository
    private lateinit var versionProvider: VersionProvider

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<KmpTorService> {
                    mockk<KmpTorService>(relaxed = true).also {
                        every { it.state } returns MutableStateFlow(KmpTorService.TorState.Stopped())
                    }
                }
            },
        )

    override fun onSetup() {
        kmpTorService = mockk(relaxed = true)
        every { kmpTorService.state } returns MutableStateFlow(KmpTorService.TorState.Stopped())

        settingsRepository =
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

        versionProvider =
            object : VersionProvider {
                override fun getVersionInfo(
                    isDemo: Boolean,
                    isIOS: Boolean,
                ) = "test/1.0"

                override fun getAppNameAndVersion(
                    isDemo: Boolean,
                    isIOS: Boolean,
                ) = "test/1.0"
            }
    }

    private fun createService(): HttpClientService =
        HttpClientService(
            kmpTorService = kmpTorService,
            sensitiveSettingsRepository = settingsRepository,
            json = Json { ignoreUnknownKeys = true },
            versionProvider = versionProvider,
            defaultHost = "127.0.0.1",
            defaultPort = 8090,
        )

    @Test
    fun `awaitClientReady returns false on timeout when no client is built`() =
        runTest(testDispatcher) {
            val service = createService()
            val result = service.awaitClientReady(timeoutMs = 100)
            assertFalse(result)
        }

    @Test
    fun `awaitClientReady does not return true from stale replay`() =
        runBlocking {
            val service = createService()

            service.activate()
            settingsRepository.update {
                it.copy(bisqApiUrl = "http://127.0.0.1:8090")
            }
            // Wait for collector on Dispatchers.Default to build the first client
            delay(200)

            // Now call awaitClientReady — should NOT return true from stale replay
            // because generation was already incremented before the call
            val result = service.awaitClientReady(timeoutMs = 200)
            service.deactivate()
            assertFalse(result)
        }

    @Test
    fun `awaitClientReady returns true when generation advances via activate`() =
        runBlocking {
            val service = createService()

            service.activate()

            val job =
                launch {
                    // Small delay so settings update triggers after awaitClientReady starts
                    delay(50)
                    settingsRepository.update {
                        it.copy(bisqApiUrl = "http://127.0.0.1:8090")
                    }
                }

            val result = service.awaitClientReady(timeoutMs = 5000)
            job.join()
            service.deactivate()
            assertTrue(result)
        }

    @Test
    fun `awaitClientReady returns true when new client is built after initial one`() =
        runBlocking {
            val service = createService()

            service.activate()
            settingsRepository.update {
                it.copy(bisqApiUrl = "http://127.0.0.1:8090")
            }
            // Wait for first client to be built
            delay(200)

            val job =
                launch {
                    delay(50)
                    settingsRepository.update {
                        it.copy(bisqApiUrl = "http://127.0.0.1:9090")
                    }
                }

            val result = service.awaitClientReady(timeoutMs = 5000)
            job.join()
            service.deactivate()
            assertTrue(result)
        }

    @Test
    fun `recreateClient increments generation and unblocks awaitClientReady`() =
        runBlocking {
            val service = createService()

            service.activate()
            settingsRepository.update {
                it.copy(bisqApiUrl = "http://127.0.0.1:8090")
            }
            // Wait for first client to be built
            delay(200)

            val job =
                launch {
                    delay(50)
                    service.recreateClient()
                }

            val result = service.awaitClientReady(timeoutMs = 5000)
            job.join()
            service.deactivate()
            assertTrue(result)
        }
}
