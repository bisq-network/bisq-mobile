package network.bisq.mobile.node.startup.splash

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.network.NetworkServiceFacade
import network.bisq.mobile.data.service.settings.SettingsServiceFacade
import network.bisq.mobile.data.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.repository.SettingsRepository
import network.bisq.mobile.domain.utils.VersionProvider
import network.bisq.mobile.domain.utils.combine
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.i18n.uiString
import network.bisq.mobile.node.common.domain.service.bootstrap.NodeApplicationBootstrapFacade
import network.bisq.mobile.node.common.domain.service.bootstrap.NodeApplicationBootstrapFacade.BootstrapPhase
import network.bisq.mobile.presentation.main.MainPresenter
import network.bisq.mobile.presentation.startup.splash.SplashPresenter
import network.bisq.mobile.presentation.startup.splash.SplashUiState

class NodeSplashPresenter(
    mainPresenter: MainPresenter,
    private val nodeApplicationBootstrapFacade: NodeApplicationBootstrapFacade,
    userProfileService: UserProfileServiceFacade,
    settingsRepository: SettingsRepository,
    settingsServiceFacade: SettingsServiceFacade,
    networkServiceFacade: NetworkServiceFacade,
    versionProvider: VersionProvider,
) : SplashPresenter(
        mainPresenter,
        nodeApplicationBootstrapFacade,
        userProfileService,
        settingsRepository,
        settingsServiceFacade,
        versionProvider,
    ) {
    private val appNameAndVersion = versionProvider.getAppNameAndVersion(ApplicationBootstrapFacade.isDemo, false)

    override val state: StateFlow<String> = nodeApplicationBootstrapFacade.state

    private val _nodeUiState =
        MutableStateFlow(NodeSplashUiState(splashUiState = SplashUiState(appNameAndVersion = appNameAndVersion)))
    val nodeUiState: StateFlow<NodeSplashUiState> = _nodeUiState.asStateFlow()

    private val _slowPathUiState = MutableStateFlow(SlowPathUiState())
    val slowPathUiState: StateFlow<SlowPathUiState> = _slowPathUiState.asStateFlow()

    private val numConnections: StateFlow<Int> = networkServiceFacade.numConnections
    private val allDataReceived: StateFlow<Boolean> = networkServiceFacade.allDataReceived

    override fun onViewAttached() {
        super.onViewAttached()

        // isBootstrapFailed / torBootstrapFailed are the source of truth for the two failure UIs.
        // The typed combine below is already at its 6-arg max, so fold the two failure flags into a
        // single input here rather than inferring Tor failure from SplashPresenter.resolveActiveDialog's
        // dialog priority (which would silently break this path if that priority were reordered).
        val failureFlags =
            combine(
                nodeApplicationBootstrapFacade.isBootstrapFailed,
                nodeApplicationBootstrapFacade.torBootstrapFailed,
            ) { appFailed, torFailed -> BootstrapFailureFlags(appFailed, torFailed) }

        presenterScope.launch {
            // 6-arg combine: use the project's typed combine (kotlinx only types up to 5);
            // imported kotlinx.combine stays for the 3-arg slow-path collector below.
            combine(
                uiState,
                nodeApplicationBootstrapFacade.bootstrapPhase,
                nodeApplicationBootstrapFacade.torBootstrapProgress,
                numConnections,
                allDataReceived,
                failureFlags,
            ) { splashUiState, phase, torProgress, peerCount, dataReceived, failure ->
                buildNodeUiState(
                    splashUiState = splashUiState,
                    phase = phase,
                    torProgress = torProgress,
                    peerCount = peerCount,
                    dataReceived = dataReceived,
                    appBootstrapFailed = failure.appBootstrapFailed,
                    torBootstrapFailed = failure.torBootstrapFailed,
                )
            }.collect { nodeUiState ->
                _nodeUiState.value = nodeUiState
            }
        }

        // Kept separate from nodeUiState so the per-second elapsed tick recomposes only the
        // slow-path banner, not the whole step list. Hidden once bootstrap has failed (the
        // "still connecting, this is normal" reassurance would contradict the failure state),
        // and gated to SlowPathUiState() below the threshold so the StateFlow dedupes — nothing
        // is emitted before the banner should appear.
        presenterScope.launch {
            combine(
                nodeApplicationBootstrapFacade.bootstrapElapsedSeconds,
                nodeApplicationBootstrapFacade.torBootstrapFailed,
                nodeApplicationBootstrapFacade.isBootstrapFailed,
            ) { elapsed, torFailed, bootstrapFailed ->
                val failed = torFailed || bootstrapFailed
                if (!failed && elapsed >= SLOW_PATH_THRESHOLD_SECONDS) {
                    SlowPathUiState(isVisible = true, elapsedSeconds = elapsed)
                } else {
                    SlowPathUiState()
                }
            }.collect { slowPathUiState ->
                _slowPathUiState.value = slowPathUiState
            }
        }
    }

    private fun buildNodeUiState(
        splashUiState: SplashUiState,
        phase: BootstrapPhase,
        torProgress: Int,
        peerCount: Int,
        dataReceived: Boolean,
        appBootstrapFailed: Boolean,
        torBootstrapFailed: Boolean,
    ): NodeSplashUiState {
        // A general bootstrap failure supersedes the inline Tor-failure UI, mirroring
        // SplashPresenter's dialog priority (isBootstrapFailed outranks torBootstrapFailed) —
        // but sourced from the facade flags so this no longer depends on resolveActiveDialog.
        val torFailed = torBootstrapFailed && !appBootstrapFailed
        val title = titleFor(phase, torFailed, appBootstrapFailed)
        val safePeerCount = peerCount.coerceAtLeast(0)

        return NodeSplashUiState(
            splashUiState =
                splashUiState.copy(
                    appNameAndVersion = appNameAndVersion,
                ),
            title = title,
            subtitle = subtitleFor(phase, torFailed, appBootstrapFailed),
            steps = buildSteps(phase, torProgress, safePeerCount, dataReceived, torFailed),
            showTorFailureActions = torFailed,
        )
    }

    private fun buildSteps(
        phase: BootstrapPhase,
        torProgress: Int,
        peerCount: Int,
        dataReceived: Boolean,
        torFailed: Boolean,
    ): List<NodeBootstrapStep> =
        listOf(
            NodeBootstrapStep(
                icon = NodeBootstrapStepIcon.TOR,
                label = if (torFailed) UiString("mobile.bootstrap.node.step.tor.failed.label") else UiString("mobile.bootstrap.node.step.tor"),
                detail = torDetail(phase, torProgress, torFailed),
                status = torStatus(phase, torFailed),
            ),
            NodeBootstrapStep(
                icon = NodeBootstrapStepIcon.PEERS,
                label = UiString("mobile.bootstrap.node.step.peers"),
                detail = peerDetail(phase, peerCount),
                status = peerStatus(phase),
            ),
            NodeBootstrapStep(
                icon = NodeBootstrapStepIcon.DATA,
                label = UiString("mobile.bootstrap.node.step.data"),
                detail = dataDetail(phase, dataReceived),
                status = dataStatus(phase, dataReceived),
            ),
            NodeBootstrapStep(
                icon = NodeBootstrapStepIcon.READY,
                label = UiString("mobile.bootstrap.node.step.ready"),
                detail = UiString(""),
                status = if (phase == BootstrapPhase.APP_INITIALIZED) NodeBootstrapStepStatus.DONE else NodeBootstrapStepStatus.PENDING,
            ),
        )

    // The Tor connection is complete once bootstrap has advanced past app initialization.
    // Exhaustive when (no ordinal arithmetic) so reordering or adding a BootstrapPhase is a
    // compile error here rather than a silent behavior change.
    private fun isTorStageDone(phase: BootstrapPhase): Boolean =
        when (phase) {
            BootstrapPhase.INITIALIZE_APP -> false
            BootstrapPhase.INITIALIZE_NETWORK,
            BootstrapPhase.INITIALIZE_SERVICES,
            BootstrapPhase.APP_INITIALIZED,
            -> true
        }

    private fun torStatus(
        phase: BootstrapPhase,
        torFailed: Boolean,
    ): NodeBootstrapStepStatus =
        when {
            torFailed -> NodeBootstrapStepStatus.FAILED
            isTorStageDone(phase) -> NodeBootstrapStepStatus.DONE
            else -> NodeBootstrapStepStatus.IN_PROGRESS
        }

    private fun peerStatus(phase: BootstrapPhase): NodeBootstrapStepStatus =
        when (phase) {
            BootstrapPhase.INITIALIZE_APP -> NodeBootstrapStepStatus.PENDING
            BootstrapPhase.INITIALIZE_NETWORK -> NodeBootstrapStepStatus.IN_PROGRESS
            BootstrapPhase.INITIALIZE_SERVICES,
            BootstrapPhase.APP_INITIALIZED,
            -> NodeBootstrapStepStatus.DONE
        }

    // Data sync only begins once peers are connected (INITIALIZE_SERVICES), so gate on phase rather
    // than a phase-independent dataReceived — otherwise the data step could report DONE while the
    // peers step above it is still IN_PROGRESS. Exhaustive when so a new BootstrapPhase is a compile error.
    private fun dataStatus(
        phase: BootstrapPhase,
        dataReceived: Boolean,
    ): NodeBootstrapStepStatus =
        when (phase) {
            BootstrapPhase.INITIALIZE_APP,
            BootstrapPhase.INITIALIZE_NETWORK,
            -> NodeBootstrapStepStatus.PENDING
            BootstrapPhase.INITIALIZE_SERVICES ->
                if (dataReceived) NodeBootstrapStepStatus.DONE else NodeBootstrapStepStatus.IN_PROGRESS
            BootstrapPhase.APP_INITIALIZED -> NodeBootstrapStepStatus.DONE
        }

    private fun torDetail(
        phase: BootstrapPhase,
        torProgress: Int,
        torFailed: Boolean,
    ): UiString =
        when {
            torFailed -> UiString("mobile.bootstrap.node.step.tor.failed.detail")
            isTorStageDone(phase) -> UiString("mobile.bootstrap.node.step.tor.done.detail")
            else -> uiString("mobile.bootstrap.node.step.tor.detail", torProgress)
        }

    private fun peerDetail(
        phase: BootstrapPhase,
        peerCount: Int,
    ): UiString =
        when (phase) {
            BootstrapPhase.INITIALIZE_NETWORK -> uiString("mobile.bootstrap.node.step.peers.detail", peerCount)
            BootstrapPhase.INITIALIZE_SERVICES,
            BootstrapPhase.APP_INITIALIZED,
            -> uiString("mobile.bootstrap.node.step.peers.done.detail", peerCount)

            else -> UiString("")
        }

    private fun dataDetail(
        phase: BootstrapPhase,
        dataReceived: Boolean,
    ): UiString =
        when (phase) {
            BootstrapPhase.INITIALIZE_APP,
            BootstrapPhase.INITIALIZE_NETWORK,
            -> UiString("")
            BootstrapPhase.INITIALIZE_SERVICES ->
                if (dataReceived) {
                    UiString("mobile.bootstrap.node.step.data.done.detail")
                } else {
                    UiString("mobile.bootstrap.node.step.data.detail")
                }
            BootstrapPhase.APP_INITIALIZED -> UiString("mobile.bootstrap.node.step.data.done.detail")
        }

    private fun titleFor(
        phase: BootstrapPhase,
        torFailed: Boolean,
        appBootstrapFailed: Boolean,
    ): UiString =
        when {
            torFailed -> UiString("mobile.bootstrap.node.title.torFailed")
            appBootstrapFailed -> UiString("mobile.bootstrap.node.title.failed")
            phase == BootstrapPhase.INITIALIZE_NETWORK -> UiString("mobile.bootstrap.node.title.peers")
            phase == BootstrapPhase.INITIALIZE_SERVICES -> UiString("mobile.bootstrap.node.title.data")
            phase == BootstrapPhase.APP_INITIALIZED -> UiString("mobile.bootstrap.node.title.ready")
            else -> UiString("mobile.bootstrap.node.title")
        }

    private fun subtitleFor(
        phase: BootstrapPhase,
        torFailed: Boolean,
        appBootstrapFailed: Boolean,
    ): UiString =
        when {
            torFailed -> UiString("mobile.bootstrap.node.subtitle.failed")
            appBootstrapFailed -> UiString("mobile.bootstrap.node.subtitle.appFailed")
            phase == BootstrapPhase.INITIALIZE_SERVICES -> UiString("mobile.bootstrap.node.subtitle.data")
            phase == BootstrapPhase.APP_INITIALIZED -> UiString("mobile.bootstrap.node.subtitle.ready")
            else -> UiString("mobile.bootstrap.node.subtitle")
        }

    // Folds the two facade failure flags into one input so the main combine stays at its 6-arg max.
    private data class BootstrapFailureFlags(
        val appBootstrapFailed: Boolean,
        val torBootstrapFailed: Boolean,
    )

    companion object {
        // Show the slow-path reassurance banner once bootstrap has been running this long
        // (wall-clock from bootstrap start). Mirrors the previous 75_000ms facade threshold.
        private const val SLOW_PATH_THRESHOLD_SECONDS = 75L
    }
}
