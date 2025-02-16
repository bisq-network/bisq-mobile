package network.bisq.mobile.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.model.offerbook.MarketListItem
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun CurrencyCard(
    item: MarketListItem,
    isSelected: Boolean = false,
    onClick: () -> Unit
) {
    val interactionSource = remember { MutableInteractionSource() }
    val numOffers = item.numOffers.collectAsState().value
    val highLightColor = BisqTheme.colors.primary

    val backgroundColor = if (isSelected) {
        BisqTheme.colors.secondary
    } else {
        BisqTheme.colors.backgroundColor
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .then(
                if (isSelected) {
                    Modifier.drawBehind {
                        drawLine(
                            color = highLightColor,
                            start = Offset(0f, size.height),
                            end = Offset(0f, 0f),
                            strokeWidth = 2.dp.toPx()
                        )
                    }
                } else {
                    Modifier.background(Color.Transparent)
                }
            )
            .padding(vertical = BisqUIConstants.ScreenPadding)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(3.0f)) {
            // If the image is not available we get an exception here and we cannot use try/catch
            // Is DynamicImage needed? If so we can pass it as
            DynamicImage(
                "drawable/markets/fiat/market_${item.market.quoteCurrencyCode
                    .lowercase()
                    .replace("-", "_")}.png",
                modifier = Modifier.size(36.dp)
            )
            BisqGap.HHalf()
            Column {
                BisqText.baseRegular(
                    text = item.market.quoteCurrencyName,
                    singleLine = true
                )
                Spacer(modifier = Modifier.height(0.dp))
                BisqText.baseRegularGrey(text = item.market.quoteCurrencyCode)
            }
        }
        BisqText.baseRegular(
            text = "$numOffers offers",
            color = BisqTheme.colors.primary,
            textAlign = TextAlign.End,
            modifier = Modifier.weight(1.0f),
        )
    }
}

