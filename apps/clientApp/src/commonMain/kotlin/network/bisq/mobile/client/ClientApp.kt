package network.bisq.mobile.client

import androidx.compose.runtime.Composable
import androidx.navigation.compose.rememberNavController
import network.bisq.mobile.client.common.presentation.navigation.ClientRootNavGraph
import network.bisq.mobile.presentation.ui.App

@Composable
fun ClientApp() {
    val rootNavController = rememberNavController()
    App(
        rootNavController = rootNavController,
        navGraphContent = {
            ClientRootNavGraph(rootNavController)
        }
    )
}