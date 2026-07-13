package network.bisq.mobile.node.common.presentation.navigation

import kotlinx.serialization.Serializable
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute

interface NodeNavRoute : NavRoute {
    @Serializable
    data object NetworkPeerConnections : NodeNavRoute

    @Serializable
    data object NetworkMyNode : NodeNavRoute
}
