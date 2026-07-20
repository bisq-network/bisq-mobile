package network.bisq.mobile.node.network.presentation.network

sealed interface NodeNetworkOverviewUiAction {
    data object OnConnectionsClick : NodeNetworkOverviewUiAction

    data object OnMyNodeClick : NodeNetworkOverviewUiAction
}
