package network.bisq.mobile.presentation.ui

import ErrorOverlay
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.SwipeBackIOSNavigationHandler
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.navigation.graph.RootNavGraph
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

interface AppPresenter : ViewPresenter {
    var navController: NavHostController

    var tabNavController: NavHostController

    // Observables for state
    val isContentVisible: StateFlow<Boolean>

    val languageCode: StateFlow<String>

    val isSmallScreen: StateFlow<Boolean>

    val tradesWithUnreadMessages: StateFlow<Map<String, Int>>
    val readMessageCountsByTrade: StateFlow<Map<String, Int>>

    // Actions
    fun toggleContentVisibility()

    fun navigateToTrustedNode()
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
    val languageCode by presenter.languageCode.collectAsState()

    I18nSupport.initialize(languageCode)

    RememberPresenterLifecycle(presenter, {
        presenter.navController = rootNavController
        presenter.tabNavController = tabNavController
        isNavControllerSet = true
    })

    Surface(
        modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars).windowInsetsPadding(WindowInsets.navigationBars).windowInsetsPadding(WindowInsets.displayCutout)
    ) {
        BisqTheme(darkTheme = true) {
            if (isNavControllerSet) {
                SwipeBackIOSNavigationHandler(rootNavController) {
                    RootNavGraph(rootNavController)
                }
            }
            ErrorOverlay()
        }
    }
}