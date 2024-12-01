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

@Composable
fun InfoBox(
    label: String,
    value: String? = null,
    valueComposable: (@Composable () -> Unit)? = null,
    rightAlign: Boolean = false,
    valueType: InfoBoxValueType = InfoBoxValueType.BoldValue,
) {

    val valueWidget: @Composable () -> Unit = if (value != null) {
        {
            when (valueType) {
                InfoBoxValueType.BoldValue -> BisqText.h6Regular(text = value)
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

    Column(
        horizontalAlignment = if (rightAlign) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        BisqText.baseRegular(text = label, color = BisqTheme.colors.grey2)
        valueWidget()
    }
}
