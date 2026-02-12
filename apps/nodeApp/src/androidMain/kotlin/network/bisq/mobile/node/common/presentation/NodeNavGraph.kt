package network.bisq.mobile.node.common.presentation

import androidx.compose.foundation.background
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import network.bisq.mobile.node.settings.backup.presentation.BackupScreen
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.graph.addCommonAppRoutes
import network.bisq.mobile.presentation.common.ui.navigation.graph.addScreen
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.koin.compose.koinInject

@Composable
fun NodeNavGraph(
    rootNavController: NavHostController,
    modifier: Modifier = Modifier,
) {
    val navigationManager: NavigationManager = koinInject()

    NavHost(
        modifier = modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = rootNavController,
        startDestination = NavRoute.Splash,
    ) {
        addCommonAppRoutes()
        addNodeAppRoutes()
    }

    DisposableEffect(rootNavController) {
        navigationManager.setRootNavController(rootNavController)
        onDispose {
            navigationManager.setRootNavController(null)
        }
    }
}

fun NavGraphBuilder.addNodeAppRoutes() {
    addScreen<NavRoute.BackupAndRestore> { BackupScreen() }
}
