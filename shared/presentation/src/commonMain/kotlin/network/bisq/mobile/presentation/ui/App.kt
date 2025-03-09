package network.bisq.mobile.presentation.ui

import ErrorOverlay
import androidx.compose.runtime.*
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import org.jetbrains.compose.ui.tooling.preview.Preview

import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.SwipeBackIOSNavigationHandler
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import org.koin.compose.koinInject

import network.bisq.mobile.presentation.ui.navigation.graph.RootNavGraph
import network.bisq.mobile.presentation.ui.theme.BisqTheme

interface AppPresenter : ViewPresenter {
    var navController: NavHostController

    var tabNavController: NavHostController

    // Observables for state
    val isContentVisible: StateFlow<Boolean>

    val languageCode: StateFlow<String>

    val isSmallScreen: StateFlow<Boolean>

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
    val errorMessage = MainPresenter._genericErrorMessage.collectAsState().value
    val systemCrashed = MainPresenter._systemCrashed.collectAsState().value


    RememberPresenterLifecycle(presenter, {
        presenter.navController = rootNavController
        presenter.tabNavController = tabNavController
        isNavControllerSet = true
    })

    val languageCode = presenter.languageCode.collectAsState().value
    I18nSupport.initialize(languageCode)

    BisqTheme(darkTheme = true) {
        if (isNavControllerSet) {
            SwipeBackIOSNavigationHandler(rootNavController) {
                RootNavGraph(rootNavController)
            }
        }
        ErrorOverlay(
            errorMessage = errorMessage,
            systemCrashed = systemCrashed,
            onClose = {
                presenter.onCloseGenericErrorPanel()
            });
    }

}