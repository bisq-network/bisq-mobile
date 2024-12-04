package network.bisq.mobile.presentation.ui.components.organisms.trades
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bitcoin_payment_confirmation
import bisqapps.shared.presentation.generated.resources.img_bitcoin_payment_waiting
import kotlinx.coroutines.delay
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun TradeFlow03BtcPayment(
    onNext: () -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(3000)
        isLoading = false
    }
    if (isLoading) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.img_bitcoin_payment_waiting,
                isLoading = isLoading
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(
                    12.dp
                )
            ) {
                BisqText.h6Regular(
                    text = "Waiting for the seller’s Bitcoin settlement"
                )
                BisqText.smallRegular(
                    text = "The seller need to start the Bitcoin transfer to your provided Lightning invoice.",
                    color = BisqTheme.colors.grey1
                )
            }
        }
    } else {

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.img_bitcoin_payment_confirmation,
                isLoading = !isLoading
            )

            Column(
                verticalArrangement = Arrangement.spacedBy(
                    12.dp
                )
            ) {
                BisqText.h6Regular(
                    text = "The seller has sent the Bitcoin via Lightning network"
                )
                BisqText.smallRegular(
                    text = "Transfers via the Lightning Network are typically near-instant. If you haven't received the payment within one minute, please contact the seller in the trade. Occasionally, payments may fail and need to be retried.",
                    color = BisqTheme.colors.grey1
                )
                BisqButton(
                    text = "Confirm receipt",
                    onClick = onNext,
                    padding = PaddingValues(horizontal = 18.dp, 6.dp)
                )
            }
        }
    }
}
