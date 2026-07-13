package network.bisq.mobile.node.network.presentation.network

sealed interface NetworkUiAction {
    data object OnConnectionsClick : NetworkUiAction

    data object OnMyNodeClick : NetworkUiAction
}
