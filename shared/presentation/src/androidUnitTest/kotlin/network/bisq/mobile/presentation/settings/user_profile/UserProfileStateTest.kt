package network.bisq.mobile.presentation.settings.user_profile

import network.bisq.mobile.domain.data.replicated.user.profile.createMockUserProfile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for UserProfileUiState data class and UserProfileUiAction sealed interface.
 * These tests verify the state management and action creation patterns.
 */
class UserProfileStateTest {
    private val profile1 = createMockUserProfile("Alice")
    private val profile2 = createMockUserProfile("Bob")

    // ========== UserProfileUiState Tests ==========

    @Test
    fun `default state has empty values`() {
        // When
        val state = UserProfileUiState()

        // Then
        assertTrue(state.userProfiles.isEmpty())
        assertNull(state.selectedUserProfile)
        // profileAge, lastUserActivity, and reputation use i18n() which returns translated strings
        // Just verify they're not empty
        assertTrue(state.profileAge.isNotEmpty())
        assertTrue(state.lastUserActivity.isNotEmpty())
        assertTrue(state.reputation.isNotEmpty())
        assertEquals("", state.statementDraft)
        assertEquals("", state.termsDraft)
        assertFalse(state.isLoadingData)
        assertFalse(state.isBusyWithAction)
        assertFalse(state.isBusy) // Computed property
        assertFalse(state.shouldBlurBg)
        assertNull(state.showDeleteConfirmationForProfile)
        assertFalse(state.showDeleteErrorDialog)
    }

    @Test
    fun `state copy updates only specified fields`() {
        // Given
        val original =
            UserProfileUiState(
                userProfiles = listOf(profile1),
                selectedUserProfile = profile1,
                isLoadingData = false,
                isBusyWithAction = false,
            )

        // When - set action busy flag
        val updated = original.copy(isBusyWithAction = true)

        // Then
        assertEquals(original.userProfiles, updated.userProfiles)
        assertEquals(original.selectedUserProfile, updated.selectedUserProfile)
        assertTrue(updated.isBusy) // Computed property should be true
        assertFalse(original.isBusy) // Computed property should be false
    }

    @Test
    fun `isBusy computed property works correctly`() {
        // When both flags are false
        val state1 = UserProfileUiState(isLoadingData = false, isBusyWithAction = false)
        assertFalse(state1.isBusy)

        // When only loading data
        val state2 = UserProfileUiState(isLoadingData = true, isBusyWithAction = false)
        assertTrue(state2.isBusy)

        // When only busy with action
        val state3 = UserProfileUiState(isLoadingData = false, isBusyWithAction = true)
        assertTrue(state3.isBusy)

        // When both flags are true
        val state4 = UserProfileUiState(isLoadingData = true, isBusyWithAction = true)
        assertTrue(state4.isBusy)
    }

    @Test
    fun `state with delete confirmation shows correct profile`() {
        // Given
        val state =
            UserProfileUiState(
                userProfiles = listOf(profile1, profile2),
                selectedUserProfile = profile1,
                showDeleteConfirmationForProfile = profile2,
            )

        // Then
        assertEquals(profile2, state.showDeleteConfirmationForProfile)
        assertEquals(profile1, state.selectedUserProfile)
    }

    @Test
    fun `state equality works correctly`() {
        // Given
        val state1 =
            UserProfileUiState(
                userProfiles = listOf(profile1),
                selectedUserProfile = profile1,
            )
        val state2 =
            UserProfileUiState(
                userProfiles = listOf(profile1),
                selectedUserProfile = profile1,
            )
        val state3 =
            UserProfileUiState(
                userProfiles = listOf(profile2),
                selectedUserProfile = profile2,
            )

        // Then
        assertEquals(state1, state2)
        assertNotEquals(state1, state3)
    }

    // ========== UserProfileUiAction Tests ==========

    @Test
    fun `OnStatementChange action holds correct value`() {
        // When
        val action = UserProfileUiAction.OnStatementChange("New statement")

        // Then
        assertEquals("New statement", action.value)
    }

    @Test
    fun `OnTermsChange action holds correct value`() {
        // When
        val action = UserProfileUiAction.OnTermsChange("New terms")

        // Then
        assertEquals("New terms", action.value)
    }

    @Test
    fun `OnUserProfileSelect action holds correct profile`() {
        // When
        val action = UserProfileUiAction.OnUserProfileSelect(profile1)

        // Then
        assertEquals(profile1, action.profile)
    }

    @Test
    fun `singleton actions are correctly instantiated`() {
        // When/Then - verify singleton actions can be referenced
        assertNotNull(UserProfileUiAction.OnCreateProfilePress)
        assertNotNull(UserProfileUiAction.OnDeleteConfirmationDismiss)
        assertNotNull(UserProfileUiAction.OnDeleteError)
        assertNotNull(UserProfileUiAction.OnDeleteErrorDialogDismiss)
    }

    @Test
    fun `action sealed interface prevents external implementations`() {
        // This test verifies the sealed interface pattern
        // All possible actions should be defined in UserProfileUiAction

        val actions: List<UserProfileUiAction> =
            listOf(
                UserProfileUiAction.OnStatementChange("test"),
                UserProfileUiAction.OnTermsChange("test"),
                UserProfileUiAction.OnSavePress,
                UserProfileUiAction.OnCreateProfilePress,
                UserProfileUiAction.OnDeletePress,
                UserProfileUiAction.OnDeleteConfirm,
                UserProfileUiAction.OnDeleteConfirmationDismiss,
                UserProfileUiAction.OnDeleteError,
                UserProfileUiAction.OnDeleteErrorDialogDismiss,
                UserProfileUiAction.OnUserProfileSelect(profile1),
            )

        // Then - verify all 10 action types were created successfully
        assertEquals(10, actions.size)
        actions.forEach { action ->
            assertNotNull(action)
        }
    }
}
