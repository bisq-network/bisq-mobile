package network.bisq.mobile.presentation.common.ui.components.molecules.dialog

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush.Companion.verticalGradient
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

private val SCROLL_FADE_HEIGHT = 44.dp

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
        // Bound card height to the available viewport so the action button stays sticky
        // while long reconnect copy (especially iOS Connect) can scroll on small screens.
        BoxWithConstraints(
            modifier =
                Modifier
                    .align(Alignment.Center)
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding4X,
                        vertical = BisqUIConstants.ScreenPadding2X,
                    ),
        ) {
            Surface(
                shape = RoundedCornerShape(BisqUIConstants.ScreenPadding),
                color = BisqTheme.colors.dark_grey40,
                modifier = Modifier.heightIn(max = maxHeight),
            ) {
                Column(
                    modifier =
                        Modifier.padding(
                            horizontal = BisqUIConstants.ScreenPadding2X,
                            vertical = BisqUIConstants.ScreenPadding4X,
                        ),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    val scrollState = rememberScrollState()
                    val canScrollForward by remember {
                        derivedStateOf { scrollState.canScrollForward }
                    }

                    Box(
                        modifier =
                            Modifier
                                .weight(1f, fill = false)
                                .fillMaxWidth(),
                    ) {
                        Column(
                            modifier = Modifier.verticalScroll(scrollState),
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
                        }

                        // Soft bottom fade when more copy is clipped above the sticky button.
                        if (canScrollForward) {
                            Box(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(SCROLL_FADE_HEIGHT)
                                        .align(Alignment.BottomCenter)
                                        .background(
                                            brush =
                                                verticalGradient(
                                                    colors =
                                                        listOf(
                                                            Color.Transparent,
                                                            BisqTheme.colors.dark_grey40,
                                                        ),
                                                ),
                                        ),
                            )
                        }
                    }
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
}

@Preview(name = "iPhone SE — iOS Connect", widthDp = 320, heightDp = 568)
@Composable
private fun ReconnectingOverlay_IPhoneSePreview() {
    BisqTheme.Preview {
        ReconnectingOverlay(
            onClick = {},
            infoKey = "mobile.connectivity.reconnecting.client.info",
            detailsKey = "mobile.connectivity.reconnecting.client.details.ios",
            buttonTextKey = "mobile.connectivity.reconnecting.restartServices",
        )
    }
}

@Preview(name = "iPhone X — iOS Connect", widthDp = 375, heightDp = 812)
@Composable
private fun ReconnectingOverlay_IPhoneXPreview() {
    BisqTheme.Preview {
        ReconnectingOverlay(
            onClick = {},
            infoKey = "mobile.connectivity.reconnecting.client.info",
            detailsKey = "mobile.connectivity.reconnecting.client.details.ios",
            buttonTextKey = "mobile.connectivity.reconnecting.restartServices",
        )
    }
}

@Preview(name = "Pixel 7 — Node default", widthDp = 412, heightDp = 915)
@Composable
private fun ReconnectingOverlay_Pixel7Preview() {
    BisqTheme.Preview {
        ReconnectingOverlay(onClick = {})
    }
}
