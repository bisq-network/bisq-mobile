package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.FloatingActionButtonDefaults
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.animations.AnimatedBadge
import network.bisq.mobile.presentation.ui.components.atoms.icons.ArrowDownIconDark
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun JumpToBottomFloatingButton(
    visible: Boolean,
    onClicked: () -> Unit,
    jumpOffset: Int = 12,
    badgeCount: Int = 0,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(
        if (visible) Visibility.VISIBLE else Visibility.GONE,
        label = "JumpToBottom visibility animation"
    )
    val bottomOffset by transition.animateDp(label = "JumpToBottom offset animation") {
        if (it == Visibility.GONE) {
            (-jumpOffset).dp
        } else {
            jumpOffset.dp
        }
    }
    if (bottomOffset > 0.dp) {
        BadgedBox(
            modifier = modifier.graphicsLayer(clip = false).offset(x = 0.dp, y = -bottomOffset),
            badge = {
                if (badgeCount > 0) {
                    AnimatedBadge(showAnimation = true, xOffset = (-8).dp, yOffset = (-4).dp) {
                        BisqText.xsmallLight(
                            badgeCount.toString(),
                            textAlign = TextAlign.Center,
                            color = BisqTheme.colors.dark_grey20,
                        )
                    }
                }
            }) {
            SmallFloatingActionButton(
                onClick = onClicked,
                shape = FloatingActionButtonDefaults.largeShape,
                containerColor = BisqTheme.colors.light_grey10,
            ) {
                ArrowDownIconDark(Modifier.size(18.dp).offset(y=1.dp))
            }
        }
    }
}

private enum class Visibility {
    VISIBLE,
    GONE
}