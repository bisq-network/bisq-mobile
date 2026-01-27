package network.bisq.mobile.client.trusted_node_setup

import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.client.common.di.clientTestModule
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettings
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module

/**
 * Tests for automatic proxy detection based on URL patterns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrustedNodeSetupPresenterProxyDetectionTest {
    private val testDispatcher = StandardTestDispatcher()
    private lateinit var wsClientService: WebSocketClientService
    private lateinit var apiAccessService: ApiAccessService
    private lateinit var kmpTorService: KmpTorService
    private lateinit var appBootstrap: ApplicationBootstrapFacade
    private lateinit var mainPresenter: MainPresenter
    private lateinit var userRepository: UserRepository
    private lateinit var sensitiveSettingsRepository: SensitiveSettingsRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)

        // Mocks
        wsClientService = mockk(relaxed = true)
        apiAccessService = mockk(relaxed = true)
        kmpTorService = mockk(relaxed = true)
        appBootstrap = mockk(relaxed = true)
        mainPresenter = mockk(relaxed = true)

        sensitiveSettingsRepository =
            object : SensitiveSettingsRepository {
                private val _data = MutableStateFlow(SensitiveSettings())
                override val data = _data

                override suspend fun fetch(): SensitiveSettings = _data.value

                override suspend fun update(transform: suspend (t: SensitiveSettings) -> SensitiveSettings) {
                    _data.value = transform(_data.value)
                }

                override suspend fun clear() {
                    _data.value = SensitiveSettings()
                }
            }

        userRepository =
            object : UserRepository {
                private val _data = MutableStateFlow(User())
                override val data = _data

                override suspend fun updateTerms(value: String) {}

                override suspend fun updateStatement(value: String) {}

                override suspend fun update(value: User) {
                    _data.value = value
                }

                override suspend fun clear() {
                    _data.value = User()
                }
            }

        startKoin {
            modules(
                clientTestModule,
                module {
                    single<WebSocketClientService> { wsClientService }
                    single<ApiAccessService> { apiAccessService }
                    single<KmpTorService> { kmpTorService }
                    single<ApplicationBootstrapFacade> { appBootstrap }
                },
            )
        }
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
        stopKoin()
    }

    @Test
    fun `onion URL automatically enables INTERNAL_TOR`() =
        runTest {
            val presenter =
                TrustedNodeSetupPresenter(
                    mainPresenter,
                    userRepository,
                    sensitiveSettingsRepository,
                    kmpTorService,
                    appBootstrap,
                )

            // Initially NONE
            assertEquals(BisqProxyOption.NONE, presenter.selectedProxyOption.value)

            // Validate a .onion URL
            val onionUrl = "http://test1234567890123456789012345678901234567890123456.onion:8090"
            presenter.validateApiUrl(onionUrl, presenter.selectedProxyOption.value)

            // Should automatically switch to INTERNAL_TOR
            assertEquals(BisqProxyOption.INTERNAL_TOR, presenter.selectedProxyOption.value)
        }

    @Test
    fun `clearnet URL uses NONE proxy option`() =
        runTest {
            val presenter =
                TrustedNodeSetupPresenter(
                    mainPresenter,
                    userRepository,
                    sensitiveSettingsRepository,
                    kmpTorService,
                    appBootstrap,
                )

            // Initially NONE
            assertEquals(BisqProxyOption.NONE, presenter.selectedProxyOption.value)

            // Validate a clearnet URL
            val clearnetUrl = "https://example.com:8090"
            presenter.validateApiUrl(clearnetUrl, presenter.selectedProxyOption.value)

            // Should remain NONE
            assertEquals(BisqProxyOption.NONE, presenter.selectedProxyOption.value)
        }

    @Test
    fun `localhost URL uses NONE proxy option`() =
        runTest {
            val presenter =
                TrustedNodeSetupPresenter(
                    mainPresenter,
                    userRepository,
                    sensitiveSettingsRepository,
                    kmpTorService,
                    appBootstrap,
                )

            // Initially NONE
            assertEquals(BisqProxyOption.NONE, presenter.selectedProxyOption.value)

            // Validate localhost URL
            val localhostUrl = "http://localhost:8090"
            presenter.validateApiUrl(localhostUrl, presenter.selectedProxyOption.value)

            // Should remain NONE
            assertEquals(BisqProxyOption.NONE, presenter.selectedProxyOption.value)
        }
}
