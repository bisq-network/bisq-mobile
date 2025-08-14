package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.runtime.Composable
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab

@Composable
fun DirectionToggle(
    selectedDirection: DirectionEnum,
    onStateChange: (DirectionEnum) -> Unit
) {
    // Offers are mirrored to what user wants. E.g. I want to buy Bitcoin using a sell offer
    val directions = listOf(
        DirectionEnum.SELL,
        DirectionEnum.BUY,
    )

    ToggleTab(
        options = directions,
        selectedOption = selectedDirection,
        onOptionSelected = onStateChange,
        singleLine = true,
        getDisplayString = {
            if (it == DirectionEnum.BUY) {
                "bisqEasy.offerbook.offerList.table.filters.offerDirection.buyFrom".i18n()
            } else {
                "bisqEasy.offerbook.offerList.table.filters.offerDirection.sellTo".i18n()
            }
        },
    )
}