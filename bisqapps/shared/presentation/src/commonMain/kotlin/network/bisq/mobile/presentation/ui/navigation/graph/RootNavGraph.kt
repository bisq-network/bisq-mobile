package network.bisq.mobile.presentation.ui.navigation.graph

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import network.bisq.mobile.presentation.ui.navigation.*
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.uicases.*
import network.bisq.mobile.presentation.ui.uicases.startup.CreateProfileScreen
import network.bisq.mobile.presentation.ui.uicases.startup.OnBoardingScreen
import network.bisq.mobile.presentation.ui.uicases.startup.SplashScreen
import network.bisq.mobile.presentation.ui.uicases.startup.TrustedNodeSetupScreen
import org.koin.compose.koinInject
import org.koin.core.qualifier.named

@Composable
fun RootNavGraph(
    startDestination: String
) {
    val navControllerDIName = if (startDestination == Routes.Splash.name) "RootNavController" else "TabNavController"
    val navController: NavHostController = koinInject(named(navControllerDIName))

    //TODO: [Need refactor] This is confusing / not proper. But for now, it works!
    //To have both primary screens and Tab bar screens inside a single NavHost.
    //At runtime, 2 actually instances are created.
    //If the code also reflects the same, it will be even easy to understand.

    NavHost(
        modifier = Modifier.background(color = BisqTheme.colors.backgroundColor),
        navController = navController,
        startDestination = startDestination,
    ) {
        composable(route = Routes.Splash.name) {
            SplashScreen()
        }
        composable(route = Routes.Onboarding.name, enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        }) {
            OnBoardingScreen()
        }
        composable(route = Routes.CreateProfile.name, enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        }) {
            CreateProfileScreen()
        }
        composable(route = Routes.TrustedNodeSetup.name, enterTransition = {
            slideIntoContainer(
                AnimatedContentTransitionScope.SlideDirection.Left,
                animationSpec = tween(300)
            )
        }) {
            TrustedNodeSetupScreen()
        }
        composable(route = Routes.TabContainer.name) {
            TabContainerScreen()
        }
        TabNavGraph()
    }
}