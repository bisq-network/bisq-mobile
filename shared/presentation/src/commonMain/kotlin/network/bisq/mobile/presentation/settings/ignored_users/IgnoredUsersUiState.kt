package network.bisq.mobile.presentation.settings.ignored_users

import network.bisq.mobile.data.replicated.user.profile.UserProfileVO

data class IgnoredUsersUiState(
    val ignoredUsers: List<UserProfileVO> = emptyList(),
    val isLoading: Boolean = true,
    /**
     * The lookup failed, on either flavour. Kept apart from an empty result so a failure is never
     * shown as "you ignore nobody": the user gets the failure plus a retry instead.
     */
    val isLoadFailed: Boolean = false,
    /** Peer the undo-ignore confirmation is open for; null while no dialog is shown. */
    val unblockUserId: String? = null,
    /** False while an undo-ignore is in flight, so the dialog shows progress and ignores re-taps. */
    val isUnblockConfirmEnabled: Boolean = true,
)
