package network.bisq.mobile.presentation.settings.ignored_users

sealed interface IgnoredUsersUiAction {
    data object OnRetryLoadClick : IgnoredUsersUiAction

    data class OnUnblockClick(
        val userId: String,
    ) : IgnoredUsersUiAction

    data object OnConfirmUnblock : IgnoredUsersUiAction

    data object OnDismissUnblockDialog : IgnoredUsersUiAction

    data class OnPeerProfileClick(
        val userId: String,
    ) : IgnoredUsersUiAction
}
