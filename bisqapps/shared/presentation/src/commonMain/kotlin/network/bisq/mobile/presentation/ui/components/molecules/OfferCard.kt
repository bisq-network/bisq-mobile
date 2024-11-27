package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_chat_outlined
import network.bisq.mobile.domain.data.model.offerbook.OfferListItem
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.PaymentMethods
import network.bisq.mobile.presentation.ui.components.atoms.ProfileRating
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun OfferCard(item: OfferListItem, onClick: () -> Unit) {
    Row(
        modifier = Modifier.clip(shape = RoundedCornerShape(8.dp)).padding(vertical = 5.dp),

        ) {
        Row(
            modifier = Modifier.background(color = BisqTheme.colors.dark5).padding(12.dp).clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileRating(item)
                PaymentMethods(item)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                BisqText.baseRegular(
                    // text = "\$50 - \$200",
                    text = item.formattedQuoteAmount,
                    color = BisqTheme.colors.light1
                )
                BisqText.smallMedium(
                    // text = "\$52,000 / BTC",
                    text = item.formattedPrice,
                    color = BisqTheme.colors.grey1
                )

            }
        }
        VerticalDivider(
            thickness = 2.dp,
            color = BisqTheme.colors.grey3,
            modifier = Modifier.height(98.dp)
        )
        Column(
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.background(color = BisqTheme.colors.dark4)
                .padding(horizontal = 10.dp, vertical = 41.dp)
        ) {
            Image(
                painterResource(Res.drawable.icon_chat_outlined),
                "",
                modifier = Modifier.size(16.dp),
            )
        }
    }
}