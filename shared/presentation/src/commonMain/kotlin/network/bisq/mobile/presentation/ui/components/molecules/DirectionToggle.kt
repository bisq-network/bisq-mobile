package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.selects.select
import network.bisq.mobile.client.replicated_model.offer.Direction
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun DirectionToggle(
    directions: List<Direction>,
    initialDirection: Direction,
    transitionX: Dp,
    onStateChange: (Direction) -> Unit
) {
    val strings = LocalStrings.current.common
    var selectedDirection by remember {
        mutableStateOf(initialDirection)
    }

    val slideOffset by animateDpAsState(
        targetValue = if (selectedDirection == directions[0]) 0.dp else transitionX,
        animationSpec = tween(durationMillis = 300)
    )

    val toggleText = if (selectedDirection.isBuy) strings.offers_list_buy_from else strings.offers_list_sell_to

    Surface(
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier.wrapContentSize()
    ) {
        Box(
            modifier = Modifier
                .background(BisqTheme.colors.dark5)
                .padding(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .offset(x = slideOffset)
                    .background(BisqTheme.colors.primary, RoundedCornerShape(4.dp))
            ) {

                BisqText.baseMedium(
                    text = toggleText,
                    color = BisqTheme.colors.light1,
                    modifier = Modifier
                        .padding(horizontal = 32.dp, vertical = 12.dp)
                        .alpha(0f),
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
            ) {
                directions.forEach{ direction ->
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 32.dp, vertical = 12.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    selectedDirection = direction
                                    onStateChange.invoke(direction)
                                }
                            )
                    ) {
                        BisqText.baseMedium(
                            text = toggleText,
                            color = BisqTheme.colors.light1,
                        )
                    }
                }
            }
        }
    }
}