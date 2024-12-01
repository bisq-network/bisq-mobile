package network.bisq.mobile.presentation.ui.uicases.offers.takeOffer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqHDivider
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.text.InfoBox
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import org.koin.compose.koinInject

@Composable
fun TakeOfferReviewTradeScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val bisqEasyStrings = LocalStrings.current.bisqEasy
    val tradeStateStrings = LocalStrings.current.bisqEasyTradeState
    val commonStrings = LocalStrings.current.common
    val presenter: ReviewTradePresenter = koinInject()

    val offer = presenter.offerListItems.collectAsState().value.first()

    MultiScreenWizardScaffold(
        commonStrings.take_offer,
        stepIndex = 2,
        stepsLength = 3,
        prevOnClick = { presenter.goBack() },
        nextButtonText = bisqEasyStrings.bisqEasy_takeOffer_review_takeOffer,
        nextOnClick = { presenter.tradeConfirmed() }
    ) {
        BisqText.h3Regular(
            text = strings.bisqEasy_tradeWizard_review_headline_taker,
            color = BisqTheme.colors.light1
        )
        Spacer(modifier = Modifier.height(16.dp))
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            InfoRow(
                label1 = tradeStateStrings.bisqEasy_tradeState_header_direction.uppercase(),
                value1 = if (offer.direction.isBuy)
                    strings.bisqEasy_tradeWizard_directionAndMarket_buy
                else
                    strings.bisqEasy_tradeWizard_directionAndMarket_sell,
                label2 = strings.bisqEasy_tradeWizard_review_paymentMethodDescription_fiat.uppercase(),
                value2 = offer.quoteSidePaymentMethods[0], // TODO: Show only selected method
            )
            InfoRow(
                label1 = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                value1 = offer.formattedPrice, // TODO: Show selected amount (in case offer has range)
                label2 = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                value2 = offer.formattedQuoteAmount
            )
        }
        BisqHDivider()
        Column(verticalArrangement = Arrangement.spacedBy(24.dp)) {
            InfoBox(
                label = strings.bisqEasy_tradeWizard_review_priceDescription_taker,
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            BisqText.h6Regular(text = "98,000.68") // TODO: Values?
                            BisqText.baseRegular(text = "BTC/USD", color = BisqTheme.colors.grey2) // TODO: Values?
                        }
                        BisqText.smallRegular(
                            text = "Float price 1.00% above market price of 60,000 BTC/USD",
                            color = BisqTheme.colors.grey4
                        )
                    }
                }
            )

            InfoRow(
                label1 = strings.bisqEasy_tradeWizard_review_paymentMethodDescription_btc,
                value1 = offer.baseSidePaymentMethods[0], // TODO: Show only selected method
                label2 = strings.bisqEasy_tradeWizard_review_paymentMethodDescription_fiat,
                value2 = offer.quoteSidePaymentMethods[0], // TODO: Show only selected method
            )
            InfoBox(
                label = strings.bisqEasy_tradeWizard_review_feeDescription,
                value = strings.bisqEasy_tradeWizard_review_noTradeFees
            )
        }
    }
}