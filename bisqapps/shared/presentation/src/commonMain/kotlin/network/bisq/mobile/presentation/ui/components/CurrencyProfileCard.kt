package network.bisq.mobile.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import cafe.adriel.lyricist.LocalStrings
import coil3.compose.AsyncImage
import network.bisq.mobile.domain.data.model.FiatCurrency
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.ExperimentalResourceApi
import org.jetbrains.compose.resources.painterResource


@OptIn(ExperimentalResourceApi::class)
@Composable
fun CurrencyProfileCard(
    currency: FiatCurrency,
    onClick: (FiatCurrency) -> Unit) {

    val strings = LocalStrings.current
    val interactionSource = remember { MutableInteractionSource() }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 14.dp, vertical = 4.dp)
            .clickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = {
                    onClick(currency)
                }
            )
        ,
        ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DynamicImage("drawable/${currency.flagImage}", contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                BisqText.baseRegular(
                    text = currency.name,
                    color = BisqTheme.colors.light1,
                )
                Spacer(modifier = Modifier.height(0.dp))
                BisqText.baseRegular(
                    text = currency.code,
                    color = BisqTheme.colors.grey2,
                )
            }
        }
        BisqText.smallRegular(
            text = "${currency.offerCount.toString()} ${strings.common_offers}",
            color = BisqTheme.colors.primary,
        )
    }
}