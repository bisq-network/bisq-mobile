package network.bisq.mobile.presentation.main

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.systemBars
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.navigation.NavHostController
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import network.bisq.mobile.data.utils.setDefaultLocale
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.alert.AlertNotificationBannerPresenter
import network.bisq.mobile.presentation.common.ui.alert.banner.AlertNotificationBanner
import network.bisq.mobile.presentation.common.ui.alert.dialog.AlertNotificationDialog
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.base.SnackbarAction
import network.bisq.mobile.presentation.common.ui.base.ViewPresenter
import network.bisq.mobile.presentation.common.ui.components.SwipeBackIOSNavigationHandler
import network.bisq.mobile.presentation.common.ui.components.context.LocalAnimationsEnabled
import network.bisq.mobile.presentation.common.ui.components.context.LocalExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.context.asExternalUrlOpener
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.LoadingOverlay
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.ReconnectingOverlay
import network.bisq.mobile.presentation.common.ui.components.molecules.dialog.WarningConfirmationDialog
import network.bisq.mobile.presentation.common.ui.components.organisms.BisqSnackbar
import network.bisq.mobile.presentation.common.ui.components.organisms.BisqSnackbarVisuals
import network.bisq.mobile.presentation.common.ui.error.GenericErrorOverlay
import network.bisq.mobile.presentation.common.ui.navigation.ExternalUriHandler
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.presentation.common.ui.network_banner.NetworkStatusBanner
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
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
    content: @Composable () -> Unit,
) {
    // Outer container consumes insets and paints the background
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .consumeWindowInsets(WindowInsets.systemBars) // Eat insets, so no white stripes
                .background(BisqTheme.colors.backgroundColor),
    ) {
        // Inner container adds padding for content
        Box(
            modifier =
                Modifier.fillMaxSize().padding(
                    top = WindowInsets.statusBars.topPaddingDp(),
                    bottom = WindowInsets.navigationBars.bottomPaddingDp(),
                ),
        ) {
            content()
        }
    }
}

/**
 * Main composable view of the application that platforms use to draw.
 */
@ExcludeFromCoverage
@Composable
fun App(
    rootNavController: NavHostController,
    navGraphContent: @Composable () -> Unit,
) {
    val presenter: AppPresenter = koinInject()
    val mainPresenter: MainPresenter = koinInject()
    val externalUrlOpener = remember(mainPresenter) { mainPresenter.asExternalUrlOpener() }
    RememberPresenterLifecycle(presenter)
    val alertNotificationPresenter: AlertNotificationBannerPresenter = koinInject()
    RememberPresenterLifecycle(alertNotificationPresenter)
    val navigationManager: NavigationManager = koinInject()
    val globalUiManager: GlobalUiManager = koinInject()

    val languageCode by presenter.languageCode.collectAsState()
    val showAnimation by presenter.showAnimation.collectAsState()
    val showAllConnectionsLostDialogue by presenter.showAllConnectionsLostDialogue.collectAsState()
    val showReconnectOverlay by presenter.showReconnectOverlay.collectAsState()
    val isLoadingBlocking by globalUiManager.isLoadingBlocking.collectAsState()
    val showLoadingDialog by globalUiManager.showLoadingDialog.collectAsState()

    // Global snackbar state
    val snackbarHostState = remember { SnackbarHostState() }

    // Collect snackbar actions from GlobalUiManager
    LaunchedEffect(Unit) {
        globalUiManager.snackbarActions.collect { action ->
            when (action) {
                is SnackbarAction.Show -> {
                    // Dismiss any existing snackbar first, then show the new one
                    snackbarHostState.currentSnackbarData?.dismiss()
                    // Launch in child coroutine so collector isn't blocked while snackbar shows
                    launch {
                        snackbarHostState.showSnackbar(
                            BisqSnackbarVisuals(
                                message = action.message,
                                type = action.type,
                                duration = action.duration,
                                position = action.position,
                            ),
                        )
                    }
                }
                is SnackbarAction.Dismiss -> {
                    snackbarHostState.currentSnackbarData?.dismiss()
                }
            }
        }
    }

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
                CompositionLocalProvider(
                    LocalAnimationsEnabled provides showAnimation,
                    LocalExternalUrlOpener provides externalUrlOpener,
                ) {
                    Column {
                        NetworkStatusBanner()
                        AlertNotificationBanner(alertNotificationPresenter)
                        navGraphContent()
                    }
                }
            }

            GenericErrorOverlay()

            if (showAllConnectionsLostDialogue) {
                WarningConfirmationDialog(
                    headline = mainPresenter.connectionsLostDialogTitleKey.i18n(),
                    message = mainPresenter.connectionsLostDialogMessageKey.i18n(),
                    confirmButtonText = mainPresenter.reconnectOverlayButtonKey.i18n(),
                    onConfirm = { mainPresenter.onConnectivityRecoveryAction() },
                    onDismiss = { presenter.onCloseConnectionLostDialogue() },
                )
            } else if (showReconnectOverlay) {
                ReconnectingOverlay(
                    onClick = { mainPresenter.onConnectivityRecoveryAction() },
                    infoKey = mainPresenter.reconnectOverlayInfoKey,
                    detailsKey = mainPresenter.reconnectOverlayDetailsKey,
                    buttonTextKey = mainPresenter.reconnectOverlayButtonKey,
                )
            }

            // Global loading overlay - blocks interaction immediately; show/hide dialog after grace delay
            LoadingOverlay(
                isBlocking = isLoadingBlocking,
                showDialog = showLoadingDialog,
            )

            AlertNotificationDialog(alertNotificationPresenter)

            // Global snackbar - displays app-wide snackbar notifications
            BisqSnackbar(snackbarHostState = snackbarHostState)
        }
    }

    DisposableEffect(Unit) {
        ExternalUriHandler.listener = { uri ->
            navigationManager.navigateFromUri(uri)
        }
        onDispose {
            ExternalUriHandler.listener = null
        }
    }
}
