package network.bisq.mobile.presentation.ui.uicases.offerbook

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import network.bisq.mobile.presentation.ui.components.atoms.AutoResizeText
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.icons.RemoveOfferIcon
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqVDivider
import network.bisq.mobile.presentation.ui.components.molecules.PaymentMethods
import network.bisq.mobile.presentation.ui.components.molecules.UserProfile
import network.bisq.mobile.presentation.ui.theme.BisqModifier
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun OfferCard(
    item: OfferItemPresentationModel,
    userAvatar: PlatformImage? = null,
    onSelectOffer: () -> Unit,
) {
    val userName by item.userName.collectAsState()
    val sellColor = BisqTheme.colors.danger.copy(alpha = 0.8f)
    val buyColor = BisqTheme.colors.primary.copy(alpha = 0.8f)
    val myOfferColor = BisqTheme.colors.mid_grey20
    val isMyOffer = item.isMyOffer

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
    val invalidOfferBackgroundColor = BisqTheme.colors.dark_grey50.copy(alpha = 0.4f)
    val backgroundColor = when {
        isMyOffer -> myOfferBackgroundColor
        item.isInvalidDueToReputation-> invalidOfferBackgroundColor
        else -> BisqTheme.colors.dark_grey50.copy(alpha = 0.9f)
    }
    
    val height = 150.dp
    val maxUsernameChars = 24

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
            modifier = Modifier.weight(3.0F).fillMaxHeight()
        ) {
            Row(
                modifier = Modifier.height(32.dp) // Fixed height to prevent pushing content down
            ) {
                if (isMyOffer) {
                    BisqText.baseRegular(
                        text = directionalLabel,
                        color = directionalLabelColor,
                        singleLine = true,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                } else {
                    BisqText.baseRegular(
                        text = directionalLabel,
                        color = directionalLabelColor,
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )

                    BisqGap.HHalf()

//                    BisqText.baseRegularHighlight(
//                        text = userName.truncate(11),
//                        color = directionalLabelColor,
//                    )
                    AutoResizeText(
                        userName.truncate(maxUsernameChars),
                        color = directionalLabelColor,
                        textStyle = BisqTheme.typography.smallRegular,
                        maxLines = 2,
                        modifier = BisqModifier
                            .textHighlight(BisqTheme.colors.dark_grey10
                                .copy(alpha = 0.4f),  BisqTheme.colors.mid_grey10)
                            .padding(top = 4.dp, bottom = 2.dp)
                            .align(Alignment.CenterVertically),
                    )
                }
            }

            BisqGap.VHalf()

            BisqText.baseLight(item.formattedQuoteAmount)

            BisqGap.VHalf()

            AutoResizeText(
                text = "@ " + item.formattedPriceSpec,
                textStyle = BisqTheme.typography.smallLight,
                maxLines = 1,
            )


            Spacer(modifier = Modifier.weight(1f))

            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                PaymentMethods(item.baseSidePaymentMethods, item.quoteSidePaymentMethods)

                if (isMyOffer) {
                    RemoveOfferIcon()
                }
            }

            BisqGap.VHalf()

        }
    }
}