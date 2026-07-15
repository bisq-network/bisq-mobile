package network.bisq.mobile.client.common.domain.service.reputation

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.messages.WebSocketEvent
import network.bisq.mobile.client.common.domain.websocket.subscription.ModificationType
import network.bisq.mobile.client.common.domain.websocket.subscription.Topic
import network.bisq.mobile.client.common.domain.websocket.subscription.WebSocketEventObserver
import network.bisq.mobile.client.common.test_utils.ClientKoinIntegrationTestBase
import network.bisq.mobile.data.replicated.user.reputation.ReputationScoreVO
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ClientReputationServiceFacadeTest : ClientKoinIntegrationTestBase() {
    private val apiGateway: ReputationApiGateway = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }
    private lateinit var facade: ClientReputationServiceFacade

    override fun onSetup() {
        facade = ClientReputationServiceFacade(apiGateway, json, isDebug = false)
    }

    private fun createFacade(isDebug: Boolean) = ClientReputationServiceFacade(apiGateway, json, isDebug = isDebug)

    @Test
    fun `activate subscribes to user reputation`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeUserReputation() } returns observer

            facade.activate()
            advanceUntilIdle()

            coVerify { apiGateway.subscribeUserReputation() }
        }

    @Test
    fun `activate tolerates reputation subscription failure`() =
        runTest {
            coEvery { apiGateway.subscribeUserReputation() } throws RuntimeException("subscribe failed")

            facade.activate()
            advanceUntilIdle()
        }

    @Test
    fun `reputation websocket event updates scoreByUserProfileId`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeUserReputation() } returns observer

            facade.activate()
            advanceUntilIdle()

            observer.setEvent(reputationEvent("""{"user-1":{"totalScore":120,"fiveSystemScore":4.2,"ranking":5}}"""))
            advanceUntilIdle()

            assertEquals(120L, facade.scoreByUserProfileId["user-1"])
        }

    @Test
    fun `reputation websocket event with null deferredPayload is ignored`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeUserReputation() } returns observer

            facade.activate()
            advanceUntilIdle()

            observer.setEvent(
                WebSocketEvent(
                    topic = Topic.REPUTATION,
                    subscriberId = "reputation-test",
                    deferredPayload = null,
                    modificationType = ModificationType.REPLACE,
                    sequenceNumber = 1,
                ),
            )
            advanceUntilIdle()

            assertTrue(facade.scoreByUserProfileId.isEmpty())
        }

    @Test
    fun `reputation websocket event with invalid payload is ignored`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeUserReputation() } returns observer

            facade.activate()
            advanceUntilIdle()

            observer.setEvent(reputationEvent("""not-json"""))
            advanceUntilIdle()

            assertTrue(facade.scoreByUserProfileId.isEmpty())
        }

    @Test
    fun `getReputation returns gateway result when isDebug`() =
        runTest {
            val score = ReputationScoreVO(totalScore = 42, fiveSystemScore = 3.5, ranking = 2)
            coEvery { apiGateway.getReputationScore("user-1") } returns Result.success(score)

            val result = createFacade(isDebug = true).getReputation("user-1")

            assertTrue(result.isSuccess)
            assertEquals(score, result.getOrNull())
            coVerify(exactly = 1) { apiGateway.getReputationScore("user-1") }
        }

    @Test
    fun `getReputation returns gateway failure when isDebug`() =
        runTest {
            coEvery { apiGateway.getReputationScore("user-1") } returns Result.failure(Exception("not found"))

            val result = createFacade(isDebug = true).getReputation("user-1")

            assertTrue(result.isFailure)
            coVerify(exactly = 1) { apiGateway.getReputationScore("user-1") }
        }

    @Test
    fun `getReputation returns cached score when not debug`() =
        runTest {
            val observer = WebSocketEventObserver()
            coEvery { apiGateway.subscribeUserReputation() } returns observer

            facade.activate()
            advanceUntilIdle()

            observer.setEvent(reputationEvent("""{"user-1":{"totalScore":120,"fiveSystemScore":4.2,"ranking":5}}"""))
            advanceUntilIdle()

            val result = facade.getReputation("user-1")

            assertTrue(result.isSuccess)
            assertEquals(
                ReputationScoreVO(totalScore = 120, fiveSystemScore = 4.2, ranking = 5),
                result.getOrNull(),
            )
            coVerify(exactly = 0) { apiGateway.getReputationScore(any()) }
        }

    @Test
    fun `getReputation returns failure when not debug and user missing from cache`() =
        runTest {
            val result = facade.getReputation("missing-user")

            assertTrue(result.isFailure)
            coVerify(exactly = 0) { apiGateway.getReputationScore(any()) }
        }

    @Test
    fun `getProfileAge returns gateway result on success`() =
        runTest {
            coEvery { apiGateway.getProfileAge("user-1") } returns Result.success(1_700_000_000_000L)

            val result = facade.getProfileAge("user-1")

            assertTrue(result.isSuccess)
            assertEquals(1_700_000_000_000L, result.getOrNull())
            coVerify(exactly = 1) { apiGateway.getProfileAge("user-1") }
        }

    @Test
    fun `getProfileAge returns failure when gateway throws`() =
        runTest {
            coEvery { apiGateway.getProfileAge("user-1") } throws RuntimeException("network error")

            val result = facade.getProfileAge("user-1")

            assertTrue(result.isFailure)
        }

    private fun reputationEvent(payload: String) =
        WebSocketEvent(
            topic = Topic.REPUTATION,
            subscriberId = "reputation-test",
            deferredPayload = payload,
            modificationType = ModificationType.REPLACE,
            sequenceNumber = 1,
        )
}
