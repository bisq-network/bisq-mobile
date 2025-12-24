package network.bisq.mobile.node.main

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import network.bisq.mobile.node.common.presentation.NodeNavGraph
import network.bisq.mobile.presentation.main.App

@Composable
fun NodeApp() {
    val rootNavController = rememberNavController()
    App(
        rootNavController = rootNavController,
        navGraphContent = {
            NodeNavGraph(rootNavController)
        },
    )
}
