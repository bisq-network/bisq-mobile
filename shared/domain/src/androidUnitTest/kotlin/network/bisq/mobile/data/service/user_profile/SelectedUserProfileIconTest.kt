package network.bisq.mobile.data.service.user_profile

import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.data.utils.PlatformImage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * The avatar of the selected profile is held by the user profile facade, so every top bar shows it
 * at once. These tests cover that it never holds the image of a profile which is not selected.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SelectedUserProfileIconTest {
    private val alice = createMockUserProfile("alice")
    private val bob = createMockUserProfile("bob")
    private val aliceImage: PlatformImage = mockk()
    private val bobImage: PlatformImage = mockk()

    private val selectedUserProfile = MutableStateFlow<UserProfileVO?>(null)
    private val icon = MutableStateFlow<PlatformImage?>(null)
    private val pendingLoads = mutableMapOf<String, CompletableDeferred<PlatformImage>>()
    private val loadedProfiles = mutableListOf<UserProfileVO>()

    @Test
    fun `icon is empty while no profile is selected`() =
        runIconTest {
            assertNull(icon.value)
            assertEquals(emptyList(), loadedProfiles)
        }

    @Test
    fun `icon is set once the image of the selected profile is loaded`() =
        runIconTest {
            selectedUserProfile.value = alice
            assertNull(icon.value)

            completeLoad(alice, aliceImage)

            assertSame(aliceImage, icon.value)
        }

    @Test
    fun `selecting another profile clears the icon until its own image is loaded`() =
        runIconTest {
            selectedUserProfile.value = alice
            completeLoad(alice, aliceImage)

            selectedUserProfile.value = bob

            assertNull(icon.value)
            completeLoad(bob, bobImage)
            assertSame(bobImage, icon.value)
        }

    @Test
    fun `slow load of the previous profile does not replace the image of the new one`() =
        runIconTest {
            selectedUserProfile.value = alice
            val aliceLoad = pendingLoads.getValue(alice.id)

            selectedUserProfile.value = bob
            completeLoad(bob, bobImage)
            aliceLoad.complete(aliceImage)

            assertSame(bobImage, icon.value)
        }

    @Test
    fun `switching back and forth ends with the image of the last selected profile`() =
        runIconTest {
            selectedUserProfile.value = alice
            completeLoad(alice, aliceImage)
            selectedUserProfile.value = bob
            selectedUserProfile.value = alice
            assertNull(icon.value)

            completeLoad(alice, aliceImage)

            assertSame(aliceImage, icon.value)
        }

    @Test
    fun `update of the selected profile keeps its image while it is loaded again`() =
        runIconTest {
            selectedUserProfile.value = alice
            completeLoad(alice, aliceImage)
            val newAliceImage: PlatformImage = mockk()

            val updatedAlice = alice.copy(statement = "updated statement")
            selectedUserProfile.value = updatedAlice

            assertSame(aliceImage, icon.value)
            completeLoad(updatedAlice, newAliceImage)
            assertSame(newAliceImage, icon.value)
        }

    @Test
    fun `icon is cleared when the selected profile is removed`() =
        runIconTest {
            selectedUserProfile.value = alice
            completeLoad(alice, aliceImage)

            selectedUserProfile.value = null

            assertNull(icon.value)
        }

    @Test
    fun `failed load leaves the icon empty and a later selection still loads`() =
        runIconTest {
            selectedUserProfile.value = alice
            pendingLoads.getValue(alice.id).completeExceptionally(IllegalStateException("no image"))
            assertNull(icon.value)

            selectedUserProfile.value = bob
            completeLoad(bob, bobImage)

            assertSame(bobImage, icon.value)
        }

    private fun completeLoad(
        userProfile: UserProfileVO,
        image: PlatformImage,
    ) {
        pendingLoads.getValue(userProfile.id).complete(image)
    }

    private fun runIconTest(testBody: suspend TestScope.() -> Unit) =
        runTest(UnconfinedTestDispatcher()) {
            val job =
                launch {
                    keepSelectedUserProfileIconUpdated(selectedUserProfile, icon) { userProfile ->
                        loadedProfiles += userProfile
                        CompletableDeferred<PlatformImage>().also { pendingLoads[userProfile.id] = it }.await()
                    }
                }
            testBody()
            job.cancel()
        }
}
