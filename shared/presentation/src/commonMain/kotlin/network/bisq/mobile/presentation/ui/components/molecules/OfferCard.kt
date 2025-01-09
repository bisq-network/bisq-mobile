package network.bisq.mobile.presentation.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
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
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.components.atoms.icons.ChatIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.LanguageIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqVDivider
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun OfferCard(
    offerListItem: OfferListItemVO,
    myTrade: Boolean = false,
    onClick: () -> Unit,
    onChatClick: () -> Unit,
) {
    val strings = LocalStrings.current.common

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(8.dp))
            .background(color = BisqTheme.colors.dark5)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(BisqUIConstants.ScreenPadding)
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
                modifier = Modifier.weight(3f),
            ) {
                UserProfile(offerListItem)
                PaymentMethods(offerListItem)
            }
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.weight(3f)
            ) {
                BisqText.baseRegular(
                    text = "${offerListItem.formattedPrice} ${offerListItem.bisqEasyOffer.market.quoteCurrencyCode}",
                    color = BisqTheme.colors.primary
                )
                BisqGap.H1()
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
                    BisqGap.H1()
                    BisqText.smallRegular(
                        text = offerListItem.formattedPriceSpec,
                        color = BisqTheme.colors.light1
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    BisqGap.H1()
                    // Len: 13 - "300 - 600 USD"
                    // Len: 17 - "3,000 - 6,000 XYZ"
                    // Len: 23 - "150,640 - 1,200,312 CRC"
                    if (offerListItem.formattedQuoteAmount.length < 18) {
                        BisqText.baseRegular(
                            text = offerListItem.formattedQuoteAmount,
                            color = BisqTheme.colors.light1
                        )
                    } else {
                        BisqText.smallRegular(
                            text = offerListItem.formattedQuoteAmount,
                            color = BisqTheme.colors.light1
                        )
                    }
                }
            }
            /*
            // TODO: This code is kept here, so ChatButton can be added back in future, when required
            if (!myTrade) {
                BisqVDivider()
                Column(
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.width(36.dp).height(108.dp).background(color = BisqTheme.colors.dark4)
                ) {
                    IconButton(onClick = onChatClick) {
                        ChatIcon()
                    }
                }
            }
            */
        }

        if (myTrade) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(
                        start = BisqUIConstants.ScreenPadding,
                        end = BisqUIConstants.ScreenPadding,
                        top = 0.dp,
                        bottom = BisqUIConstants.ScreenPadding,
                    )
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onClick
                    ),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                BisqText.baseRegular("3 days ago", color = BisqTheme.colors.light1)
                BisqButton(
                    text = strings.common_buy,
                    disabled = true,
                    padding = PaddingValues(
                        horizontal = BisqUIConstants.ScreenPadding2X,
                        vertical = BisqUIConstants.ScreenPaddingHalf
                    ),
                    backgroundColor = BisqTheme.colors.warning
                )
            }
        }
    }
}