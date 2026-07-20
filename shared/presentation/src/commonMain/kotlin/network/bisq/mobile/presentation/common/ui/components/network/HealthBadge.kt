package network.bisq.mobile.presentation.common.ui.components.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState.HEALTHY
import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState.OFFLINE
import network.bisq.mobile.presentation.common.ui.components.network.NetworkHealthState.SYNCING
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@Composable
fun HealthBadge(state: NetworkHealthState) {
    val (bgColor, labelKey) =
        when (state) {
            HEALTHY -> BisqTheme.colors.primary to "mobile.networkInfo.overview.health.healthy"
            SYNCING -> BisqTheme.colors.warning to "mobile.networkInfo.overview.health.syncing"
            OFFLINE -> BisqTheme.colors.danger to "mobile.networkInfo.overview.health.offline"
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                .background(bgColor)
                .fillMaxWidth()
                .padding(horizontal = BisqUIConstants.ScreenPadding, vertical = BisqUIConstants.ScreenPaddingQuarter),
        contentAlignment = Alignment.Center,
    ) {
        BisqText.XSmallMedium(text = labelKey.i18n(), color = BisqTheme.colors.dark_grey20)
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun HealthBadgePreview() {
    BisqTheme.Preview {
        Column(modifier = Modifier.padding(BisqUIConstants.ScreenPadding)) {
            HealthBadge(HEALTHY)
            BisqGap.V1()
            HealthBadge(SYNCING)
            BisqGap.V1()
            HealthBadge(OFFLINE)
        }
    }
}
