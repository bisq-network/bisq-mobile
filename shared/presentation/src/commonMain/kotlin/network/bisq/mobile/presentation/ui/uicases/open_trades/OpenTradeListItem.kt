package network.bisq.mobile.presentation.ui.uicases.open_trades

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
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.ui.components.molecules.UserProfile
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun OpenTradeListItem(
    item: TradeItemPresentationModel,
    onSelect: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(8.dp))
            .background(color = BisqTheme.colors.dark5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onSelect
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f)
            ) {
                Row {
                    BisqText.baseLight(
                        text = item.directionalTitle.uppercase().replace(":", ""), // 'Buying from:' or 'Selling to:'
                        color = BisqTheme.colors.grey2
                    )
                }
                Row(modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)) {
                    UserProfile(item.peersUserProfile)
                }
                Row {
                    BisqText.smallLight(
                        text = "${item.formattedDate} ${item.formattedTime}",
                        color = BisqTheme.colors.grey2
                    )
                }
                Row {
                    BisqText.smallLight(
                        text = "Trade ID: ${item.shortTradeId}",
                        color = BisqTheme.colors.grey2
                    )
                }
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.weight(1f)
            ) {
                Row {
                    BisqText.largeRegular(
                        text = item.formattedQuoteAmount,
                        color = BisqTheme.colors.primary
                    )
                }
                Row(modifier = Modifier.padding(top = 1.dp)) {
                    BisqText.smallRegular(
                        text = "@ ",
                        color = BisqTheme.colors.grey2
                    )
                    BisqText.smallRegular(
                        text = item.formattedPrice,
                        color = BisqTheme.colors.light1
                    )
                }
                Row {
                    BisqText.smallRegular(
                        text = "${item.formattedBaseAmount} BTC",
                        color = BisqTheme.colors.light1
                    )
                }
                Row(modifier = Modifier.padding(top = 5.dp)) {
                    PaymentMethods(
                        listOf(item.bitcoinSettlementMethod),
                        listOf(item.fiatPaymentMethod)
                    )
                }
            }
        }
    }
}