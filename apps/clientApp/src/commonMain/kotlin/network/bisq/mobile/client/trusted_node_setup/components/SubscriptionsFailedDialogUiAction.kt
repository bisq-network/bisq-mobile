package network.bisq.mobile.client.trusted_node_setup.components

sealed interface SubscriptionsFailedDialogUiAction {
    data object OnRetryPress : SubscriptionsFailedDialogUiAction

    data object OnContinuePress : SubscriptionsFailedDialogUiAction
}
