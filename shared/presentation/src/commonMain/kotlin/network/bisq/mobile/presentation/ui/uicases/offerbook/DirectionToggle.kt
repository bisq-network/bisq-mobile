package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.helpers.StringHelper
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun DirectionToggle(
    directions: List<DirectionEnum>,
    initialDirection: DirectionEnum,
    onStateChange: (DirectionEnum) -> Unit
) {
    ToggleTab(
        options = directions,
        initialOption = initialDirection,
        onStateChange = onStateChange,
        getDisplayString = {
            if (it == DirectionEnum.BUY) {
                "Buy from"
            } else {
                "Sell to"
            }
        },
    )
}