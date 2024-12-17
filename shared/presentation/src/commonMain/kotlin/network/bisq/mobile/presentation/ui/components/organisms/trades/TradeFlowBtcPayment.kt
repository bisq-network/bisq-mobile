package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.img_bitcoin_payment_confirmation
import bisqapps.shared.presentation.generated.resources.img_bitcoin_payment_waiting
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.delay
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants

/**
 * Trade flow's 3rd Stepper section
 */
@Composable
fun TradeFlowBtcPayment(
    onNext: () -> Unit
) {
    val strings = LocalStrings.current.bisqEasyTradeState
    var isLoading by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        delay(3000)
        isLoading = false
    }

    if (isLoading) {
        ShowLoaderBtcPayment(onNext)
        return
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BisqGap.V1()
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
                text = strings.bisqEasy_tradeState_info_buyer_phase3b_headline_ln,
            )
            BisqText.smallRegular(
                text = strings.bisqEasy_tradeState_info_buyer_phase3b_info_ln,
                color = BisqTheme.colors.grey1
            )
            BisqButton(
                text = strings.bisqEasy_tradeState_info_buyer_phase3b_confirmButton_ln,
                onClick = onNext,
                padding = PaddingValues(horizontal = 18.dp, 6.dp)
            )
        }
    }
}

@Composable
fun ShowLoaderBtcPayment(
    onNext: () -> Unit
) {
    val strings = LocalStrings.current.bisqEasyTradeState

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        BisqGap.V1()
        CircularLoadingImage(
            image = Res.drawable.img_bitcoin_payment_waiting,
            isLoading = true
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(
                12.dp
            )
        ) {
            BisqText.h6Regular(
                text = strings.bisqEasy_tradeState_info_buyer_phase3a_seller_wait_message
            )
            BisqText.smallRegular(
                text = strings.bisqEasy_tradeState_info_buyer_phase3a_info("Lightning invoice"),
                color = BisqTheme.colors.grey1
            )
        }
    }
}
