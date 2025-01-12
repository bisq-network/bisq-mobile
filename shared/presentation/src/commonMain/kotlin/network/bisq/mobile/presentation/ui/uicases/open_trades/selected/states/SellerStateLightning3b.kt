package network.bisq.mobile.presentation.ui.uicases.open_trades.selected.states

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.trade_bitcoin_confirmation
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.CircularLoadingImage
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme

@Composable
fun SellerStateLightning3b(
    presenter: SellerStateLightning3bPresenter,
) {
    RememberPresenterLifecycle(presenter)

    Column {
        BisqGap.V1()
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularLoadingImage(
                image = Res.drawable.trade_bitcoin_confirmation,
                isLoading = true
            )
            // Wait for buyer confirming Bitcoin receipt
            BisqText.h5Light(text = "bisqEasy.tradeState.info.seller.phase3b.headline.ln".i18n())
        }

        Column {
            BisqGap.V1()
            BisqText.baseLight(
                // Transfers via the Lightning Network are usually near-instant and reliable....
                text = "bisqEasy.tradeState.info.seller.phase3b.info.ln".i18n(),
                color = BisqTheme.colors.grey2
            )

            BisqGap.V1()
            BisqButton(
                // Complete trade
                text = "bisqEasy.tradeState.info.seller.phase3b.confirmButton.ln".i18n(),
                onClick = { presenter.onCompleteTrade() },
                padding = PaddingValues(
                    horizontal = 18.dp,
                    vertical = 6.dp
                )
            )
        }
    }
}
