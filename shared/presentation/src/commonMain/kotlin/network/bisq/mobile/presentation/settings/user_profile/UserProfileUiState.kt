package network.bisq.mobile.presentation.settings.user_profile

import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.i18n.i18n

data class UserProfileUiState(
    val userProfiles: List<UserProfileVO> = emptyList(),
    val selectedUserProfile: UserProfileVO? = null,
    val profileAge: String = "data.na".i18n(),
    val lastUserActivity: String = "data.na".i18n(),
    val reputation: String = "data.na".i18n(),
    val statementDraft: String = "",
    val termsDraft: String = "",
    val isLoadingData: Boolean = false,
    val isBusyWithAction: Boolean = false,
    val shouldBlurBg: Boolean = false,
    val showDeleteConfirmationForProfile: UserProfileVO? = null,
    val showDeleteErrorDialog: Boolean = false,
) {
    /**
     * Computed property: UI is busy if either loading data or performing an action
     */
    val isBusy: Boolean
        get() = isLoadingData || isBusyWithAction
}
