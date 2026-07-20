package network.bisq.mobile.node.network.presentation.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
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
fun NodeNetworkOverviewScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<NodeNetworkOverviewPresenter>()
    val uiState by presenter.uiState.collectAsState()

    NetworkContent(
        uiState = uiState,
        onAction = presenter::onAction,
        topBar = { TopBar("mobile.more.network".i18n(), showUserAvatar = false) },
    )
}

@Composable
internal fun NetworkContent(
    uiState: NodeNetworkOverviewUiState,
    onAction: (NodeNetworkOverviewUiAction) -> Unit,
    topBar: @Composable () -> Unit,
) {
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
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                HealthBadge(state = uiState.healthState)
            }

            BisqGap.V1()

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                StatChip(
                    label = "mobile.networkInfo.overview.peers".i18n(),
                    value = uiState.peerCount.toString(),
                    valueColor = if (uiState.peerCount > 0) BisqTheme.colors.primary else BisqTheme.colors.danger,
                    modifier = Modifier.weight(1f),
                )
                StatChip(
                    label = "mobile.networkInfo.overview.transport".i18n(),
                    value = "mobile.networkInfo.overview.daemon".i18n(),
                    valueColor = BisqTheme.colors.white,
                    modifier = Modifier.weight(1f),
                )
                StatChip(
                    label = "mobile.networkInfo.overview.daemon".i18n(),
                    value =
                        if (uiState.isTorRunning) {
                            "mobile.networkInfo.overview.torRunning".i18n()
                        } else {
                            "mobile.networkInfo.overview.torStopped".i18n()
                        },
                    valueColor = if (uiState.isTorRunning) BisqTheme.colors.primary else BisqTheme.colors.danger,
                    modifier = Modifier.weight(1f),
                )
            }

            BisqGap.V2()

            NetworkSectionLabel(text = "mobile.networkInfo.overview.details".i18n())
            BisqGap.VHalf()

            SubPageEntryCard(onClick = { onAction(NodeNetworkOverviewUiAction.OnConnectionsClick) }) {
                Column {
                    BisqText.BaseRegular(text = "mobile.networkInfo.connections.title".i18n(), color = BisqTheme.colors.white)
                    BisqGap.VQuarter()
                    BisqText.SmallLight(
                        text = "mobile.networkInfo.overview.connections".i18nPlural(uiState.peerCount),
                        color = BisqTheme.colors.mid_grey20,
                    )
                }
            }

            BisqGap.VHalf()

            SubPageEntryCard(onClick = { onAction(NodeNetworkOverviewUiAction.OnMyNodeClick) }) {
                Column {
                    BisqText.BaseRegular(text = "mobile.networkInfo.myNode.title".i18n(), color = BisqTheme.colors.white)
                    BisqGap.VQuarter()
                    BisqText.StyledText(
                        text =
                            uiState.onionAddress
                                ?: "mobile.networkInfo.overview.addressLoading".i18n(),
                        style = BisqTheme.typography.smallLight,
                        color = BisqTheme.colors.mid_grey20,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
        }
    }
}

@Composable
private fun StatChip(
    label: String,
    value: String,
    valueColor: Color,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        BisqText.XSmallLight(text = label, color = BisqTheme.colors.mid_grey20)
        BisqGap.VHalf()
        BisqText.BaseMedium(text = value, color = valueColor)
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NetworkContentHealthyPreview() {
    BisqTheme.Preview {
        NetworkContent(
            uiState =
                NodeNetworkOverviewUiState(
                    peerCount = 7,
                    isTorRunning = true,
                    onionAddress = "jd4tx3nljykg5z3vqy7w2m8n4p6r0t2u4w6y8a0c2e4g6i8k.onion:1234",
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
private fun NetworkContentOfflinePreview() {
    BisqTheme.Preview {
        NetworkContent(
            uiState = NodeNetworkOverviewUiState(),
            onAction = {},
            topBar = {},
        )
    }
}
