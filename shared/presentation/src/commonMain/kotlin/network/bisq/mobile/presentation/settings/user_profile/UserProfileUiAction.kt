package network.bisq.mobile.presentation.settings.user_profile

import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

sealed interface UserProfileUiAction {
    data class OnStatementChange(
        val value: String,
    ) : UserProfileUiAction

    data class OnTermsChange(
        val value: String,
    ) : UserProfileUiAction

    data class OnSavePress(
        val profileId: String,
        val uiState: UserProfileUiState,
    ) : UserProfileUiAction

    object OnCreateProfilePress : UserProfileUiAction

    data class OnDeletePress(
        val profile: UserProfileVO,
    ) : UserProfileUiAction

    data class OnDeleteConfirm(
        val profile: UserProfileVO,
    ) : UserProfileUiAction

    object OnDeleteConfirmationDismiss : UserProfileUiAction

    object OnDeleteError : UserProfileUiAction

    object OnDeleteErrorDialogDismiss : UserProfileUiAction

    data class OnUserProfileSelect(
        val profile: UserProfileVO,
    ) : UserProfileUiAction
}
