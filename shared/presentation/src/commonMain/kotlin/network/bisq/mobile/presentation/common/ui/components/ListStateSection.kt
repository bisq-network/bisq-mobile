package network.bisq.mobile.presentation.common.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

/**
 * Standard empty / no-results / error message section for list screens:
 * optional icon, centered title, optional subtitle and action button.
 */
@Composable
fun ListStateSection(
    title: String,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    useHeadlineStyle: Boolean = true,
    icon: (@Composable () -> Unit)? = null,
    buttonText: String? = null,
    buttonType: BisqButtonType = BisqButtonType.Default,
    onButtonClick: () -> Unit = {},
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    verticalPadding: Dp = BisqUIConstants.ScreenPadding3X,
) {
    Column(
        modifier =
            modifier.padding(
                horizontal = BisqUIConstants.ScreenPadding2X,
                vertical = verticalPadding,
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = verticalArrangement,
    ) {
        if (icon != null) {
            icon()
            BisqGap.V2()
        }
        if (useHeadlineStyle) {
            BisqText.H5Light(text = title, textAlign = TextAlign.Center)
        } else {
            BisqText.BaseLight(text = title, textAlign = TextAlign.Center)
        }
        if (subtitle != null) {
            BisqGap.V1()
            BisqText.SmallLightGrey(text = subtitle, textAlign = TextAlign.Center)
        }
        if (buttonText != null) {
            if (subtitle != null) {
                BisqGap.V2()
            } else {
                BisqGap.V1()
            }
            BisqButton(
                text = buttonText,
                type = buttonType,
                onClick = onButtonClick,
            )
        }
    }
}

@Preview
@Composable
private fun ListStateSection_EmptyPreview() {
    BisqTheme.Preview {
        ListStateSection(
            title = "No trades yet",
            subtitle = "Your completed trades will show up here.",
            buttonText = "Browse offers",
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        )
    }
}

@Preview
@Composable
private fun ListStateSection_NoResultsPreview() {
    BisqTheme.Preview {
        ListStateSection(
            title = "No results match your search",
            useHeadlineStyle = false,
            buttonText = "Clear search",
            buttonType = BisqButtonType.Grey,
            modifier = Modifier.fillMaxSize(),
        )
    }
}
