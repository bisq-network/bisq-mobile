package network.bisq.mobile.presentation.ui.components.atoms.text

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

enum class InfoBoxValueType {
    BoldValue,
    SmallValue,
    TitleSmall,
}

/*
Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
    BisqText.largeRegular(
        text = strings.bisqEasy_tradeState_header_direction,
        color = BisqTheme.colors.grey2
    )
    BisqText.h5Regular(
        text = if (offer.direction.isBuy)
            strings.bisqEasy_tradeWizard_directionAndMarket_buy
        else
            strings.bisqEasy_tradeWizard_directionAndMarket_sell
    )
}
*/

@Composable
fun InfoBox(
    label: String,
    value: String,
    rightAlign: Boolean = false,
    valueType: InfoBoxValueType = InfoBoxValueType.BoldValue,
) {

    val valueWidget: @Composable () -> Unit = {
        when (valueType) {
            InfoBoxValueType.BoldValue -> BisqText.h5Regular(text = value)
            InfoBoxValueType.SmallValue -> BisqText.baseRegular(text = value)
            InfoBoxValueType.TitleSmall -> BisqText.h2Regular(text = value)
        }
    }

    Column(
        horizontalAlignment = if(rightAlign) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BisqText.largeRegular(text = label, color = BisqTheme.colors.grey2)
        valueWidget()
    }
}
