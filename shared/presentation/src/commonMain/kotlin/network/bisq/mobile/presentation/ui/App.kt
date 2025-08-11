package network.bisq.mobile.presentation.ui

import ErrorOverlay
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.SwipeBackIOSNavigationHandler
import network.bisq.mobile.presentation.ui.components.context.LocalAnimationsEnabled
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.navigation.graph.RootNavGraph
import network.bisq.mobile.presentation.ui.navigation.graph.TabNavGraph
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

    val showAnimation: StateFlow<Boolean>

    // Actions
    fun toggleContentVisibility()

    fun navigateToTrustedNode()
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
        modifier = Modifier
            .fillMaxSize()
            .consumeWindowInsets(WindowInsets.systemBars) // Eat insets, so no white stripes
            .background(Color.Black) // Or your desired background color behind system bars
    ) {
        // Inner container adds padding for content
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(
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

    I18nSupport.initialize(languageCode)

    SafeInsetsContainer {
        BisqTheme(darkTheme = true) {
            if (isNavControllerSet) {
                SwipeBackIOSNavigationHandler(rootNavController) {
                    CompositionLocalProvider(LocalAnimationsEnabled provides showAnimation) {
                        RootNavGraph(rootNavController)
                    }
                }
            }
            ErrorOverlay()
        }
    }
}