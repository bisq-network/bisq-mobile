package network.bisq.mobile.client.network.presentation.network

sealed interface ClientNetworkOverviewUiAction {
    data object OnConnectionsClick : ClientNetworkOverviewUiAction

    data object OnMyConnectionClick : ClientNetworkOverviewUiAction

    data object OnCheckConnectionSettings : ClientNetworkOverviewUiAction
}
