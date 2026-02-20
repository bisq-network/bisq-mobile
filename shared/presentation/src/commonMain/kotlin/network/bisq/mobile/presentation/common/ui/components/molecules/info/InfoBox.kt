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
import network.bisq.mobile.presentation.common.ui.components.molecules.AmountWithCurrency
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import org.jetbrains.compose.ui.tooling.preview.Preview

enum class InfoBoxValueType {
    BoldValue,
    SmallValue,
    TitleSmall,
}

enum class InfoBoxStyle {
    Style1, // Label on top, value below
    Style2, // Value on top, label below
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
    val valueWidget: @Composable () -> Unit =
        if (value != null) {
            // todo just a quick fix for min-max values to allow to display them without breaking layout
            // val adjustedValueType = if (value.length > 14) InfoBoxValueType.SmallValue else valueType
            // buddha: Even with `SmallValue` it breaks for currencies like 'Vietnamese Dong'. So I made ...
            //    ... them (Amount to Pay, Amount to Receive) take 2 rows in CreateOfferReviewScreen
            {
                when (valueType) {
                    InfoBoxValueType.BoldValue ->
                        if (style == InfoBoxStyle.Style1) {
                            BisqText.H6Light(value)
                        } else {
                            BisqText.BaseLight(value)
                        }

                    InfoBoxValueType.SmallValue -> BisqText.BaseLight(value)
                    InfoBoxValueType.TitleSmall -> BisqText.H4Light(value)
                }
            }
        } else if (valueComposable != null) {
            {
                valueComposable()
            }
        } else {
            {
                BisqText.H6Light(text = "mobile.components.infoBox.error".i18n(), color = BisqTheme.colors.danger)
            }
        }

    when (style) {
        InfoBoxStyle.Style1 -> {
            Column(
                horizontalAlignment = if (rightAlign) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                BisqText.BaseLightGrey(label)
                valueWidget()
                if (subvalue != null) {
                    BisqText.SmallLight(text = subvalue, color = BisqTheme.colors.mid_grey30)
                }
            }
        }

        InfoBoxStyle.Style2 -> {
            Column(
                horizontalAlignment = if (rightAlign) Alignment.End else Alignment.Start,
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                valueWidget()
                if (subvalue != null) {
                    BisqText.XSmallLight(text = subvalue, color = BisqTheme.colors.mid_grey30)
                }
                BisqText.SmallLightGrey(text = label, modifier = Modifier.offset(y = (-4).dp))
            }
        }
    }
}

@Preview
@Composable
private fun InfoBox_DefaultPreview() {
    BisqTheme.Preview {
        InfoBox(
            label = "AMOUNT TO PAY",
            value = "1,000.00 USD",
        )
    }
}

@Preview
@Composable
private fun InfoBox_Style2Preview() {
    BisqTheme.Preview {
        InfoBox(
            label = "Market Price",
            value = "99,000.00",
            style = InfoBoxStyle.Style2,
        )
    }
}

@Preview
@Composable
private fun InfoBox_TitleSmallPreview() {
    BisqTheme.Preview {
        InfoBox(
            label = "Total Balance",
            value = "2.5 BTC",
            valueType = InfoBoxValueType.TitleSmall,
        )
    }
}

@Preview
@Composable
private fun InfoBox_SmallValuePreview() {
    BisqTheme.Preview {
        InfoBox(
            label = "TRANSACTION ID",
            value = "abc123...xyz789",
            valueType = InfoBoxValueType.SmallValue,
        )
    }
}

@Preview
@Composable
private fun InfoBox_WithSubvaluePreview() {
    BisqTheme.Preview {
        InfoBox(
            label = "AMOUNT TO RECEIVE",
            value = "0.01000000 BTC",
            subvalue = "Includes 0.1% fee",
        )
    }
}

@Preview
@Composable
private fun InfoBox_RightAlignPreview() {
    BisqTheme.Preview {
        InfoBox(
            label = "PRICE",
            value = "100,000.00 USD",
            rightAlign = true,
        )
    }
}

@Preview
@Composable
private fun InfoBox_ValueComposablePreview() {
    BisqTheme.Preview {
        InfoBox(
            label = "AMOUNT TO RECEIVE",
            valueComposable = {
                AmountWithCurrency("60.00 USD")
            },
        )
    }
}
