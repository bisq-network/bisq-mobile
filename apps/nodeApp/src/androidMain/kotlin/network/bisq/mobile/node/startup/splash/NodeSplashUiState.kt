package network.bisq.mobile.node.startup.splash

import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.presentation.startup.splash.SplashUiState

data class NodeSplashUiState(
    val splashUiState: SplashUiState = SplashUiState(),
    val title: UiString = UiString(""),
    val subtitle: UiString = UiString(""),
    val steps: List<NodeBootstrapStep> = emptyList(),
    val showTorFailureActions: Boolean = false,
)

data class SlowPathUiState(
    val isVisible: Boolean = false,
    val elapsedSeconds: Long = 0L,
)

data class NodeBootstrapStep(
    val icon: String,
    val label: UiString,
    val detail: UiString,
    val status: NodeBootstrapStepStatus,
)

object NodeBootstrapStepIcon {
    const val TOR = "1"
    const val PEERS = "2"
    const val DATA = "3"
    const val READY = "★"
}

enum class NodeBootstrapStepStatus {
    PENDING,
    IN_PROGRESS,
    DONE,
    FAILED,
}
