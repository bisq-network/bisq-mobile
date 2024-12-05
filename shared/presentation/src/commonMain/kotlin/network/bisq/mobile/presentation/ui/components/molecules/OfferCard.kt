package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.icon_chat_outlined
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqVDivider
import network.bisq.mobile.presentation.ui.components.atoms.PaymentMethods
import network.bisq.mobile.presentation.ui.components.atoms.ProfileRating
import network.bisq.mobile.presentation.ui.components.atoms.icons.ChatIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import org.jetbrains.compose.resources.painterResource

@Composable
fun OfferCard(
    item: OfferListItem,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
    ) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(8.dp))
            .background(color = BisqTheme.colors.dark5)
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                ProfileRating(item)
                PaymentMethods(item)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                BisqText.smallMedium(
                    // text = "\$52,000 / BTC",
                    text = item.formattedPrice,
                    color = BisqTheme.colors.primary
                )
                BisqText.largeRegular(
                    text = "\$98,000 / BTC",
                    color = BisqTheme.colors.grey1
                )
                BisqText.baseRegular(
                    text = item.formattedQuoteAmount,
                    color = BisqTheme.colors.light1
                )
            }
        }
        BisqVDivider()
        Column(
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.width(36.dp).height(108.dp).background(color = BisqTheme.colors.dark4)
        ) {
            IconButton(onClick = onChatClick) {
                ChatIcon(modifier = Modifier.size(24.dp))
            }
        }
    }
}