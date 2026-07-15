package network.bisq.mobile.node.network.presentation.my_node

import androidx.compose.foundation.background
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
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CopyIconButton
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBar
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycleBackStackAware

@Composable
fun NetworkMyNodeScreen() {
    val presenter = RememberPresenterLifecycleBackStackAware<NetworkMyNodePresenter>()
    val uiState by presenter.uiState.collectAsState()

    NetworkMyNodeContent(
        uiState = uiState,
        topBar = { TopBar("mobile.networkInfo.myNode.title".i18n(), showUserAvatar = false) },
    )
}

@Composable
internal fun NetworkMyNodeContent(
    uiState: NetworkMyNodeUiState,
    topBar: @Composable () -> Unit,
) {
    val loading = "mobile.networkInfo.overview.addressLoading".i18n()
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
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .background(BisqTheme.colors.dark_grey40)
                        .padding(
                            horizontal = BisqUIConstants.ScreenPadding,
                            vertical = BisqUIConstants.ScreenPadding,
                        ),
            ) {
                NetworkSectionLabel(text = "mobile.networkInfo.myNode.identity".i18n())
                BisqGap.VHalf()
                NetworkInfoRow(
                    label = "mobile.networkInfo.myNode.onionAddress".i18n(),
                    value = uiState.onionAddress ?: loading,
                    showCopy = uiState.onionAddress != null,
                )
                BisqGap.V1()
                NetworkInfoRow(
                    label = "mobile.networkInfo.myNode.keyId".i18n(),
                    value = uiState.keyId ?: loading,
                )

                BisqHDivider(verticalPadding = BisqUIConstants.ScreenPadding)

                NetworkSectionLabel(text = "mobile.networkInfo.myNode.software".i18n())
                BisqGap.VHalf()
                NetworkInfoRow(
                    label = "mobile.networkInfo.myNode.appVersion".i18n(),
                    value = uiState.appVersion,
                )

                BisqHDivider(verticalPadding = BisqUIConstants.ScreenPadding)

                NetworkSectionLabel(text = "mobile.networkInfo.myNode.transport".i18n())
                BisqGap.VHalf()
                NetworkInfoRow(
                    label = "mobile.networkInfo.myNode.torStatus".i18n(),
                    value =
                        if (uiState.isTorRunning) {
                            "mobile.networkInfo.overview.torRunning".i18n()
                        } else {
                            "mobile.networkInfo.overview.torStopped".i18n()
                        },
                )
            }
        }
    }
}

@Composable
private fun NetworkSectionLabel(text: String) {
    BisqText.XSmallMedium(
        text = text.uppercase(),
        color = BisqTheme.colors.mid_grey20,
    )
}

@Composable
private fun NetworkInfoRow(
    label: String,
    value: String,
    showCopy: Boolean = false,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BisqText.SmallRegular(text = label, color = BisqTheme.colors.mid_grey30)
        BisqGap.VQuarter()
        if (showCopy) {
            Row(verticalAlignment = Alignment.Top) {
                BisqText.SmallRegular(
                    text = value,
                    color = BisqTheme.colors.white,
                    modifier = Modifier.weight(1f),
                )
                CopyIconButton(value)
            }
        } else {
            BisqText.SmallRegular(text = value, color = BisqTheme.colors.white)
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NetworkMyNodeContentPopulatedPreview() {
    BisqTheme.Preview {
        NetworkMyNodeContent(
            uiState =
                NetworkMyNodeUiState(
                    onionAddress = "jd4tx3nljykg5z3vbqrd6fkwpouneimxsacyt2q7hegh5dolk3n.onion:1234",
                    keyId = "135e9801eb1b50d29e6d0035e93",
                    appVersion = "0.4.2",
                    isTorRunning = true,
                ),
            topBar = {},
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NetworkMyNodeContentLoadingPreview() {
    BisqTheme.Preview {
        NetworkMyNodeContent(
            uiState = NetworkMyNodeUiState(appVersion = "0.4.2"),
            topBar = {},
        )
    }
}
