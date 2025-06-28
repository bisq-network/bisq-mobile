package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun JumpToBottomFloatingButton(
    enabled: Boolean,
    onClicked: () -> Unit,
    jumpOffset: Int = 96,
    modifier: Modifier = Modifier
) {
    val transition = updateTransition(
        if (enabled) Visibility.VISIBLE else Visibility.GONE,
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
        ExtendedFloatingActionButton(
            icon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    modifier = Modifier.height(18.dp),
                    contentDescription = null
                )
            },
            text = { BisqText.baseRegular("mobile.tradeChat.jumpToBottom".i18n(), color = BisqTheme.colors.dark_grey10) },
            onClick = onClicked,
            containerColor = BisqTheme.colors.light_grey10,
            contentColor = BisqTheme.colors.dark_grey10,
            modifier = modifier
                .offset(x = 0.dp, y = -bottomOffset)
                .height(BisqUIConstants.ScreenPadding3X)
        )
    }
}

private enum class Visibility {
    VISIBLE,
    GONE
}