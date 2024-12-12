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
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.dp
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import network.bisq.mobile.domain.data.model.MarketListItem
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun CurrencyProfileCard(
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
            .padding(horizontal = 4.dp, vertical = (16/LocalDensity.current.density).dp)
            // .padding(vertical = 4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick
            )
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // If the image is not available we get an exception here and we cannot use try/catch
            // Is DynamicImage needed? If so we can pass it as
            DynamicImage(
                "drawable/markets/fiat/market_${item.market.quoteCurrencyCode
                    .lowercase()
                    .replace("-", "_")}.png",
                modifier = Modifier.size(36.dp)
            )

            Spacer(modifier = Modifier.width(8.dp))
            Column {
                BisqText.baseRegular(
                    text = item.market.quoteCurrencyName,
                    color = BisqTheme.colors.light1,
                )
                Spacer(modifier = Modifier.height(0.dp))
                BisqText.baseRegular(
                    text = item.market.quoteCurrencyCode,
                    color = BisqTheme.colors.grey2,
                )
            }
        }
        BisqText.smallRegular(
            text = "$numOffers offers",
            color = BisqTheme.colors.primary,
        )
    }
}

