package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_fiat_payment_waiting
import kotlinx.coroutines.delay
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun TradeFlow02FiatPayment(
    onNext: () -> Unit
) {
    var isLoading by remember { mutableStateOf(false) }

    if (!isLoading) {
        Column {
            BisqText.h6Regular(
                text = "Send 10000.02 USD to the seller’s payment account"
            )
            BisqTextField(
                value = "10000.02 USD",
                onValueChanged = {},
                label = "Amount to transfer",
                placeholder = ""
            )
            BisqTextField(
                value = "someone@zelle.com",
                onValueChanged = {},
                label = "Payment account of seller",
                placeholder = ""
            )
            BisqText.smallRegular(
                text = "Please leave the ‘Reason for payment’ field empty, in case you make a bank transfer",
                color = BisqTheme.colors.grey1
            )
            BisqButton(
                text = "Confirm payment of 10000.02 USD",
                onClick = {
                    isLoading = true
                },
                padding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 6.dp
                )
            )
        }
    } else {
        LaunchedEffect(Unit) {
            delay(3000)
            isLoading = false
            onNext()
        }
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.img_fiat_payment_waiting,
                isLoading = isLoading
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(
                    12.dp
                )
            ) {
                BisqText.h6Regular(
                    text = "Waiting for the seller to confirm receipt of payment"
                )
                BisqText.smallRegular(
                    text = "Once the seller has received the payment of 10000.02 USD, they will start the Bitcoin transfer to your provided Lightning invoice.",
                    color = BisqTheme.colors.grey1
                )
            }
        }
    }

    /*
    Column {
        BisqText.h6Regular(
            text = "Send 10000.02 USD to the seller’s payment account"
        )
        BisqTextField(
            value = "10000.02 USD",
            onValueChanged = {},
            label = "Amount to transfer",
            placeholder = ""
        )
        BisqTextField(
            value = "someone@zelle.com",
            onValueChanged = {},
            label = "Payment account of seller",
            placeholder = ""
        )
        BisqText.smallRegular(
            text = "Please leave the ‘Reason for payment’ field empty, in case you make a bank transfer",
            color = BisqTheme.colors.grey1
        )
        BisqButton(
            text = "Confirm payment of 10000.02 USD",
            onClick = onNext,
            padding = PaddingValues(
                horizontal = 18.dp,
                vertical = 6.dp
            )
        )
    }
    */
}