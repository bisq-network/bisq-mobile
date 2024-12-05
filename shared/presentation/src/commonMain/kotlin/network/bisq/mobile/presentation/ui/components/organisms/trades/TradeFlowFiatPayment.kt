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
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
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
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
        BisqText.h6Regular(
            text = strings.bisqEasy_tradeState_info_buyer_phase2a_headline(sendAmount)
        )
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
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
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
        BisqText.smallRegular(
            text = strings.bisqEasy_tradeState_info_buyer_phase2a_reasonForPaymentInfo,
            color = BisqTheme.colors.grey1
        )
        Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
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

    Spacer(modifier = Modifier.height(BisqUIConstants.ScreenPadding))
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