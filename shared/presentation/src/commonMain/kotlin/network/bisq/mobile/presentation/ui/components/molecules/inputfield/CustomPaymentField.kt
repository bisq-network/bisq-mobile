package network.bisq.mobile.presentation.ui.components.molecules.inputfield

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextFieldType
import network.bisq.mobile.presentation.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

@Composable
fun CustomPaymentField(
    onAddCustomPayment: ((String) -> Unit)? = null,
) {

    var value by rememberSaveable { mutableStateOf("") }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
            .background(BisqTheme.colors.dark_grey50)
            .padding(start = BisqUIConstants.ScreenPadding)
            .padding(vertical = BisqUIConstants.Zero),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero),
        verticalAlignment = Alignment.CenterVertically
    ) {
        DynamicImage(
            path = "drawable/payment/fiat/add_custom_grey.png",
            fallbackPath = "drawable/payment/fiat/custom_payment_1.png",
            contentDescription = "mobile.components.paymentTypeCard.customPaymentMethod".i18n(value),
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X)
        )
        BisqTextField(
            value = value,
            onValueChange = { newValue, _ -> value = newValue },
            placeholder = "bisqEasy.tradeWizard.paymentMethods.customMethod.prompt".i18n(),
            modifier = Modifier.weight(1f),
            backgroundColor = BisqTheme.colors.dark_grey50,
            maxLength = 50,
            type = BisqTextFieldType.Transparent,
        )
        IconButton(
            onClick = {
                onAddCustomPayment?.invoke(value.trim())
                value = ""
            },
            enabled = value.isNotBlank(),
            colors = IconButtonDefaults.iconButtonColors(
                containerColor = BisqTheme.colors.primary,
                contentColor = BisqTheme.colors.white,
                disabledContainerColor = BisqTheme.colors.primaryDisabled,
                disabledContentColor = BisqTheme.colors.mid_grey20
            ),
        ) {
            AddIcon()
        }
    }
}
