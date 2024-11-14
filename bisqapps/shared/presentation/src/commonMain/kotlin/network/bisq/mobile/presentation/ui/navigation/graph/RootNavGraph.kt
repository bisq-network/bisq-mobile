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
import network.bisq.mobile.presentation.ui.navigation.*
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.*
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfileScreen
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingScreen
import network.bisq.mobile.presentation.ui.uicases.startup.SplashScreen
import network.bisq.mobile.presentation.ui.uicases.startup.URLScreen

@Composable
fun RootNavGraph(
    rootNavController: NavHostController,
    innerPadding: PaddingValues,
    startDestination: String
) {
    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = rootNavController,
        startDestination = startDestination,
    ) {
        composable(route = Routes.Splash.name) {
            SplashScreen(rootNavController = rootNavController, innerPadding = innerPadding)
        }
        composable(route = Routes.Onboarding.name, enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        }) {
            OnBoardingScreen(rootNavController = rootNavController)
        }
        composable(route = Routes.CreateProfile.name, enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        }) {
            CreateProfileScreen(rootNavController = rootNavController)
        }
        composable(route = Routes.BisqUrl.name, enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        }) {
            URLScreen(rootNavController = rootNavController)
        }
        composable(route = Routes.TabContainer.name) {
            TabContainerScreen(rootNavController = rootNavController)
        }
        TabNavGraph(rootNavController, innerPadding)
    }
}