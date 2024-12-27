package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.replicated.offer.bisq_easy.OfferListItemVO
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.LanguageIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun OfferCard(
    offerListItem: OfferListItemVO,
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
                modifier = Modifier.weight(2f)
            ) {
                UserProfile(offerListItem)
                PaymentMethods(offerListItem)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.weight(2f)
            ) {
                // Len: 13 - "300 - 600 USD"
                // Len: 17 - "3,000 - 6,000 XYZ"
                // Len: 23 - "150,640 - 1,200,312 CRC"
                //if (offerListItem.formattedQuoteAmount.length < 18) {
                BisqText.baseRegular(
                    text = offerListItem.formattedQuoteAmount,
                    color = BisqTheme.colors.primary
                )
                /* } else {
                     BisqText.smallRegular(
                         text = offerListItem.formattedQuoteAmount,
                         color = BisqTheme.colors.primary
                     )
                 }*/
                BisqGap.H1()
                Row {
                    BisqText.smallRegular(
                        text = "@ ",
                        color = BisqTheme.colors.grey2
                    )
                    BisqText.smallRegular(
                        text = offerListItem.formattedPriceSpec,
                        color = BisqTheme.colors.light1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    LanguageIcon()
                    BisqText.smallRegular(
                        text = " : ",
                        color = BisqTheme.colors.grey2
                    )
                    BisqText.smallRegular(
                        text = offerListItem.bisqEasyOffer.supportedLanguageCodes.joinToString(", ").uppercase(),
                        color = BisqTheme.colors.light1
                    )
                }
            }
        }
        /*  BisqVDivider()
          Column(
              verticalArrangement = Arrangement.Center,
              horizontalAlignment = Alignment.CenterHorizontally,
              modifier = Modifier.width(36.dp).height(108.dp).background(color = BisqTheme.colors.dark4)
          ) {
              IconButton(onClick = onChatClick) {
                  ChatIcon(modifier = Modifier.size(24.dp))
              }
          }*/
    }
}