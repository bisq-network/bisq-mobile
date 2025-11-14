package network.bisq.mobile.presentation.ui

import ErrorOverlay
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.setDefaultLocale
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.SwipeBackIOSNavigationHandler
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.context.LocalAnimationsEnabled
import network.bisq.mobile.presentation.ui.components.molecules.dialog.LoadingDialog
import network.bisq.mobile.presentation.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.navigation.ExternalUriHandler
import network.bisq.mobile.presentation.ui.navigation.graph.RootNavGraph
import network.bisq.mobile.presentation.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.banners.NetworkStatusBanner
import org.koin.compose.koinInject

interface AppPresenter : ViewPresenter {

    // Observables for state
    val isMainContentVisible: StateFlow<Boolean>

    val languageCode: StateFlow<String>

    val isSmallScreen: StateFlow<Boolean>

    val tradesWithUnreadMessages: StateFlow<Map<String, Int>>

    val showAnimation: StateFlow<Boolean>

    val showAllConnectionsLostDialogue: StateFlow<Boolean>

    val showReconnectOverlay: StateFlow<Boolean>

    // Actions
    fun setIsMainContentVisible(value: Boolean)

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
fun App() {
    val presenter: AppPresenter = koinInject()
    val navigationManager: NavigationManager = koinInject()
    val globalUiManager: GlobalUiManager = koinInject()
    val rootNavController = rememberNavController()

    DisposableEffect(rootNavController) {
        navigationManager.setRootNavController(rootNavController)
        onDispose {
            navigationManager.setRootNavController(null)
        }
    }
    RememberPresenterLifecycle(presenter)

    val languageCode by presenter.languageCode.collectAsState()
    val showAnimation by presenter.showAnimation.collectAsState()
    val showAllConnectionsLostDialogue by presenter.showAllConnectionsLostDialogue.collectAsState()
    val showReconnectOverlay by presenter.showReconnectOverlay.collectAsState()
    val showLoadingDialog by globalUiManager.showLoadingDialog.collectAsState()

    LaunchedEffect(languageCode) {
        if (languageCode.isNotBlank()) {
            // TODO is that needed? We set the language for i18n in the SettingsServiceFacade
            I18nSupport.setLanguage(languageCode)
            setDefaultLocale(languageCode)
        }
    }

    BisqTheme {
        SafeInsetsContainer {
            SwipeBackIOSNavigationHandler(rootNavController) {
                CompositionLocalProvider(LocalAnimationsEnabled provides showAnimation) {
                    DisposableEffect(Unit) {
                        ExternalUriHandler.listener = { uri ->
                            navigationManager.navigateFromUri(uri)
                        }
                        onDispose {
                            ExternalUriHandler.listener = null
                        }
                    }
                    Column {
                        NetworkStatusBanner()
                        RootNavGraph(rootNavController)
                    }
                }
            }

            ErrorOverlay()

            if (showAllConnectionsLostDialogue) {
                WarningConfirmationDialog(
                    headline = "mobile.connectivity.disconnected.title".i18n(),
                    message = "mobile.connectivity.disconnected.message".i18n(),
                    confirmButtonText = "mobile.connectivity.disconnected.restart".i18n(),
                    onConfirm = { presenter.onRestartApp() },
                    onDismiss = { presenter.onCloseConnectionLostDialogue() }
                )
            } else if (showReconnectOverlay) {
                ReconnectingOverlay(onClick = { presenter.onRestartApp() })
            }

            // Global loading dialog - renders on top of everything (last child = topmost z-order)
            if (showLoadingDialog) {
                LoadingDialog()
            }
        }
    }
}

@Composable
fun ReconnectingOverlay(onClick: (() -> Unit)? = null) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BisqTheme.colors.backgroundColor.copy(alpha = 0.85f))
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { /* consume clicks */ }
    ) {
        Surface(
            shape = RoundedCornerShape(BisqUIConstants.ScreenPadding),
            color = BisqTheme.colors.dark_grey40,
            modifier = Modifier.align(Alignment.Center)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding4X,
                    vertical = BisqUIConstants.ScreenPadding2X
                ),
        ) {
            Column(
                modifier = Modifier.padding(
                    horizontal = BisqUIConstants.ScreenPadding2X,
                    vertical = BisqUIConstants.ScreenPadding4X
                ),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                BisqText.h3Light(
                    text = "mobile.connectivity.reconnecting.title".i18n(),
                    color = BisqTheme.colors.white,
                    textAlign = TextAlign.Center
                )

                BisqGap.VQuarter()
                CircularProgressIndicator(
                    color = BisqTheme.colors.primary,
                    modifier = Modifier.size(70.dp),
                    strokeWidth = 1.dp
                )
                BisqGap.VQuarter()

                BisqText.largeLight(
                    text = "mobile.connectivity.reconnecting.info".i18n(),
                    color = BisqTheme.colors.light_grey50,
                    textAlign = TextAlign.Center
                )

                BisqText.baseLight(
                    text = "mobile.connectivity.reconnecting.details".i18n(),
                    color = BisqTheme.colors.light_grey50,
                    textAlign = TextAlign.Center
                )
                BisqGap.VHalf()
                BisqButton(
                    text = "mobile.connectivity.reconnecting.restart".i18n(),
                    type = BisqButtonType.Outline,
                    onClick = onClick
                )
            }
        }
    }
}
