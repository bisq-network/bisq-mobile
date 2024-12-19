package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.atoms.icons.ChatIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.LanguageIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqVDivider
import network.bisq.mobile.presentation.ui.components.molecules.*
import network.bisq.mobile.presentation.ui.theme.BisqTheme

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
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.weight(3f)
            ) {
                UserProfile(item)
                PaymentMethods(item)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.weight(2f)
            ) {
                BisqText.smallMedium(
                    text = item.formattedPrice,
                    color = BisqTheme.colors.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageIcon()
                    BisqText.largeRegular(
                        text = ": ${item.supportedLanguageCodes}",
                        color = BisqTheme.colors.grey1
                    )
                }
                BisqGap.H1()
                // Len: 13 - "300 - 600 USD"
                // Len: 17 - "3,000 - 6,000 XYZ"
                // Len: 23 - "150,640 - 1,200,312 CRC"
                if (item.formattedQuoteAmount.length < 18) {
                    BisqText.baseRegular(
                        text = item.formattedQuoteAmount,
                        color = BisqTheme.colors.light1
                    )
                } else {
                    BisqText.smallRegular(
                        text = item.formattedQuoteAmount,
                        color = BisqTheme.colors.light1
                    )
                }
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