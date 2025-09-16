package network.bisq.mobile.presentation.ui

import ErrorOverlay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.setDefaultLocale
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.SwipeBackIOSNavigationHandler
import network.bisq.mobile.presentation.ui.components.context.LocalAnimationsEnabled
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WarningConfirmationDialog
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

    val showAnimation: StateFlow<Boolean>

    val showAllConnectionsLostDialogue: StateFlow<Boolean>

    // Actions
    fun toggleContentVisibility()

    fun navigateToTrustedNode()

    fun onCloseConnectionLostDialogue()

    fun onRestartApp()

    fun onTerminateApp()
}

@Composable
fun WindowInsets.topPaddingDp(): Dp {
    val density = LocalDensity.current
    val topPx = getTop(density)
    return with(density) { topPx.toDp() }
}

@Composable
fun WindowInsets.bottomPaddingDp(): Dp {
    val density = LocalDensity.current
    val bottomPx = getBottom(density)
    return with(density) { bottomPx.toDp() }
}

@Composable
fun SafeInsetsContainer(
    content: @Composable () -> Unit
) {
    // Outer container consumes insets and paints the background
    Box(
        modifier = Modifier.fillMaxSize()
            .consumeWindowInsets(WindowInsets.systemBars) // Eat insets, so no white stripes
            .background(BisqTheme.colors.backgroundColor)
    ) {
        // Inner container adds padding for content
        Box(
            modifier = Modifier.fillMaxSize().padding(
                top = WindowInsets.statusBars.topPaddingDp(),
                bottom = WindowInsets.navigationBars.bottomPaddingDp()
            )
        ) {
            content()
        }
    }
}

/**
 * Main composable view of the application that platforms use to draw.
 */
@Composable
@Preview
fun App() {
    val presenter: AppPresenter = koinInject()
    val rootNavController = rememberNavController()
    val tabNavController = rememberNavController()
    var isNavControllerSet by remember { mutableStateOf(false) }

    RememberPresenterLifecycle(presenter, {
        presenter.navController = rootNavController
        presenter.tabNavController = tabNavController
        isNavControllerSet = true
    })

    val languageCode by presenter.languageCode.collectAsState()
    val showAnimation by presenter.showAnimation.collectAsState()
    val showAllConnectionsLostDialogue by presenter.showAllConnectionsLostDialogue.collectAsState()

    LaunchedEffect(languageCode) {
        if (languageCode.isNotBlank()) {
            // TODO is that needed? We set the language for i18n in the SettingsServiceFacade
            I18nSupport.setLanguage(languageCode)
            setDefaultLocale(languageCode)
        }
    }

    BisqTheme {
        SafeInsetsContainer {
            if (isNavControllerSet) {
                SwipeBackIOSNavigationHandler(rootNavController) {
                    CompositionLocalProvider(LocalAnimationsEnabled provides showAnimation) {
                        RootNavGraph(rootNavController)
                    }
                }
            }
            ErrorOverlay()

            if (showAllConnectionsLostDialogue) {
                WarningConfirmationDialog(
                    headline = "connectivity.disconnected.title".i18n(),
                    message = "connectivity.disconnected.message".i18n(),
                    confirmButtonText = "connectivity.disconnected.restart".i18n(),
                    onConfirm = { presenter.onRestartApp() },
                    onDismiss = { presenter.onCloseConnectionLostDialogue() }
                )
            }
        }
    }
}