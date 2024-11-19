package network.bisq.mobile.presentation.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.*
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import cafe.adriel.lyricist.ProvideStrings
import cafe.adriel.lyricist.rememberStrings
import org.jetbrains.compose.ui.tooling.preview.Preview

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ViewPresenter
import org.koin.compose.koinInject
import network.bisq.mobile.presentation.ui.navigation.Routes

import network.bisq.mobile.presentation.ui.navigation.graph.RootNavGraph
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.koin.mp.KoinPlatform.getKoin

interface AppPresenter: ViewPresenter {
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

    val rootNavController = rememberNavController()
    val tabNavController = rememberNavController()

    var isNavControllerSet by remember { mutableStateOf(false) }

    val presenter: AppPresenter = koinInject()


    LaunchedEffect(rootNavController) {
        presenter.onViewAttached()
        getKoin().setProperty("RootNavController", rootNavController)
        getKoin().setProperty("TabNavController", tabNavController)
        isNavControllerSet = true
    }

    val lyricist = rememberStrings()
    // lyricist.languageTag = Locales.FR

    BisqTheme(darkTheme = true) {
        ProvideStrings(lyricist) {
            if (isNavControllerSet) {
                RootNavGraph(
                    startDestination = Routes.Splash.name
                )
            }
        }
    }

}