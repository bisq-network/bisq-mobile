package network.bisq.mobile.client.main

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import network.bisq.mobile.client.common.presentation.navigation.ClientRootNavGraph
import network.bisq.mobile.presentation.main.App

@Composable
fun ClientApp() {
    val rootNavController = rememberNavController()
    App(
        rootNavController = rootNavController,
        navGraphContent = {
            ClientRootNavGraph(rootNavController)
        },
    )
}
