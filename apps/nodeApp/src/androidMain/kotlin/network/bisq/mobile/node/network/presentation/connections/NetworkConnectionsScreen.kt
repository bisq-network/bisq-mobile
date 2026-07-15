package network.bisq.mobile.node.network.presentation.connections

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.no_connections
import network.bisq.mobile.domain.utils.DateUtils
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
import network.bisq.mobile.node.common.domain.service.network.NodePeerInfo
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware
import org.jetbrains.compose.resources.painterResource

@Composable
fun NetworkConnectionsScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<NetworkConnectionsPresenter>()
    val uiState by presenter.uiState.collectAsState()

    NetworkConnectionsContent(
        uiState = uiState,
        topBar = { TopBar("mobile.networkInfo.connections.title".i18n(), showUserAvatar = false) },
    )
}

@Composable
internal fun NetworkConnectionsContent(
    uiState: NetworkConnectionsUiState,
    topBar: @Composable () -> Unit,
) {
    BisqScaffold(topBar = topBar) { paddingValues ->
        if (uiState.peers.isEmpty()) {
            EmptyState(modifier = Modifier.padding(paddingValues))
        } else {
            LazyColumn(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                        .padding(
                            horizontal = BisqUIConstants.ScreenPadding,
                            vertical = BisqUIConstants.ScreenPadding,
                        ),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                item {
                    BisqText.SmallRegular(
                        text = "mobile.networkInfo.connections.peers".i18nPlural(uiState.peerCount),
                        color = BisqTheme.colors.mid_grey20,
                    )
                }
                items(uiState.peers, key = { it.connectionId }) { peer ->
                    ConnectionCard(peer = peer)
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier = Modifier) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = BisqUIConstants.ScreenPadding2X),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            painter = painterResource(Res.drawable.no_connections),
            contentDescription = null,
            modifier = Modifier.size(BisqUIConstants.ScreenPadding4X),
        )
        BisqGap.V2()
        BisqText.BaseMedium(
            text = "mobile.networkInfo.connections.empty".i18n(),
            color = BisqTheme.colors.mid_grey30,
            textAlign = TextAlign.Center,
        )
        BisqGap.V1()
        BisqText.SmallLight(
            text = "mobile.networkInfo.connections.emptyHint".i18n(),
            color = BisqTheme.colors.mid_grey20,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun ConnectionCard(peer: NodePeerInfo) {
    val directionColor = if (peer.isOutbound) BisqTheme.colors.primary else BisqTheme.colors.mid_grey30
    val directionLabel =
        if (peer.isOutbound) {
            "mobile.networkInfo.connections.outbound".i18n()
        } else {
            "mobile.networkInfo.connections.inbound".i18n()
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(BisqTheme.colors.dark_grey40)
                .padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPadding,
                ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Start,
    ) {
        Box(
            modifier =
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(directionColor),
        )

        BisqGap.H1()

        Column(modifier = Modifier.weight(1f)) {
            BisqText.StyledText(
                text = peer.address,
                style = BisqTheme.typography.smallMedium,
                color = BisqTheme.colors.white,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            BisqGap.VQuarter()
            BisqText.XSmallLight(
                text = DateUtils.toMediumDateTime(peer.establishedAtMillis, includeSeconds = true),
                color = BisqTheme.colors.mid_grey20,
            )
        }

        BisqGap.H1()

        Column(horizontalAlignment = Alignment.End) {
            if (peer.isSeed) {
                Box(
                    modifier =
                        Modifier
                            .border(
                                width = 1.dp,
                                color = BisqTheme.colors.mid_grey10,
                                shape = RoundedCornerShape(BisqUIConstants.BorderRadiusSmall),
                            ).padding(
                                horizontal = BisqUIConstants.ScreenPaddingHalf,
                                vertical = BisqUIConstants.ScreenPaddingQuarter,
                            ),
                ) {
                    BisqText.XSmallLight(
                        text = "mobile.networkInfo.connections.seed".i18n(),
                        color = BisqTheme.colors.mid_grey20,
                    )
                }
                BisqGap.VQuarter()
            }
            BisqText.XSmallLight(text = directionLabel, color = BisqTheme.colors.mid_grey20)
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NetworkConnectionsContentPopulatedPreview() {
    BisqTheme.Preview {
        NetworkConnectionsContent(
            uiState =
                NetworkConnectionsUiState(
                    peerCount = 3,
                    peers =
                        listOf(
                            NodePeerInfo(
                                connectionId = "1",
                                address = "abcd1234efgh5678ijkl.onion:1234",
                                isOutbound = true,
                                establishedAtMillis = 0L,
                                isSeed = true,
                            ),
                            NodePeerInfo(
                                connectionId = "2",
                                address = "mnop9012qrst3456uvwx.onion:1234",
                                isOutbound = false,
                                establishedAtMillis = 0L,
                                isSeed = false,
                            ),
                        ),
                ),
            topBar = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NetworkConnectionsContentEmptyPreview() {
    BisqTheme.Preview {
        NetworkConnectionsContent(
            uiState = NetworkConnectionsUiState(),
            topBar = {},
        )
    }
}
