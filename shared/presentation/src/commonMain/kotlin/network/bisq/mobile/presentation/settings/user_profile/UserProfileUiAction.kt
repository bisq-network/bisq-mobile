package network.bisq.mobile.presentation.settings.user_profile

import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

sealed interface UserProfileUiAction {
    data class OnStatementChanged(
        val value: String,
    ) : UserProfileUiAction

    data class OnTermsChanged(
        val value: String,
    ) : UserProfileUiAction

    data class OnSavePressed(
        val profileId: String,
        val uiState: UserProfileUiState,
    ) : UserProfileUiAction

    object OnCreateProfilePressed : UserProfileUiAction

    data class OnDeletePressed(
        val profile: UserProfileVO,
    ) : UserProfileUiAction

    data class OnDeleteConfirmed(
        val profile: UserProfileVO,
    ) : UserProfileUiAction

    object OnDeleteConfirmationDismissed : UserProfileUiAction

    object OnDeleteError : UserProfileUiAction

    object OnDeleteErrorDialogDismissed : UserProfileUiAction

    data class OnUserProfileSelected(
        val profile: UserProfileVO,
    ) : UserProfileUiAction
}
