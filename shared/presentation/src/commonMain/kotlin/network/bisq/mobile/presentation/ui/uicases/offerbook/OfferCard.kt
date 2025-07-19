package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.PlatformImage
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.displayString
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.mirror
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.utils.StringUtils.truncate
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.RemoveOfferIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqVDivider
import network.bisq.mobile.presentation.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.ui.components.molecules.UserProfile
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun OfferCard(
    item: OfferItemPresentationModel,
    userAvatar: PlatformImage? = null,
    onSelectOffer: () -> Unit,
) {
    val userName = item.userName.collectAsState().value
    val sellColor = BisqTheme.colors.danger.copy(alpha = 0.8f)
    val buyColor = BisqTheme.colors.primary.copy(alpha = 0.8f)
    val myOfferColor = BisqTheme.colors.mid_grey20
    val isMyOffer = item.isMyOffer
    val isInvalidDueToReputation = item.isInvalidDueToReputation
    
    val directionalLabel: String
    val directionalLabelColor: Color
    val makersDirection = item.bisqEasyOffer.direction
    val takersDirection = makersDirection.mirror
    
    if (isMyOffer) {
        directionalLabel = "mobile.bisqEasy.offerbook.offerCard.offerToBTC".i18n(makersDirection.displayString)
        directionalLabelColor = myOfferColor
    } else {
        if (takersDirection.isBuy) {
            directionalLabel = "mobile.bisqEasy.offerbook.offerCard.BuyBitcoinFrom".i18n(takersDirection.displayString)
            directionalLabelColor = buyColor
        } else {
            directionalLabel = "mobile.bisqEasy.offerbook.offerCard.SellBitcoinTo".i18n(takersDirection.displayString)
            directionalLabelColor = sellColor
        }
    }

    val myOfferBackgroundColor = BisqTheme.colors.primary.copy(alpha = 0.15f)
    val invalidOfferBackgroundColor = BisqTheme.colors.dark_grey40.copy(alpha = 0.5f)
    val backgroundColor = when {
        isMyOffer -> myOfferBackgroundColor
        isInvalidDueToReputation -> invalidOfferBackgroundColor
        else -> BisqTheme.colors.dark_grey40
    }
    
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
            user = item.makersUserProfile,
            userAvatar = userAvatar,
            reputation = item.makersReputationScore,
            supportedLanguageCodes = item.bisqEasyOffer.supportedLanguageCodes,
            showUserName = false,
            modifier = Modifier.weight(1.0F)
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

            BisqText.baseRegular(item.formattedQuoteAmount)

            BisqGap.VHalf()

            BisqText.smallRegular("@ " + item.formattedPriceSpec)

            BisqGap.V1()

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                PaymentMethods(item.baseSidePaymentMethods, item.quoteSidePaymentMethods)

                if (isMyOffer) {
                    RemoveOfferIcon()
                }
            }
            
            if (isInvalidDueToReputation) {
                BisqGap.V1()
                BisqText.smallRegular(
                    text = "mobile.bisqEasy.offerbook.insufficientReputation".i18n(),
                    color = BisqTheme.colors.warning
                )
            }
        }
    }
}