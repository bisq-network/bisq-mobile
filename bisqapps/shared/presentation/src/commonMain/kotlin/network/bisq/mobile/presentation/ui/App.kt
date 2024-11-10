package network.bisq.mobile.presentation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.navigation.compose.rememberNavController
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import org.jetbrains.compose.ui.tooling.preview.Preview

import kotlinx.coroutines.flow.StateFlow
import org.koin.compose.koinInject
import network.bisq.mobile.presentation.ui.screens.OnBoardingScreen
import network.bisq.mobile.presentation.ui.screens.SplashScreen
import network.bisq.mobile.presentation.i18n.Locales
import network.bisq.mobile.presentation.ui.navigation.Routes

import network.bisq.mobile.presentation.ui.navigation.graph.RootNavGraph
import network.bisq.mobile.presentation.ui.theme.BisqTheme

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
    val lyricist = rememberStrings()
    lyricist.languageTag = Locales.FR

    BisqTheme(darkTheme = true) {
        ProvideStrings(lyricist) {
            RootNavGraph(
                rootNavController = navController,
                innerPadding = PaddingValues(),
                startDestination = Routes.Splash.name
            )
        }
    }

}