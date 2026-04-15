package network.bisq.mobile.presentation.common.ui.components.molecules.inputfield

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
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldColors
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.AddIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants

private const val MAX_CUSTOM_PAYMENT_LENGTH = 50

@Composable
fun CustomPaymentField(
    onAddCustomPayment: ((String) -> Unit)? = null,
) {
    var value by rememberSaveable { mutableStateOf("") }
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(shape = RoundedCornerShape(BisqUIConstants.ScreenPaddingHalf))
                .background(BisqTheme.colors.dark_grey50)
                .padding(start = BisqUIConstants.ScreenPadding)
                .padding(vertical = BisqUIConstants.Zero),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.Zero),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        DynamicImage(
            path = "files/payment/fiat/add_custom_grey.png",
            fallbackPath = "files/payment/fiat/custom_payment_1.png",
            contentDescription = "mobile.components.paymentTypeCard.customPaymentMethod".i18n(value),
            modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
        )
        BisqTextFieldV0(
            value = value,
            onValueChange = { newValue -> value = newValue.take(MAX_CUSTOM_PAYMENT_LENGTH) },
            placeholder = "bisqEasy.tradeWizard.paymentMethods.customMethod.prompt".i18n(),
            modifier = Modifier.weight(1f),
            colors =
                BisqTextFieldColors.default(
                    backgroundColor = BisqTheme.colors.dark_grey50,
                    focusedBackgroundColor = BisqTheme.colors.dark_grey50,
                    disabledBackgroundColor = BisqTheme.colors.dark_grey50,
                ),
            singleLine = true,
        )
        IconButton(
            onClick = {
                onAddCustomPayment?.invoke(value.trim())
                value = ""
            },
            enabled = value.isNotBlank(),
            colors =
                IconButtonDefaults.iconButtonColors(
                    containerColor = BisqTheme.colors.primary,
                    contentColor = BisqTheme.colors.white,
                    disabledContainerColor = BisqTheme.colors.primaryDisabled,
                    disabledContentColor = BisqTheme.colors.mid_grey20,
                ),
        ) {
            AddIcon()
        }
    }
}
