package network.bisq.mobile.client.common.domain.service.user_profile

import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.ConnectionState
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.utils.PlatformImage
import org.junit.Test
import kotlin.test.assertEquals

/**
 * Covers the NUM_USER_PROFILES subscription collector of [ClientUserProfileServiceFacade]: a
 * valid event updates [ClientUserProfileServiceFacade.numUserProfiles].
 *
 * Separate from [ClientUserProfileServiceFacadeTest] because that class owns the Robolectric +
 * `TestApplication` exception for real bitmap decoding, while this one drives `serviceScope`
 * through the Koin integration leaf (the two must never mix in one class — see testing catalog).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ClientUserProfileServiceFacadeNumProfilesTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: UserProfileApiGateway = mockk(relaxed = true)
    private val clientCatHashService: ClientCatHashService<PlatformImage> = mockk(relaxed = true)
    private val webSocketClientService: WebSocketClientService = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var facade: ClientUserProfileServiceFacade

    override fun onSetup() {
        // Never Connected: activate()'s profile-refresh branch stays out of this test's way.
        every { webSocketClientService.connectionState } returns MutableStateFlow(ConnectionState.Connecting)
        facade = ClientUserProfileServiceFacade(apiGateway, clientCatHashService, json, webSocketClientService)
    }

    @Test
    fun `num user profiles websocket event updates numUserProfiles`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeNumUserProfiles() } returns observer

            facade.activate()
            advanceUntilIdle()

            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.NUM_USER_PROFILES,
                    subscriberId = "num-profiles-test",
                    deferredPayload = "7",
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()

            assertEquals(7, facade.numUserProfiles.value)
        }
}
