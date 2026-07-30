package network.bisq.mobile.client.common.domain.service.user_profile

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import network.bisq.mobile.client.common.domain.websocket.WebSocketClientService
import network.bisq.mobile.client.common.test_utils.TestApplication
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import network.bisq.mobile.data.utils.createEmptyImage
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull

/**
 * Covers [ClientUserProfileServiceFacade.getUserProfileIcon]: it composes the avatar on the IO
 * dispatcher and returns a fallback image (never throwing) when the cat-hash service fails to
 * produce one.
 *
 * Runs under Robolectric because both the composed image and the fallback go through real Android
 * bitmap decoding (`createEmptyImage` / `PlatformImage.deserialize`). It builds the facade directly
 * with mocks rather than extending the Koin integration base: `getUserProfileIcon` never touches
 * `serviceScope`, and [TestApplication] already starts Koin, so a second startKoin would collide.
 */
@RunWith(RobolectricTestRunner::class)
@Config(application = TestApplication::class, sdk = [28])
class ClientUserProfileServiceFacadeTest {
    private val apiGateway: UserProfileApiGateway = mockk(relaxed = true)
    private val clientCatHashService: ClientCatHashService<PlatformImage> = mockk(relaxed = true)
    private val webSocketClientService: WebSocketClientService = mockk(relaxed = true)
    private val json = Json { ignoreUnknownKeys = true }

    private fun facade() =
        ClientUserProfileServiceFacade(
            apiGateway,
            clientCatHashService,
            json,
            webSocketClientService,
        )

    @Test
    fun `getUserProfileIcon returns the composed image on success`() =
        runTest {
            val composed = createEmptyImage()
            every { clientCatHashService.getImage(any(), any()) } returns composed

            val result = facade().getUserProfileIcon(createMockUserProfile("Alice"))

            assertEquals(composed, result)
        }

    @Test
    fun `getUserProfileIcon returns a fallback image when the cat-hash service throws`() =
        runTest {
            every { clientCatHashService.getImage(any(), any()) } throws RuntimeException("compose failed")

            val result = facade().getUserProfileIcon(createMockUserProfile("Bob"))

            // Must degrade to a non-null fallback rather than propagating the failure.
            assertNotNull(result)
        }

    @Test
    fun `getUserProfileIcon rethrows cancellation from the compose call instead of falling back`() =
        runTest {
            // getImage is non-suspend, so a cancel-while-suspended can't unblock its IO thread
            // deterministically; raising the cancellation from the compose call exercises the same
            // guard that decides propagate-vs-fallback. A genuine cancellation must NOT be masked by
            // the fallback image.
            every { clientCatHashService.getImage(any(), any()) } throws CancellationException("cancelled")

            assertFailsWith<CancellationException> {
                facade().getUserProfileIcon(createMockUserProfile("Cara"))
            }
        }
}
