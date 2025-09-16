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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.button.CloseIconButton
import network.bisq.mobile.presentation.ui.helpers.customPaymentIconIndex
import network.bisq.mobile.presentation.ui.theme.BisqTheme

private val CUSTOM_PAYMENT_ICON_IDS = listOf(
    "custom_payment_1",
    "custom_payment_2",
    "custom_payment_3",
    "custom_payment_4",
    "custom_payment_5",
    "custom_payment_6",
)

@Composable
fun PaymentTypeCard(
    image: String,
    title: String,
    onClick: (String) -> Unit,
    onRemove: ((String) -> Unit)? = null,
    isSelected: Boolean = false,
    isCustomPaymentMethod: Boolean = false,
) {

    val customIndex = if(isCustomPaymentMethod)
        customPaymentIconIndex(title, CUSTOM_PAYMENT_ICON_IDS.size)
    else
        0
    val backgroundColor = if (isSelected) {
        BisqTheme.colors.primaryDim
    } else {
        BisqTheme.colors.dark_grey50
    }

    Row(
        modifier = Modifier.fillMaxWidth()
            .clip(shape = RoundedCornerShape(6.dp))
            .background(backgroundColor)
            .padding(horizontal = 18.dp)
            .padding(vertical = 10.dp)
            .clickable(
                onClick = { onClick(title) },
                interactionSource = remember { MutableInteractionSource() },
                indication = null
            ),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box {
            DynamicImage(
                path = image,
                fallbackPath = "drawable/payment/fiat/${CUSTOM_PAYMENT_ICON_IDS[customIndex]}.png",
                contentDescription =  if (isCustomPaymentMethod) "mobile.components.paymentTypeCard.customPaymentMethod".i18n(title) else title,
                modifier = Modifier.size(20.dp)
            )
            if (isCustomPaymentMethod && title.isNotEmpty()) {
                Box(modifier = Modifier.size(20.dp), contentAlignment = Alignment.Center) {
                    val firstChar = title.firstOrNull()?.toString().orEmpty()
                    BisqText.baseRegular(
                        text = firstChar,
                        textAlign = TextAlign.Center,
                        color = BisqTheme.colors.dark_grey20,
                        modifier = Modifier.size(20.dp).wrapContentSize()
                    )
                }
            }
        }
        BisqText.baseRegular(title, modifier = Modifier.weight(1.0f))
        if (isCustomPaymentMethod) {
            CloseIconButton(
                onClick = {
                    onRemove?.invoke(title)
                }
            )
        }
    }
}