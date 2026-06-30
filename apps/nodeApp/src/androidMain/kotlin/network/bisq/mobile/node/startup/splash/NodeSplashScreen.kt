package network.bisq.mobile.node.startup.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.uiString
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogoGrey
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.startup.splash.SplashActiveDialog
import network.bisq.mobile.presentation.startup.splash.SplashDialogs
import network.bisq.mobile.presentation.startup.splash.SplashUiAction
import network.bisq.mobile.presentation.startup.splash.SplashUiState
import org.koin.compose.koinInject

@Composable
fun NodeSplashScreen() {
    val presenter: NodeSplashPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.nodeUiState.collectAsState()
    val slowPath by presenter.slowPathUiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        NodeBootstrapContent(
            uiState = uiState,
            slowPath = slowPath,
            onAction = presenter::onAction,
        )
        SplashDialogs(
            uiState = uiState.splashUiState.withoutNodeInlineFailureDialog(),
            onAction = presenter::onAction,
        )
    }
}

private fun SplashUiState.withoutNodeInlineFailureDialog(): SplashUiState =
    if (activeDialog == SplashActiveDialog.TorBootstrapFailed) {
        copy(activeDialog = null)
    } else {
        this
    }

@Composable
private fun NodeBootstrapContent(
    uiState: NodeSplashUiState,
    slowPath: SlowPathUiState,
    onAction: (SplashUiAction) -> Unit,
) {
    BoxWithConstraints(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BisqTheme.colors.backgroundColor),
    ) {
        // The whole screen scrolls so the steps, recovery actions and progress bar stay reachable on
        // short screens / large font scales. heightIn(min = viewport) + SpaceBetween keeps the hero
        // block at the top and the progress bar pinned to the bottom when content fits, and lets
        // everything scroll once it overflows. Note: Modifier.weight does not work inside
        // verticalScroll, which is why the layout uses SpaceBetween over a min-height column instead of
        // weighted spacers.
        val viewportHeight = maxHeight
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = BisqUIConstants.ScreenPadding2X),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = viewportHeight),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween,
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BisqGap.V3()
                    BisqLogoGrey(modifier = Modifier.size(80.dp))
                    BisqGap.V1()
                    BisqText.BaseLight(
                        text = uiState.splashUiState.appNameAndVersion,
                        color = BisqTheme.colors.mid_grey20,
                    )
                    BisqGap.V3()
                    BisqText.H5Light(
                        text = uiState.title.i18n(),
                        color = BisqTheme.colors.white,
                        textAlign = TextAlign.Center,
                    )
                    BisqGap.VHalf()
                    BisqText.SmallLight(
                        text = uiState.subtitle.i18n(),
                        color = BisqTheme.colors.mid_grey20,
                        textAlign = TextAlign.Center,
                    )
                    BisqGap.V3()
                    NodeStepList(steps = uiState.steps)

                    if (uiState.showTorFailureActions) {
                        TorFailureActions(
                            onRestartTor = { onAction(SplashUiAction.OnRestartTor) },
                            onPurgeRestart = { onAction(SplashUiAction.OnPurgeRestartTor) },
                        )
                    }

                    if (slowPath.isVisible) {
                        BisqGap.V2()
                        SlowPathBanner(elapsedSeconds = slowPath.elapsedSeconds)
                    }
                }

                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    BisqGap.V2()
                    BisqProgressBar(
                        progress = uiState.splashUiState.progress,
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 40.dp)
                                .height(2.dp),
                    )
                    BisqGap.VHalf()
                }
            }
        }
    }
}

@Composable
private fun NodeStepList(steps: List<NodeBootstrapStep>) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey30)
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingHalf),
    ) {
        steps.forEachIndexed { index, step ->
            NodeBootstrapStepRow(step = step)
            if (index < steps.lastIndex) {
                Row {
                    Spacer(modifier = Modifier.width(13.dp))
                    Box(
                        modifier =
                            Modifier
                                .width(2.dp)
                                .height(BisqUIConstants.ScreenPaddingHalf)
                                .background(BisqTheme.colors.dark_grey50),
                    )
                }
            }
        }
    }
}

@Composable
private fun NodeBootstrapStepRow(step: NodeBootstrapStep) {
    val isActive = step.status == NodeBootstrapStepStatus.IN_PROGRESS
    val isDone = step.status == NodeBootstrapStepStatus.DONE
    val isFailed = step.status == NodeBootstrapStepStatus.FAILED
    val isPending = step.status == NodeBootstrapStepStatus.PENDING

    val dotColor =
        when {
            isDone -> BisqTheme.colors.primary
            isFailed -> BisqTheme.colors.danger
            isActive -> BisqTheme.colors.primaryHover
            else -> BisqTheme.colors.mid_grey10
        }

    val labelColor =
        when {
            isPending -> BisqTheme.colors.mid_grey10
            isFailed -> BisqTheme.colors.danger
            else -> BisqTheme.colors.white
        }

    val detailColor =
        when {
            isPending -> BisqTheme.colors.dark_grey50
            isFailed -> BisqTheme.colors.danger
            isActive -> BisqTheme.colors.primary65
            else -> BisqTheme.colors.mid_grey20
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BisqUIConstants.ScreenPaddingHalf),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(dotColor),
            contentAlignment = Alignment.Center,
        ) {
            when {
                isDone ->
                    BisqText.XSmallMedium(
                        text = "✓",
                        color = BisqTheme.colors.backgroundColor,
                    )

                isFailed ->
                    BisqText.XSmallMedium(
                        text = "✕",
                        color = BisqTheme.colors.white,
                    )

                isActive ->
                    BisqText.XSmallLight(
                        text = step.icon,
                        color = BisqTheme.colors.backgroundColor,
                    )

                else ->
                    BisqText.XSmallLight(
                        text = step.icon,
                        color = BisqTheme.colors.mid_grey20,
                    )
            }
        }

        BisqGap.H2()

        Column(modifier = Modifier.weight(1f)) {
            BisqText.BaseRegular(
                text = step.label.i18n(),
                color = labelColor,
            )
            val detail = step.detail.i18n()
            if (detail.isNotEmpty()) {
                BisqGap.VQuarter()
                BisqText.XSmallLight(
                    text = detail,
                    color = detailColor,
                )
            }
        }

        if (isActive) {
            BisqGap.H1()
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(BisqTheme.colors.primary),
            )
        }
    }
}

@Composable
private fun SlowPathBanner(elapsedSeconds: Long) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.yellow50)
                .border(
                    width = 1.dp,
                    color = BisqTheme.colors.yellow30,
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                ).padding(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.SmallMedium(
            text = "mobile.bootstrap.slowPath.title".i18n(),
            color = BisqTheme.colors.yellow,
        )
        BisqGap.VHalf()

        BisqText.XSmallLight(
            text = "mobile.bootstrap.slowPath.elapsed".i18n(elapsedSeconds),
            color = BisqTheme.colors.yellow10,
        )
        BisqGap.VHalf()
        BisqText.XSmallLight(
            text = "mobile.bootstrap.slowPath.body".i18n(),
            color = BisqTheme.colors.yellow,
        )
    }
}

@Composable
private fun TorFailureActions(
    onRestartTor: () -> Unit,
    onPurgeRestart: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqGap.V2()
        BisqButton(
            text = "mobile.bootstrap.node.failure.restartTor".i18n(),
            onClick = onRestartTor,
            fullWidth = true,
            type = BisqButtonType.Outline,
        )
        BisqGap.V1()
        BisqButton(
            text = "mobile.bootstrap.node.failure.clearTorData".i18n(),
            onClick = onPurgeRestart,
            fullWidth = true,
            type = BisqButtonType.Danger,
        )
        BisqGap.VHalf()
        BisqText.XSmallLight(
            text = "mobile.bootstrap.node.failure.hint".i18n(),
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

private fun previewNodeSplashUiState(
    title: UiString,
    subtitle: UiString = UiString("mobile.bootstrap.node.subtitle"),
    progress: Float,
    steps: List<NodeBootstrapStep>,
    showTorFailureActions: Boolean = false,
): NodeSplashUiState =
    NodeSplashUiState(
        splashUiState =
            SplashUiState(
                progress = progress,
                appNameAndVersion = "Bisq Easy v0.5.0",
            ),
        title = title,
        subtitle = subtitle,
        steps = steps,
        showTorFailureActions = showTorFailureActions,
    )

private fun previewNodeSteps(
    torStatus: NodeBootstrapStepStatus = NodeBootstrapStepStatus.PENDING,
    torDetail: UiString = UiString(""),
    torFailed: Boolean = false,
    peersStatus: NodeBootstrapStepStatus = NodeBootstrapStepStatus.PENDING,
    peersDetail: UiString = UiString(""),
    dataStatus: NodeBootstrapStepStatus = NodeBootstrapStepStatus.PENDING,
    dataDetail: UiString = UiString(""),
    readyStatus: NodeBootstrapStepStatus = NodeBootstrapStepStatus.PENDING,
): List<NodeBootstrapStep> =
    listOf(
        NodeBootstrapStep(
            icon = NodeBootstrapStepIcon.TOR,
            label = if (torFailed) UiString("mobile.bootstrap.node.step.tor.failed.label") else UiString("mobile.bootstrap.node.step.tor"),
            detail = torDetail,
            status = torStatus,
        ),
        NodeBootstrapStep(
            icon = NodeBootstrapStepIcon.PEERS,
            label = UiString("mobile.bootstrap.node.step.peers"),
            detail = peersDetail,
            status = peersStatus,
        ),
        NodeBootstrapStep(
            icon = NodeBootstrapStepIcon.DATA,
            label = UiString("mobile.bootstrap.node.step.data"),
            detail = dataDetail,
            status = dataStatus,
        ),
        NodeBootstrapStep(
            icon = NodeBootstrapStepIcon.READY,
            label = UiString("mobile.bootstrap.node.step.ready"),
            detail = UiString(""),
            status = readyStatus,
        ),
    )

@Composable
private fun NodeSplashPreview(
    uiState: NodeSplashUiState,
    slowPath: SlowPathUiState = SlowPathUiState(),
) {
    BisqTheme.Preview {
        NodeBootstrapContent(
            uiState = uiState,
            slowPath = slowPath,
            onAction = {},
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Node bootstrap - Tor starting")
@Composable
private fun NodeSplash_TorStartingPreview() {
    NodeSplashPreview(
        previewNodeSplashUiState(
            title = UiString("mobile.bootstrap.node.title"),
            progress = 0.15f,
            steps =
                previewNodeSteps(
                    torStatus = NodeBootstrapStepStatus.IN_PROGRESS,
                    torDetail = uiString("mobile.bootstrap.node.step.tor.detail", 50),
                ),
        ),
    )
}

@ExcludeFromCoverage
@Preview(name = "Node bootstrap - Connecting to P2P")
@Composable
private fun NodeSplash_P2PConnectingPreview() {
    NodeSplashPreview(
        previewNodeSplashUiState(
            title = UiString("mobile.bootstrap.node.title.peers"),
            progress = 0.5f,
            steps =
                previewNodeSteps(
                    torStatus = NodeBootstrapStepStatus.DONE,
                    torDetail = UiString("mobile.bootstrap.node.step.tor.done.detail"),
                    peersStatus = NodeBootstrapStepStatus.IN_PROGRESS,
                    peersDetail = uiString("mobile.bootstrap.node.step.peers.detail", 3),
                ),
        ),
    )
}

@ExcludeFromCoverage
@Preview(name = "Node bootstrap - Syncing data")
@Composable
private fun NodeSplash_DataSyncPreview() {
    NodeSplashPreview(
        previewNodeSplashUiState(
            title = UiString("mobile.bootstrap.node.title.data"),
            subtitle = UiString("mobile.bootstrap.node.subtitle.data"),
            progress = 0.75f,
            steps =
                previewNodeSteps(
                    torStatus = NodeBootstrapStepStatus.DONE,
                    torDetail = UiString("mobile.bootstrap.node.step.tor.done.detail"),
                    peersStatus = NodeBootstrapStepStatus.DONE,
                    peersDetail = uiString("mobile.bootstrap.node.step.peers.done.detail", 8),
                    dataStatus = NodeBootstrapStepStatus.IN_PROGRESS,
                    dataDetail = UiString("mobile.bootstrap.node.step.data.detail"),
                ),
        ),
    )
}

@ExcludeFromCoverage
@Preview(name = "Node bootstrap - Ready")
@Composable
private fun NodeSplash_ReadyPreview() {
    NodeSplashPreview(
        previewNodeSplashUiState(
            title = UiString("mobile.bootstrap.node.title.ready"),
            subtitle = UiString("mobile.bootstrap.node.subtitle.ready"),
            progress = 1f,
            steps =
                previewNodeSteps(
                    torStatus = NodeBootstrapStepStatus.DONE,
                    torDetail = UiString("mobile.bootstrap.node.step.tor.done.detail"),
                    peersStatus = NodeBootstrapStepStatus.DONE,
                    peersDetail = uiString("mobile.bootstrap.node.step.peers.done.detail", 8),
                    dataStatus = NodeBootstrapStepStatus.DONE,
                    dataDetail = UiString("mobile.bootstrap.node.step.data.done.detail"),
                    readyStatus = NodeBootstrapStepStatus.DONE,
                ),
        ),
    )
}

@ExcludeFromCoverage
@Preview(name = "Node bootstrap - Slow path")
@Composable
private fun NodeSplash_SlowPathPreview() {
    NodeSplashPreview(
        uiState =
            previewNodeSplashUiState(
                title = UiString("mobile.bootstrap.node.title.peers"),
                progress = 0.35f,
                steps =
                    previewNodeSteps(
                        torStatus = NodeBootstrapStepStatus.DONE,
                        torDetail = UiString("mobile.bootstrap.node.step.tor.done.detail"),
                        peersStatus = NodeBootstrapStepStatus.IN_PROGRESS,
                        peersDetail = uiString("mobile.bootstrap.node.step.peers.detail", 1),
                    ),
            ),
        slowPath = SlowPathUiState(isVisible = true, elapsedSeconds = 76L),
    )
}

@ExcludeFromCoverage
@Preview(name = "Node bootstrap - Tor failure")
@Composable
private fun NodeSplash_TorFailurePreview() {
    NodeSplashPreview(
        previewNodeSplashUiState(
            title = UiString("mobile.bootstrap.node.title.failed"),
            subtitle = UiString("mobile.bootstrap.node.subtitle.failed"),
            progress = 0.08f,
            steps =
                previewNodeSteps(
                    torStatus = NodeBootstrapStepStatus.FAILED,
                    torDetail = UiString("mobile.bootstrap.node.step.tor.failed.detail"),
                    torFailed = true,
                ),
            showTorFailureActions = true,
        ),
    )
}
