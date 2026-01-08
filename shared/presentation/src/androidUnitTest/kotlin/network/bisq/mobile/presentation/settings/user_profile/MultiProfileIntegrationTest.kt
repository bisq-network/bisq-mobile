package network.bisq.mobile.presentation.settings.user_profile

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.createEmptyImage
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Integration tests for multi-profile feature that simulate real user workflows.
 * These tests use a more realistic fake implementation of UserProfileServiceFacade
 * to test the complete flow from UI action to state update.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MultiProfileIntegrationTest {
    private val testDispatcher = StandardTestDispatcher()

    private lateinit var userProfileService: FakeMultiProfileService
    private lateinit var presenter: UserProfilePresenter

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        userProfileService = FakeMultiProfileService()
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ========== Complete User Workflows ==========

    @Test
    fun `complete workflow - create, switch, and delete profiles`() =
        runTest(testDispatcher) {
            // Given - Start with one profile
            val alice = createMockUserProfile("Alice")
            userProfileService.addProfile(alice)
            userProfileService.selectProfile(alice.networkId.pubKey.id)

            // When - Create a second profile
            val bob = createMockUserProfile("Bob")
            userProfileService.addProfile(bob)
            advanceUntilIdle()

            // Then - Should have 2 profiles
            assertEquals(2, userProfileService.userProfiles.value.size)
            assertTrue(userProfileService.userProfiles.value.contains(alice))
            assertTrue(userProfileService.userProfiles.value.contains(bob))

            // When - Switch to Bob
            val selectResult = userProfileService.selectUserProfile(bob.networkId.pubKey.id)
            advanceUntilIdle()

            // Then - Bob should be selected
            assertTrue(selectResult.isSuccess)
            assertEquals(bob, userProfileService.selectedUserProfile.value)

            // When - Delete Alice
            val deleteResult = userProfileService.deleteUserProfile(alice.networkId.pubKey.id)
            advanceUntilIdle()

            // Then - Should have 1 profile (Bob), and Bob should still be selected
            assertTrue(deleteResult.isSuccess)
            assertEquals(1, userProfileService.userProfiles.value.size)
            assertEquals(bob, userProfileService.selectedUserProfile.value)
            assertFalse(userProfileService.userProfiles.value.contains(alice))
        }

    @Test
    fun `deleting selected profile auto-selects another profile`() =
        runTest(testDispatcher) {
            // Given - 3 profiles, Alice is selected
            val alice = createMockUserProfile("Alice")
            val bob = createMockUserProfile("Bob")
            val charlie = createMockUserProfile("Charlie")

            userProfileService.addProfile(alice)
            userProfileService.addProfile(bob)
            userProfileService.addProfile(charlie)
            userProfileService.selectProfile(alice.networkId.pubKey.id)
            advanceUntilIdle()

            assertEquals(alice, userProfileService.selectedUserProfile.value)

            // When - Delete Alice (the selected profile)
            val result = userProfileService.deleteUserProfile(alice.networkId.pubKey.id)
            advanceUntilIdle()

            // Then - Should auto-select Bob (first remaining profile)
            assertTrue(result.isSuccess)
            assertEquals(2, userProfileService.userProfiles.value.size)
            assertNotNull(userProfileService.selectedUserProfile.value)
            assertEquals(bob, userProfileService.selectedUserProfile.value)
        }

    @Test
    fun `cannot delete last profile`() =
        runTest(testDispatcher) {
            // Given - Only one profile
            val alice = createMockUserProfile("Alice")
            userProfileService.addProfile(alice)
            userProfileService.selectProfile(alice.networkId.pubKey.id)
            advanceUntilIdle()

            // When - Try to delete the last profile
            val result = userProfileService.deleteUserProfile(alice.networkId.pubKey.id)
            advanceUntilIdle()

            // Then - Should fail
            assertTrue(result.isFailure)
            assertEquals(1, userProfileService.userProfiles.value.size)
            assertEquals(alice, userProfileService.selectedUserProfile.value)
        }

    @Test
    fun `updating profile preserves other profiles`() =
        runTest(testDispatcher) {
            // Given - 2 profiles
            val alice = createMockUserProfile("Alice")
            val bob = createMockUserProfile("Bob")
            userProfileService.addProfile(alice)
            userProfileService.addProfile(bob)
            userProfileService.selectProfile(alice.networkId.pubKey.id)
            advanceUntilIdle()

            // When - Update Alice's profile
            val result =
                userProfileService.updateAndPublishUserProfile(
                    alice.networkId.pubKey.id,
                    "Alice's new statement",
                    "Alice's new terms",
                )
            advanceUntilIdle()

            // Then - Alice should be updated, Bob unchanged
            assertTrue(result.isSuccess)
            val updatedAlice = result.getOrNull()
            assertNotNull(updatedAlice)
            assertEquals("Alice's new statement", updatedAlice.statement)
            assertEquals("Alice's new terms", updatedAlice.terms)

            // Bob should still exist unchanged
            val bobInList = userProfileService.userProfiles.value.find { it.networkId.pubKey.id == bob.networkId.pubKey.id }
            assertNotNull(bobInList)
            assertEquals(bob.statement, bobInList.statement)
        }

    // ========== Fake Service Implementation ==========

    /**
     * Realistic fake implementation that simulates multi-profile behavior
     */
    private class FakeMultiProfileService : UserProfileServiceFacade {
        private val _userProfiles = MutableStateFlow<List<UserProfileVO>>(emptyList())
        override val userProfiles: StateFlow<List<UserProfileVO>> = _userProfiles

        private val _selectedUserProfile = MutableStateFlow<UserProfileVO?>(null)
        override val selectedUserProfile: StateFlow<UserProfileVO?> = _selectedUserProfile

        override val ignoredProfileIds: StateFlow<Set<String>> = MutableStateFlow(emptySet())
        override val numUserProfiles: StateFlow<Int> = MutableStateFlow(0)

        fun addProfile(profile: UserProfileVO) {
            _userProfiles.update { it + profile }
        }

        fun selectProfile(id: String) {
            _selectedUserProfile.value = _userProfiles.value.find { it.networkId.pubKey.id == id }
        }

        override suspend fun getOwnedUserProfiles(): Result<List<UserProfileVO>> = Result.success(_userProfiles.value)

        override suspend fun selectUserProfile(id: String): Result<UserProfileVO> {
            val profile = _userProfiles.value.find { it.networkId.pubKey.id == id }
            return if (profile != null) {
                _selectedUserProfile.value = profile
                Result.success(profile)
            } else {
                Result.failure(Exception("Profile not found: $id"))
            }
        }

        override suspend fun deleteUserProfile(id: String): Result<UserProfileVO> {
            if (_userProfiles.value.size <= 1) {
                return Result.failure(Exception("Cannot delete last profile"))
            }

            val profileToDelete =
                _userProfiles.value.find { it.networkId.pubKey.id == id }
                    ?: return Result.failure(Exception("Profile not found: $id"))

            _userProfiles.update { it.filterNot { profile -> profile.networkId.pubKey.id == id } }

            // If deleted profile was selected, auto-select first remaining profile
            val newSelected =
                if (_selectedUserProfile.value
                        ?.networkId
                        ?.pubKey
                        ?.id == id
                ) {
                    _userProfiles.value.firstOrNull()
                } else {
                    _selectedUserProfile.value
                } ?: _userProfiles.value.first()

            _selectedUserProfile.value = newSelected
            return Result.success(newSelected)
        }

        override suspend fun updateAndPublishUserProfile(
            profileId: String,
            statement: String?,
            terms: String?,
        ): Result<UserProfileVO> {
            val profile =
                _userProfiles.value.find { it.networkId.pubKey.id == profileId }
                    ?: return Result.failure(Exception("Profile not found: $profileId"))

            val updated = profile.copy(statement = statement ?: "", terms = terms ?: "")
            _userProfiles.update { list ->
                list.map { if (it.networkId.pubKey.id == profileId) updated else it }
            }

            if (_selectedUserProfile.value
                    ?.networkId
                    ?.pubKey
                    ?.id == profileId
            ) {
                _selectedUserProfile.value = updated
            }

            return Result.success(updated)
        }

        // Minimal implementations for other required methods
        override suspend fun hasUserProfile(): Boolean = _userProfiles.value.isNotEmpty()

        override suspend fun generateKeyPair(
            imageSize: Int,
            result: (String, String, PlatformImage?) -> Unit,
        ) {
        }

        override suspend fun createAndPublishNewUserProfile(nickName: String) {}

        override suspend fun getUserIdentityIds(): List<String> = _userProfiles.value.map { it.networkId.pubKey.id }

        override suspend fun findUserProfile(profileId: String): UserProfileVO? = _userProfiles.value.find { it.networkId.pubKey.id == profileId }

        override suspend fun findUserProfiles(ids: List<String>): List<UserProfileVO> = _userProfiles.value.filter { it.networkId.pubKey.id in ids }

        override suspend fun getUserProfileIcon(
            userProfile: UserProfileVO,
            size: Number,
        ): PlatformImage = createEmptyImage()

        override suspend fun getUserProfileIcon(userProfile: UserProfileVO): PlatformImage = createEmptyImage()

        override suspend fun getUserPublishDate(): Long = 0L

        override suspend fun userActivityDetected() {}

        override suspend fun ignoreUserProfile(profileId: String) {}

        override suspend fun undoIgnoreUserProfile(profileId: String) {}

        override suspend fun isUserIgnored(profileId: String): Boolean = false

        override suspend fun getIgnoredUserProfileIds(): Set<String> = emptySet()

        override suspend fun reportUserProfile(
            accusedUserProfile: UserProfileVO,
            message: String,
        ): Result<Unit> = Result.success(Unit)

        override suspend fun activate() {}

        override suspend fun deactivate() {}
    }
}
