package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

@Composable
fun ReconnectingOverlay(
    onClick: (() -> Unit)? = null,
    infoKey: String = "mobile.connectivity.reconnecting.info",
    detailsKey: String = "mobile.connectivity.reconnecting.details",
    buttonTextKey: String = "mobile.connectivity.reconnecting.restart",
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(BisqTheme.colors.backgroundColor.copy(alpha = 0.85f))
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                ) { /* consume clicks */ },
    ) {
        Surface(
            shape = RoundedCornerShape(BisqUIConstants.ScreenPadding),
            color = BisqTheme.colors.dark_grey40,
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding4X,
                        vertical = BisqUIConstants.ScreenPadding2X,
                    ),
        ) {
            Column(
                modifier =
                    Modifier.padding(
                        horizontal = BisqUIConstants.ScreenPadding2X,
                        vertical = BisqUIConstants.ScreenPadding4X,
                    ),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                BisqText.H3Light(
                    text = "mobile.connectivity.reconnecting.title".i18n(),
                    color = BisqTheme.colors.white,
                    textAlign = TextAlign.Center,
                )

                BisqGap.VQuarter()
                CircularProgressIndicator(
                    color = BisqTheme.colors.primary,
                    modifier = Modifier.size(70.dp),
                    strokeWidth = 1.dp,
                )
                BisqGap.VQuarter()

                BisqText.LargeLight(
                    text = infoKey.i18n(),
                    color = BisqTheme.colors.light_grey50,
                    textAlign = TextAlign.Center,
                )

                BisqText.BaseLight(
                    text = detailsKey.i18n(),
                    color = BisqTheme.colors.light_grey50,
                    textAlign = TextAlign.Center,
                )
                BisqGap.VHalf()
                BisqButton(
                    text = buttonTextKey.i18n(),
                    type = BisqButtonType.Outline,
                    onClick = onClick,
                )
            }
        }
    }
}
