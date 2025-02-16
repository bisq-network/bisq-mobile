package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.displayString
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.utils.StringUtils.truncate
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.LanguageIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.RemoveOfferIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqVDivider
import network.bisq.mobile.presentation.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.ui.components.molecules.UserProfile
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun OfferCard(
    item: OfferItemPresentationModel,
    onSelectOffer: () -> Unit,
) {
    val userName = item.userName.collectAsState().value
    val sellColor = BisqTheme.colors.danger.copy(alpha = 0.8f) //todo add sell color
    val buyColor = BisqTheme.colors.primary.copy(alpha = 0.8f)
    val myOfferColor = BisqTheme.colors.grey2
    val isMyOffer = item.isMyOffer
    val directionalLabel: String
    val directionalLabelColor: Color
    val makersDirection = item.bisqEasyOffer.direction
    val takersDirection = makersDirection.mirror
    if (isMyOffer) {
        directionalLabel = "My offer to ${makersDirection.displayString} Bitcoin"
        directionalLabelColor = myOfferColor
    } else {
        if (takersDirection.isBuy) {
            directionalLabel = "${takersDirection.displayString} Bitcoin from"
            directionalLabelColor = buyColor
        } else {
            directionalLabel = "${takersDirection.displayString} Bitcoin to"
            directionalLabelColor = sellColor
        }
    }

    val myOfferBackgroundColor = BisqTheme.colors.primary.copy(alpha = 0.15f)
    val removeMyOfferBackgroundColor = BisqTheme.colors.dark1.copy(alpha = 0.6f)
    val backgroundColor = if (isMyOffer) myOfferBackgroundColor else BisqTheme.colors.dark5
    val height = 120.dp

    //todo mirror layout of my offers as in Bisq 2
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(8.dp))
            .background(color = backgroundColor)
            .height(height)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelectOffer
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
            //verticalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier.padding(12.dp).weight(2f)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (isMyOffer) {
                    BisqText.baseRegular(
                        text = directionalLabel,
                        color = directionalLabelColor
                    )
                } else {
                    BisqText.baseMedium(
                        text = directionalLabel,
                        color = directionalLabelColor
                    )

                    BisqText.baseRegular(
                        text = userName.truncate(15),
                        color = directionalLabelColor,
                        modifier = Modifier
                            .background(
                                color = BisqTheme.colors.dark1.copy(alpha = 0.4f),
                                shape = RoundedCornerShape(CornerSize(4.dp))
                            )
                            .border(
                                border = BorderStroke(
                                    width = 0.25.dp,
                                    color = BisqTheme.colors.grey1
                                ),
                                shape = RoundedCornerShape(CornerSize(4.dp))
                            )
                            .padding(top = 1.dp, bottom = 2.dp, start = 10.dp, end = 10.dp)
                    )
                }
            }

            UserProfile(item.makersUserProfile, false)

            Row(verticalAlignment = Alignment.CenterVertically) {
                LanguageIcon()
                BisqText.smallRegularGrey(text = " : ")
                BisqText.smallRegular(text = item.bisqEasyOffer.supportedLanguageCodes.joinToString(", ").uppercase())
            }
        }

        Column(
            horizontalAlignment = Alignment.End,
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.padding(12.dp).weight(1f)
        ) {
            // Len: 13 - "300 - 600 USD"
            // Len: 17 - "3,000 - 6,000 XYZ"
            // Len: 23 - "150,640 - 1,200,312 CRC"
            //if (offerListItem.formattedQuoteAmount.length < 18) {
            BisqGap.HHalf()
            BisqText.baseRegular(
                text = item.formattedQuoteAmount,
                color = BisqTheme.colors.primary
            )
            /* } else {
                 BisqText.smallRegular(
                     text = offerListItem.formattedQuoteAmount,
                     color = BisqTheme.colors.primary
                 )
             }*/

            Row {
                BisqText.smallRegularGrey(text = "@ ")
                BisqText.smallRegular(text = item.formattedPriceSpec)
            }

            PaymentMethods(item.baseSidePaymentMethods, item.quoteSidePaymentMethods)

            BisqGap.HHalf()
        }

        if (isMyOffer) {
            Row(
                modifier = Modifier
                    .clip(shape = RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                    .background(color = removeMyOfferBackgroundColor)
                    .height(height),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Start
            ) {
                BisqVDivider(
                    thickness = 1.dp,
                    modifier = Modifier.height(height)
                )
                Column(
                    modifier = Modifier.width(45.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    RemoveOfferIcon(modifier = Modifier.size(18.dp).width(45.dp))
                }
            }
        }
    }
}