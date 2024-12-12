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
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.delay
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.trades.ITradeFlowPresenter
import org.koin.compose.koinInject

/**
 * Trade flow's 2nd Stepper section
 */
@Composable
fun TradeFlowFiatPayment(
    onNext: () -> Unit
) {
    val strings = LocalStrings.current.bisqEasyTradeState
    var isLoading by remember { mutableStateOf(false) }
    val presenter: ITradeFlowPresenter = koinInject()

    val sendAmount = "1234.56 USD"
    val sendID = "someone@zelle.com"

    if (presenter.confirmingFiatPayment.collectAsState().value) {
        ShowLoaderFiatPayment(onNext)
        return
    }

    Column {
        BisqGap.V1()
        BisqText.h6Regular(
            text = strings.bisqEasy_tradeState_info_buyer_phase2a_headline(sendAmount)
        )
        BisqGap.V1()
        BisqTextField(
            value = sendAmount,
            onValueChanged = {},
            label = strings.bisqEasy_tradeState_info_buyer_phase2a_quoteAmount,
            disabled = true,
        )
        BisqTextField(
            value = sendID,
            onValueChanged = {},
            label = strings.bisqEasy_tradeState_info_buyer_phase2a_sellersAccount,
            disabled = true,
        )
        BisqGap.V1()
        BisqText.smallRegular(
            text = strings.bisqEasy_tradeState_info_buyer_phase2a_reasonForPaymentInfo,
            color = BisqTheme.colors.grey1
        )
        BisqGap.V1()
        BisqButton(
            text = strings.bisqEasy_tradeState_info_buyer_phase2a_confirmFiatSent(sendAmount),
            onClick = { presenter.confirmFiatPayment() },
            padding = PaddingValues(
                horizontal = 18.dp,
                vertical = 6.dp
            )
        )
    }
}

@Composable
fun ShowLoaderFiatPayment(
    onNext: () -> Unit
) {
    val strings = LocalStrings.current.bisqEasyTradeState
    val sendAmount = "1234.56 USD"

    LaunchedEffect(Unit) {
        delay(3000)
        // isLoading = false
        onNext()
    }

    BisqGap.V1()
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        CircularLoadingImage(
            image = Res.drawable.img_fiat_payment_waiting,
            isLoading = true
        )

        Column(
            verticalArrangement = Arrangement.spacedBy(
                12.dp
            )
        ) {
            BisqText.h6Regular(
                text = strings.bisqEasy_tradeState_info_buyer_phase2a_seller_wait_message
            )
            BisqText.smallRegular(
                text = strings.bisqEasy_tradeState_info_buyer_phase2b_info(sendAmount, "Lightning invoice"),
                color = BisqTheme.colors.grey1
            )
        }
    }
}