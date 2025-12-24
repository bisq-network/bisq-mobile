package network.bisq.mobile.presentation.tabs.open_trades

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.presentation.open_trades.TradeItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.common.ui.components.molecules.UserProfileRow
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun OpenTradeListItem(
    item: TradeItemPresentationModel,
    userProfileIconProvider: suspend (UserProfileVO) -> PlatformImage,
    unreadCount: Int,
    onSelect: () -> Unit,
) {
    val hasNotifications = unreadCount > 0

    OpenTradeCard(
        borderWidth = 6.dp,
        borderColor = BisqTheme.colors.yellow,
        hasNotifications = hasNotifications,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onSelect,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(
                horizontalAlignment = Alignment.Start,
                modifier = Modifier.weight(1f),
            ) {
                BisqText.BaseLightGrey(
                    text =
                        item.directionalTitle
                            .uppercase()
                            .replace(":", ""),
                    // 'Buying from:' or 'Selling to:'
                )
                Row(modifier = Modifier.padding(top = 6.dp, bottom = 16.dp)) {
                    UserProfileRow(
                        userProfile = item.peersUserProfile,
                        userProfileIconProvider = userProfileIconProvider,
                        reputation = item.peersReputationScore,
                        showUserName = true,
                        badgeCount = unreadCount,
                    )
                }
                BisqText.SmallLightGrey("${item.formattedDate} ${item.formattedTime}")
                BisqText.SmallLightGrey("mobile.bisqEasy.openTrades.title".i18n(item.shortTradeId))
            }
            Column(
                horizontalAlignment = Alignment.End,
                modifier = Modifier.weight(1f),
            ) {
                BisqText.LargeRegular(
                    text = item.quoteAmountWithCode,
                    color = BisqTheme.colors.primary,
                )
                BisqGap.VHalf()
                Row(modifier = Modifier.padding(top = 1.dp)) {
                    if (item.formattedPrice.length > 18) {
                        BisqText.XSmallRegularGrey("@ ")
                        BisqText.XSmallRegular(item.formattedPrice)
                    } else {
                        BisqText.SmallRegularGrey("@ ")
                        BisqText.SmallRegular(item.formattedPrice)
                    }
                }
                BisqGap.VQuarter()
                BtcSatsText(item.formattedBaseAmount)
                BisqGap.VHalf()
                PaymentMethods(
                    listOf(item.bitcoinSettlementMethod),
                    listOf(item.fiatPaymentMethod),
                )
            }
        }
    }
}
