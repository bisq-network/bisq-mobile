package network.bisq.mobile.presentation.ui.components.molecules.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme

enum class InfoBoxValueType {
    BoldValue,
    SmallValue,
    TitleSmall,
}

enum class InfoBoxStyle {
    Style1, // Label on top, value below
    Style2  // Value on top, label below
}

@Composable
fun InfoBox(
    label: String,
    value: String? = null,
    valueComposable: (@Composable () -> Unit)? = null,
    rightAlign: Boolean = false,
    valueType: InfoBoxValueType = InfoBoxValueType.BoldValue,
    style: InfoBoxStyle = InfoBoxStyle.Style1,
) {

    val valueWidget: @Composable () -> Unit = if (value != null) {
        // todo just a quick fix for min-max values to allow to display them without breaking layout
        val adjustedValueType = if (value.length > 14) InfoBoxValueType.SmallValue else valueType
        {
            when (adjustedValueType) {
                InfoBoxValueType.BoldValue -> if (style == InfoBoxStyle.Style1) BisqText.h6Regular(text = value)
                else (BisqText.baseRegular(text = value))
                InfoBoxValueType.SmallValue -> BisqText.baseRegular(text = value)
                InfoBoxValueType.TitleSmall -> BisqText.h4Regular(text = value)
            }
        }
    } else if (valueComposable != null) {
        {
            valueComposable()
        }
    } else {
        {
            BisqText.h6Regular(text = "[ERR] Pass either value or valueComposable", color = BisqTheme.colors.danger)
        }
    }

    when (style) {
        InfoBoxStyle.Style1 -> {
            Column(
                horizontalAlignment = if (rightAlign) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BisqText.baseRegular(text = label, color = BisqTheme.colors.grey2)
                valueWidget()
            }
        }

        InfoBoxStyle.Style2 -> {
            Column(
                horizontalAlignment = if (rightAlign) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                valueWidget()
                BisqText.smallRegular(text = label, color = BisqTheme.colors.grey2, modifier = Modifier.offset(y = (-4).dp))
            }
        }
    }

    /*
    Column(
        horizontalAlignment = if (rightAlign) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        BisqText.baseRegular(text = label, color = BisqTheme.colors.grey2)
        valueWidget()
    }
    */
}
