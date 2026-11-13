package network.bisq.mobile.client.common.domain.service.user_profile

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.test.coroutines.StandardTestDispatcherProvider
import org.junit.Test

/**
 * Covers the user-activity republish of [ClientUserProfileServiceFacade]: its request runs in the
 * caller's scope and waits for a connection, so it must not hold up the facade's teardown.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientUserProfileServiceFacadeActivityTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: UserProfileApiGateway = mockk(relaxed = true)
    private val clientCatHashService: ClientCatHashService<PlatformImage> = mockk(relaxed = true)
    private val webSocketClientService: WebSocketClientService = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var facade: ClientUserProfileServiceFacade

    override fun onSetup() {
        every { webSocketClientService.connectionState } returns MutableStateFlow(ConnectionState.Connecting)
        facade = ClientUserProfileServiceFacade(apiGateway, clientCatHashService, json, webSocketClientService, StandardTestDispatcherProvider(testDispatcher))
    }

    @Test
    fun `deactivate completes while a user activity request waits for a connection`() =
        runTest {
            coEvery { apiGateway.triggerUserActivityDetection() } coAnswers { awaitCancellation() }
            facade.activate()
            backgroundScope.launch { facade.userActivityDetected() }
            testScheduler.runCurrent()

            // Hangs here if the request holds the lock clearState takes
            facade.deactivate()
        }

    @Test
    fun `a failed user activity request lets the next activity retry`() =
        runTest {
            coEvery { apiGateway.triggerUserActivityDetection() } returns Result.failure(IllegalStateException("offline"))

            facade.userActivityDetected()
            facade.userActivityDetected()

            coVerify(exactly = 2) { apiGateway.triggerUserActivityDetection() }
        }

    @Test
    fun `a successful user activity request throttles the next one`() =
        runTest {
            coEvery { apiGateway.triggerUserActivityDetection() } returns Result.success(Unit)

            facade.userActivityDetected()
            facade.userActivityDetected()

            coVerify(exactly = 1) { apiGateway.triggerUserActivityDetection() }
        }
}
