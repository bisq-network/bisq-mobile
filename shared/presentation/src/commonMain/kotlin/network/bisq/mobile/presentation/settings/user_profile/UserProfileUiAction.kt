package network.bisq.mobile.presentation.settings.user_profile

import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO

sealed interface UserProfileUiAction {
    data class OnStatementChange(
        val value: String,
    ) : UserProfileUiAction

    data class OnTermsChange(
        val value: String,
    ) : UserProfileUiAction

    object OnSavePress : UserProfileUiAction

    object OnCreateProfilePress : UserProfileUiAction

    object OnDeletePress : UserProfileUiAction

    object OnDeleteConfirm : UserProfileUiAction

    object OnDeleteConfirmationDismiss : UserProfileUiAction

    object OnDeleteError : UserProfileUiAction

    object OnDeleteErrorDialogDismiss : UserProfileUiAction

    data class OnUserProfileSelect(
        val profile: UserProfileVO,
    ) : UserProfileUiAction
}
