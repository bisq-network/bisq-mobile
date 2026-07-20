package network.bisq.mobile.presentation.common.ui.components.network

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

@Composable
fun NetworkSectionLabel(
    text: String,
    modifier: Modifier = Modifier,
) {
    BisqText.XSmallMedium(
        text = text.uppercase(),
        color = BisqTheme.colors.mid_grey20,
        modifier =
            modifier
                .fillMaxWidth()
                .padding(
                    top = BisqUIConstants.ScreenPadding,
                    bottom = BisqUIConstants.ScreenPaddingHalf,
                ),
    )
}

@ExcludeFromCoverage
@Preview
@Composable
private fun NetworkSectionLabelPreview() {
    BisqTheme.Preview {
        NetworkSectionLabel(text = "Trusted Node")
    }
}
