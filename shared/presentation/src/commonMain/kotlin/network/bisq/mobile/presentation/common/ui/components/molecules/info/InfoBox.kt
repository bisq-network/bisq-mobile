package network.bisq.mobile.presentation.common.ui.components.molecules.info

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

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
    subvalue: String? = null,
    valueComposable: (@Composable () -> Unit)? = null,
    rightAlign: Boolean = false,
    valueType: InfoBoxValueType = InfoBoxValueType.BoldValue,
    style: InfoBoxStyle = InfoBoxStyle.Style1,
) {

    val valueWidget: @Composable () -> Unit = if (value != null) {
        // todo just a quick fix for min-max values to allow to display them without breaking layout
        // val adjustedValueType = if (value.length > 14) InfoBoxValueType.SmallValue else valueType
        // buddha: Even with `SmallValue` it breaks for currencies like 'Vietnamese Dong'. So I made ...
        //    ... them (Amount to Pay, Amount to Receive) take 2 rows in CreateOfferReviewScreen
        {
            when (valueType) {
                InfoBoxValueType.BoldValue -> if (style == InfoBoxStyle.Style1)
                    BisqText.h6Light(value)
                else
                    BisqText.baseLight(value)
                InfoBoxValueType.SmallValue -> BisqText.baseLight(value)
                InfoBoxValueType.TitleSmall -> BisqText.h4Light(value)
            }
        }
    } else if (valueComposable != null) {
        {
            valueComposable()
        }
    } else {
        {
            BisqText.h6Light(text = "mobile.components.infoBox.error".i18n(), color = BisqTheme.colors.danger)
        }
    }

    when (style) {
        InfoBoxStyle.Style1 -> {
            Column(
                horizontalAlignment = if (rightAlign) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                BisqText.baseLightGrey(label)
                valueWidget()
                if (subvalue != null) {
                    BisqText.smallLight(text = subvalue, color = BisqTheme.colors.mid_grey30)
                }
            }
        }

        InfoBoxStyle.Style2 -> {
            Column(
                horizontalAlignment = if (rightAlign) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(0.dp)
            ) {
                valueWidget()
                if (subvalue != null) {
                    BisqText.xsmallLight(text = subvalue, color = BisqTheme.colors.mid_grey30)
                }
                BisqText.smallLightGrey(text = label, modifier = Modifier.offset(y = (-4).dp))
            }
        }
    }

}