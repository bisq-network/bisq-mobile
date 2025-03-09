package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.runtime.Composable
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab

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
                "Buy from" //TODO:i18n
            } else {
                "Sell to" //TODO:i18n
            }
        },
    )
}