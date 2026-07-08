package network.bisq.mobile.client.splash

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.UiString
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqProgressBar
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.BisqLogoGrey
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.startup.splash.SplashDialogs
import network.bisq.mobile.presentation.startup.splash.SplashUiState
import org.koin.compose.koinInject

@Composable
fun ClientSplashScreen(route: NavRoute.Splash = NavRoute.Splash()) {
    val presenter: ClientSplashPresenter = koinInject()
    remember(route) { presenter.applyRoute(route) }
    RememberPresenterLifecycle(presenter)

    val uiState by presenter.clientUiState.collectAsState()

    Box(modifier = Modifier.fillMaxSize()) {
        ClientSplashContent(uiState = uiState)

        SplashDialogs(
            uiState = uiState.splashUiState,
            onAction = presenter::onAction,
        )
    }
}

@Composable
private fun ClientSplashContent(uiState: ClientSplashUiState) {
    BootstrapShell(
        title = uiState.title.i18n(),
        subtitle = uiState.subtitle.i18n(),
        appVersion = uiState.splashUiState.appNameAndVersion,
        progress = uiState.progress,
    ) {
        ConnectBootstrapStrip(
            connectingLabel = "mobile.bootstrap.connect.step.connecting".i18n(),
            loadingDataLabel = "mobile.bootstrap.connect.step.loadingData".i18n(),
            connectingDone = uiState.connectingDone,
            loadingDataActive = uiState.loadingDataActive,
            loadingDataDone = uiState.loadingDataDone,
            connectingDetail = uiState.connectingDetail.i18n(),
            loadingDetail = uiState.loadingDetail.i18n(),
        )
    }
}

/**
 * Full-screen bootstrap shell: logo/version header at top, phase content vertically centred, and the
 * overall progress bar pinned to the bottom above the status line.
 */
@Composable
private fun BootstrapShell(
    title: String,
    subtitle: String,
    appVersion: String,
    progress: Float,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BisqTheme.colors.backgroundColor)
                .padding(horizontal = BisqUIConstants.ScreenPadding2X),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.weight(0.12f))
        BisqLogoGrey(modifier = Modifier.size(80.dp))
        BisqGap.V1()
        BisqText.BaseLight(
            text = appVersion,
            color = BisqTheme.colors.mid_grey20,
        )
        Spacer(modifier = Modifier.weight(0.08f))

        BisqText.H5Light(
            text = title,
            color = BisqTheme.colors.white,
            textAlign = TextAlign.Center,
        )
        BisqGap.VHalf()
        BisqText.SmallLight(
            text = subtitle,
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.weight(0.1f))

        content()

        Spacer(modifier = Modifier.weight(1f))

        BisqProgressBar(
            progress = progress,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 40.dp)
                    .height(2.dp),
        )
        BisqGap.VHalf()
    }
}

/**
 * Connect bootstrap: two phases only (Connect WebSocket → Load data). A horizontal strip of
 * dot + label, a connector line, dot + label. Deliberately simpler than the Node step list —
 * there is no local Tor, no peer graph, no inventory download shown to the user.
 */
@Composable
private fun ConnectBootstrapStrip(
    connectingLabel: String,
    loadingDataLabel: String,
    connectingDone: Boolean,
    loadingDataActive: Boolean,
    loadingDataDone: Boolean,
    connectingDetail: String,
    loadingDetail: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            ConnectPhaseNode(
                label = connectingLabel,
                done = connectingDone,
                active = !connectingDone && !loadingDataActive,
            )
            ConnectPhaseConnector(active = connectingDone)
            ConnectPhaseNode(
                label = loadingDataLabel,
                done = loadingDataDone,
                active = loadingDataActive,
            )
        }

        val activeDetail = if (connectingDone) loadingDetail else connectingDetail
        if (activeDetail.isNotEmpty()) {
            BisqGap.V1()
            BisqText.SmallLight(
                text = activeDetail,
                color = BisqTheme.colors.mid_grey20,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun ConnectPhaseNode(
    label: String,
    done: Boolean,
    active: Boolean,
) {
    val bgColor =
        when {
            done -> BisqTheme.colors.primary
            active -> BisqTheme.colors.primaryHover
            else -> BisqTheme.colors.dark_grey40
        }
    val markColor =
        when {
            done || active -> BisqTheme.colors.backgroundColor
            else -> BisqTheme.colors.mid_grey10
        }

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(bgColor),
            contentAlignment = Alignment.Center,
        ) {
            if (done) {
                BisqText.SmallMedium(text = "✓", color = markColor)
            } else {
                Box(
                    modifier =
                        Modifier
                            .size(10.dp)
                            .clip(CircleShape)
                            .background(markColor),
                )
            }
        }
        BisqGap.VQuarter()
        BisqText.XSmallLight(
            text = label,
            color =
                when {
                    done -> BisqTheme.colors.mid_grey20
                    active -> BisqTheme.colors.white
                    else -> BisqTheme.colors.mid_grey10
                },
        )
    }
}

@Composable
private fun ConnectPhaseConnector(active: Boolean) {
    Box(
        modifier =
            Modifier
                .width(60.dp)
                .padding(bottom = 18.dp) // offset to align with circle centres
                .height(2.dp)
                .background(if (active) BisqTheme.colors.primary else BisqTheme.colors.dark_grey40),
    )
}

// ==================================================================================
// Previews
// ==================================================================================

private fun previewClientUiState(
    title: UiString,
    subtitle: UiString,
    progress: Float,
    connectingDone: Boolean = false,
    loadingDataActive: Boolean = false,
    loadingDataDone: Boolean = false,
    connectingDetail: UiString = UiString(""),
    loadingDetail: UiString = UiString(""),
): ClientSplashUiState =
    ClientSplashUiState(
        splashUiState = SplashUiState(appNameAndVersion = "Bisq Connect v0.6.3"),
        progress = progress,
        title = title,
        subtitle = subtitle,
        connectingDone = connectingDone,
        loadingDataActive = loadingDataActive,
        loadingDataDone = loadingDataDone,
        connectingDetail = connectingDetail,
        loadingDetail = loadingDetail,
    )

@ExcludeFromCoverage
@Preview(name = "Connect 1: connecting")
@Composable
private fun ClientSplash_Connecting_Preview() {
    BisqTheme.Preview {
        ClientSplashContent(
            uiState =
                previewClientUiState(
                    title = UiString("mobile.bootstrap.connect.title"),
                    subtitle = UiString("mobile.bootstrap.connect.subtitle"),
                    progress = 0.3f,
                    connectingDetail = UiString("mobile.bootstrap.connect.step.connecting.detail"),
                ),
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Connect 2: loading data")
@Composable
private fun ClientSplash_LoadingData_Preview() {
    BisqTheme.Preview {
        ClientSplashContent(
            uiState =
                previewClientUiState(
                    title = UiString("mobile.bootstrap.connect.title.loadingData"),
                    subtitle = UiString("mobile.bootstrap.connect.subtitle.loadingData"),
                    progress = 0.6f,
                    connectingDone = true,
                    loadingDataActive = true,
                    loadingDetail = UiString("mobile.bootstrap.connect.step.loadingData.detail"),
                ),
        )
    }
}

@ExcludeFromCoverage
@Preview(name = "Connect 3: done")
@Composable
private fun ClientSplash_Done_Preview() {
    BisqTheme.Preview {
        ClientSplashContent(
            uiState =
                previewClientUiState(
                    title = UiString("mobile.bootstrap.connect.title.done"),
                    subtitle = UiString("mobile.bootstrap.connect.subtitle.done"),
                    progress = 1f,
                    connectingDone = true,
                    loadingDataDone = true,
                ),
        )
    }
}
