package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import network.bisq.mobile.presentation.ui.navigation.OnBoardingRouteScreen
import network.bisq.mobile.presentation.ui.navigation.SplashRouteScreen
import network.bisq.mobile.presentation.ui.screens.OnBoardingScreen
import network.bisq.mobile.presentation.ui.screens.SplashScreen
import network.bisq.mobile.presentation.ui.theme.backgroundColor

@Composable
fun RootNavGraph(
    rootNavController: NavHostController,
    innerPadding: PaddingValues,
    startDestination: String
) {
    NavHost(
        modifier = Modifier.background(color = backgroundColor),
        navController = rootNavController,
        startDestination = startDestination,
    ) {
        composable(route = SplashRouteScreen.Splash.route) {
            SplashScreen(rootNavController = rootNavController, innerPadding = innerPadding)
        }
        composable(route = OnBoardingRouteScreen.OnBoard.route,enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        }) {
            OnBoardingScreen(rootNavController = rootNavController)
        }
    }
}