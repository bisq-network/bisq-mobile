package network.bisq.mobile.client.splash

import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.startup.splash.SplashUiState

/**
 * Connect-specific splash state, wrapping the shared [SplashUiState] (progress, status, dialogs)
 * and adding the fields the 2-phase bootstrap strip needs. Holds i18n *keys* as [UiString]; the
 * composable resolves them at render time.
 */
data class ClientSplashUiState(
    val splashUiState: SplashUiState = SplashUiState(),
    val title: UiString = UiString(""),
    val subtitle: UiString = UiString(""),
    // Progress shown on the bottom bar. Derived from the bootstrap phase, NOT from
    // splashUiState.progress: the latter is forced to 1.0 on connection failure to trigger
    // navigation, which would otherwise fill the bar while the strip is still on phase 1.
    val progress: Float = 0f,
    val connectingDone: Boolean = false,
    val loadingDataActive: Boolean = false,
    val loadingDataDone: Boolean = false,
    val connectingDetail: UiString = UiString(""),
    val loadingDetail: UiString = UiString(""),
)
