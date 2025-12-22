package network.bisq.mobile.presentation.common.ui.components.molecules

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
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
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.button.CloseIconButton
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme

@Composable
fun PaymentTypeCard(
    image: String,
    title: String,
    onClick: (String) -> Unit,
    onRemove: ((String) -> Unit)? = null,
    isSelected: Boolean = false,
    isCustomPaymentMethod: Boolean = false,
    showRemoveCustom: Boolean = false,
) {
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
        // Use PaymentMethodIcon for both known and custom payment methods
        // For custom methods, pass the title as methodId to get the overlay letter
        // For known methods, pass the image path to use the correct icon
        PaymentMethodIcon(
            methodId = if (isCustomPaymentMethod) title else image.substringAfterLast("/").substringBefore("."),
            isPaymentMethod = true,
            size = 20.dp,
            contentDescription = if (isCustomPaymentMethod) "mobile.components.paymentTypeCard.customPaymentMethod".i18n(title) else title,
            iconPathOverride = image,
        )
        BisqText.baseRegular(title, modifier = Modifier.weight(1.0f))
        if (isCustomPaymentMethod && showRemoveCustom) {
            CloseIconButton(
                onClick = {
                    onRemove?.invoke(title)
                }
            )
        }
    }
}