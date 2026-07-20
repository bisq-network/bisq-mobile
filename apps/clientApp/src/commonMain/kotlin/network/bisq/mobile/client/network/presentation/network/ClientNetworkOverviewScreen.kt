package network.bisq.mobile.client.network.presentation.network

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
import network.bisq.mobile.presentation.common.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.components.network.HealthBadge
import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState
import network.bisq.mobile.presentation.common.ui.components.network.NetworkSectionLabel
import network.bisq.mobile.presentation.common.ui.components.network.SubPageEntryCard
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware

@Composable
fun ClientNetworkOverviewScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<ClientNetworkOverviewPresenter>()
    val uiState by presenter.uiState.collectAsState()

    ClientNetworkOverviewContent(
        uiState = uiState,
        onAction = presenter::onAction,
        topBar = { TopBar("mobile.more.network".i18n(), showUserAvatar = false) },
    )
}

@Composable
internal fun ClientNetworkOverviewContent(
    uiState: ClientNetworkOverviewUiState,
    onAction: (ClientNetworkOverviewUiAction) -> Unit,
    topBar: @Composable () -> Unit,
) {
    val nodeLabel = uiState.trustedNodeHost ?: "mobile.networkInfo.overview.addressLoading".i18n()
    BisqScaffold(topBar = topBar) { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(paddingValues)
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPadding,
                    ),
            horizontalAlignment = Alignment.Start,
        ) {
            HealthBadge(state = uiState.healthState)

            BisqGap.V1()

            BridgeTopologyCard(
                isReachable = uiState.isReachable,
                onCheckSettings = { onAction(ClientNetworkOverviewUiAction.OnCheckConnectionSettings) },
            )

            NetworkSectionLabel(text = "mobile.networkInfo.connect.trustedNode".i18n())

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .background(BisqTheme.colors.dark_grey40)
                        .padding(horizontal = BisqUIConstants.ScreenPadding),
            ) {
                InfoRow(
                    label = "mobile.networkInfo.connect.endpoint".i18n(),
                    value = nodeLabel,
                    showCopy = uiState.trustedNodeHost != null,
                )
                BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)
                InfoRow(
                    label = "mobile.networkInfo.connect.routing".i18n(),
                    value =
                        if (uiState.isTorRouted) {
                            "mobile.networkInfo.connect.routing.tor".i18n()
                        } else {
                            "mobile.networkInfo.connect.routing.clearnet".i18n()
                        },
                )
                BisqHDivider(verticalPadding = BisqUIConstants.ScreenPaddingHalf)
                InfoRow(
                    label = "mobile.networkInfo.connect.latency".i18n(),
                    value =
                        uiState.latencyMs
                            ?.let { "$it ms" }
                            ?: "mobile.networkInfo.connect.notAvailable".i18n(),
                )
            }

            NetworkSectionLabel(text = "mobile.networkInfo.connect.viaYourNode".i18n())

            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .background(BisqTheme.colors.dark_grey40)
                        .padding(horizontal = BisqUIConstants.ScreenPadding),
            ) {
                InfoRow(
                    label = "mobile.networkInfo.overview.peers".i18n(),
                    value =
                        uiState.peerCountViaNode
                            ?.let { "mobile.networkInfo.overview.connections".i18nPlural(it) }
                            ?: "mobile.networkInfo.connect.notAvailable".i18n(),
                )
            }

            NetworkSectionLabel(text = "mobile.networkInfo.overview.details".i18n())

            SubPageEntryCard(onClick = { onAction(ClientNetworkOverviewUiAction.OnConnectionsClick) }) {
                Column {
                    BisqText.BaseRegular(text = "mobile.networkInfo.connections.title".i18n(), color = BisqTheme.colors.white)
                    BisqGap.VQuarter()
                    BisqText.SmallLight(
                        text = "mobile.networkInfo.connect.connections.subtitle".i18n(),
                        color = BisqTheme.colors.mid_grey20,
                    )
                }
            }

            BisqGap.VHalf()

            SubPageEntryCard(onClick = { onAction(ClientNetworkOverviewUiAction.OnMyConnectionClick) }) {
                Column {
                    BisqText.BaseRegular(text = "mobile.networkInfo.connect.myConnection.title".i18n(), color = BisqTheme.colors.white)
                    BisqGap.VQuarter()
                    BisqText.SmallLight(
                        text = "mobile.networkInfo.connect.myConnection.subtitle".i18n(),
                        color = BisqTheme.colors.mid_grey20,
                    )
                }
            }
        }
    }
}

@Composable
private fun BridgeTopologyCard(
    isReachable: Boolean,
    onCheckSettings: () -> Unit,
) {
    val nodeColor = if (isReachable) BisqTheme.colors.primary else BisqTheme.colors.danger
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding2X,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqText.XSmallMedium(
            text = "mobile.networkInfo.connect.topology.connectedVia".i18n(),
            color = BisqTheme.colors.mid_grey20,
        )
        BisqGap.V1()

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            TopologyBox(label = "mobile.networkInfo.connect.topology.you".i18n(), color = BisqTheme.colors.mid_grey30)
            BisqGap.H1()
            BisqText.XSmallLight(text = "→", color = BisqTheme.colors.mid_grey20)
            BisqGap.H1()
            TopologyBox(
                label = "mobile.networkInfo.connect.trustedNode".i18n(),
                color = nodeColor,
                modifier = Modifier.widthIn(max = 140.dp),
            )
            BisqGap.H1()
            BisqText.XSmallLight(text = "→", color = BisqTheme.colors.mid_grey20)
            BisqGap.H1()
            TopologyBox(label = "mobile.networkInfo.connect.topology.network".i18n(), color = BisqTheme.colors.mid_grey30)
        }

        BisqGap.V1()

        BisqText.XSmallLight(
            text =
                if (isReachable) {
                    "mobile.networkInfo.connect.topology.safe".i18n()
                } else {
                    "mobile.networkInfo.connect.topology.lost".i18n()
                },
            color = if (isReachable) BisqTheme.colors.mid_grey20 else BisqTheme.colors.danger,
            textAlign = TextAlign.Center,
        )

        if (!isReachable) {
            BisqGap.V1()
            BisqText.SmallRegular(
                text = "mobile.networkInfo.connect.checkSettings".i18n(),
                color = BisqTheme.colors.mid_grey30,
                modifier = Modifier.clickable { onCheckSettings() },
            )
        }
    }
}

@Composable
private fun TopologyBox(
    label: String,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadiusSmall))
                .background(BisqTheme.colors.dark_grey50)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqText.StyledText(
            text = label,
            style = BisqTheme.typography.xsmallMedium,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun InfoRow(
    label: String,
    value: String,
    showCopy: Boolean = false,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BisqUIConstants.ScreenPadding),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.SmallRegular(text = label, color = BisqTheme.colors.mid_grey30)
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf, Alignment.End),
        ) {
            AutoResizeText(
                text = value,
                textStyle = BisqTheme.typography.smallRegular,
                color = BisqTheme.colors.white,
                textAlign = TextAlign.End,
                maxLines = 2,
                modifier = Modifier.weight(1f, fill = false),
            )
            if (showCopy) {
                CopyIconButton(value)
            }
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun ClientNetworkOverviewReachablePreview() {
    BisqTheme.Preview {
        ClientNetworkOverviewContent(
            uiState =
                ClientNetworkOverviewUiState(
                    trustedNodeHost = "r7m2xpqowg3bvf8t.onion",
                    isReachable = true,
                    isTorRouted = true,
                    peerCountViaNode = 12,
                    healthState = NetworkHealthState.HEALTHY,
                ),
            onAction = {},
            topBar = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun ClientNetworkOverviewUnreachablePreview() {
    BisqTheme.Preview {
        ClientNetworkOverviewContent(
            uiState = ClientNetworkOverviewUiState(trustedNodeHost = "r7m2xpqowg3bvf8t.onion"),
            onAction = {},
            topBar = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun BridgeTopologyCardReachablePreview() {
    BisqTheme.Preview {
        BridgeTopologyCard(isReachable = true, onCheckSettings = {})
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun BridgeTopologyCardUnreachablePreview() {
    BisqTheme.Preview {
        BridgeTopologyCard(isReachable = false, onCheckSettings = {})
    }
}
