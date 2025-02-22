package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqVDivider
import network.bisq.mobile.presentation.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.ui.components.molecules.UserProfile
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

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
    val backgroundColor = if (isMyOffer) myOfferBackgroundColor else BisqTheme.colors.dark4
    val height = 140.dp

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(8.dp))
            .background(color = backgroundColor)
            .height(height)
            .padding(BisqUIConstants.ScreenPadding)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onSelectOffer
            ),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.Start
    ) {
        UserProfile(
            item.makersUserProfile,
            item.makersReputationScore,
            item.bisqEasyOffer.supportedLanguageCodes,
            false,
            Modifier.weight(1.0F)
        )

        BisqGap.H1()
        BisqVDivider(thickness = BisqUIConstants.ScreenPaddingQuarter, color = BisqTheme.colors.primary)
        BisqGap.H1()

        Column(
            modifier = Modifier.weight(3.0F)
        ) {
            Row {
                if (isMyOffer) {
                    BisqText.baseRegular(
                        text = directionalLabel,
                        color = directionalLabelColor
                    )
                } else {
                    BisqText.baseRegular(
                        text = directionalLabel,
                        color = directionalLabelColor
                    )

                    BisqGap.HHalf()

                    BisqText.baseRegularHighlight(
                        text = userName.truncate(15),
                        color = directionalLabelColor,
                    )
                }
            }

            BisqGap.VHalf()

            BisqText.baseRegular(text = item.formattedQuoteAmount)

            BisqGap.VHalf()

            BisqText.smallRegular(text = "@ " + item.formattedPriceSpec)

            BisqGap.V1()

            PaymentMethods(item.baseSidePaymentMethods, item.quoteSidePaymentMethods)
        }

    }
}