package network.bisq.mobile.presentation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview

import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject
import network.bisq.mobile.presentation.ui.screens.OnBoardingScreen
import network.bisq.mobile.presentation.ui.screens.SplashScreen
import network.bisq.mobile.presentation.ui.navigation.Routes

import network.bisq.mobile.presentation.ui.navigation.graph.RootNavGraph

interface AppPresenter {
    // Observables for state
    val isContentVisible: StateFlow<Boolean>
    val greetingText: StateFlow<String>

    // Actions
    fun toggleContentVisibility()
}

/**
 * Main composable view of the application that platforms use to draw.
 */
@Composable
@Preview
fun App() {
    val presenter: AppPresenter = koinInject()

    val navController = rememberNavController()
    RootNavGraph(
        rootNavController = navController,
        innerPadding = PaddingValues(),
        startDestination = Routes.Splash.name
    )

    //MaterialTheme {
        //SplashScreen()
        //OnBoardingScreen()
    //}

// Collecting state from presenter
//        val showContent by presenter.isContentVisible.collectAsState()
//        val greeting by presenter.greetingText.collectAsState()
//        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//            Button(onClick = { presenter.toggleContentVisibility() }) {
//                Text("Click me!")
//            }
//            AnimatedVisibility(showContent) {
//                Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
//                    Image(painterResource(Res.drawable.compose_multiplatform), null)
//                    Text("Compose: $greeting")
//                }
//            }
//        }


}