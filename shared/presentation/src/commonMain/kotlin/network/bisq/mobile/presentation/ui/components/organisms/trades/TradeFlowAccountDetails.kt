package network.bisq.mobile.presentation.ui.components.organisms.trades

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.*
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.trades.ITradeFlowPresenter
import org.koin.compose.koinInject

/**
 * Trade flow's 1st Stepper section
 */
@Composable
fun TradeFlowAccountDetails(
    onNext: () -> Unit
) {
    val strings = LocalStrings.current.bisqEasyTradeState
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val presenter: ITradeFlowPresenter = koinInject()

    Column {
        BisqGap.V1()
        BisqText.baseRegular(text = strings.bisqEasy_tradeState_info_buyer_phase1a_seller_wait_message)
        BisqGap.V2()
        BisqText.h6Regular(
            text = strings.bisqEasy_tradeState_info_buyer_phase1a_bitcoinPayment_headline_LN
        )
        BisqGap.V1()
        BisqTextField(
            label = strings.bisqEasy_tradeState_info_buyer_phase1a_bitcoinPayment_description_LN,
            value = presenter.receiveAddress.collectAsState().value,
            onValueChanged = { presenter.setReceiveAddress(it) },
        )
        BisqGap.V1()
        BisqButton(
            text = strings.bisqEasy_tradeState_info_buyer_phase1a_send,
            onClick = onNext,
            padding = PaddingValues(
                horizontal = 18.dp,
                vertical = 6.dp
            )
        )
        BisqGap.V1()
        BisqText.smallMedium(
            text = strings.bisqEasy_tradeState_info_buyer_phase1a_wallet_prompt_prefix
        )
        // TODO: Make a small variation of the button
        BisqButton(text = stringsBisqEasy.bisqEasy_walletGuide_tabs_headline, onClick = { presenter.openWalletGuideLink() })
    }
}