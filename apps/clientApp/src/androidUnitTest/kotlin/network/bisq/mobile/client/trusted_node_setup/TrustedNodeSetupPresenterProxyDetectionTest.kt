package network.bisq.mobile.client.trusted_node_setup

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.client.common.domain.access.ApiAccessService
import network.bisq.mobile.client.common.domain.httpclient.BisqProxyOption
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepository
import network.bisq.mobile.client.common.domain.sensitive_settings.SensitiveSettingsRepositoryMock
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.test_utils.KoinIntegrationTestBase
import network.bisq.mobile.client.common.test_utils.UserRepositoryMock
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.network.KmpTorService
import network.bisq.mobile.presentation.main.MainPresenter
import org.junit.Assert.assertEquals
import org.junit.Test
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Tests for automatic proxy detection based on URL patterns.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TrustedNodeSetupPresenterProxyDetectionTest : KoinIntegrationTestBase() {
    private val wsClientService: WebSocketClientService = mockk(relaxed = true)
    private val apiAccessService: ApiAccessService = mockk(relaxed = true)
    private val kmpTorService: KmpTorService = mockk(relaxed = true)
    private val appBootstrap: ApplicationBootstrapFacade = mockk(relaxed = true)
    private val mainPresenter: MainPresenter = mockk(relaxed = true)
    private val userRepository: UserRepository = UserRepositoryMock()
    private val sensitiveSettingsRepository: SensitiveSettingsRepository = SensitiveSettingsRepositoryMock()

    override fun additionalModules(): List<Module> =
        listOf(
            module {
                single<WebSocketClientService> { wsClientService }
                single<ApiAccessService> { apiAccessService }
                single<KmpTorService> { kmpTorService }
                single<ApplicationBootstrapFacade> { appBootstrap }
            },
        )

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
