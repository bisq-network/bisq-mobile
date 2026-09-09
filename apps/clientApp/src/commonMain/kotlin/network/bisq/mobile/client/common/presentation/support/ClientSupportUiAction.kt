package network.bisq.mobile.client.common.presentation.support

sealed interface ClientSupportUiAction {
    data object OnRequestDeviceToken : ClientSupportUiAction

    data class OnCopyToken(
        val token: String,
    ) : ClientSupportUiAction
}
