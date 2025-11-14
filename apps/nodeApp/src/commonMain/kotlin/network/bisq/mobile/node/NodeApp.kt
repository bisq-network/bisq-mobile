package network.bisq.mobile.node

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import network.bisq.mobile.node.common.presentation.NodeNavGraph
import network.bisq.mobile.presentation.ui.App

@Composable
fun NodeApp() {
    val rootNavController = rememberNavController()
    App(
        rootNavController = rootNavController,
        navGraphContent = {
            NodeNavGraph(rootNavController)
        }
    )
}