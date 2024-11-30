package network.bisq.mobile.presentation.ui.uicases.offers.takeOffer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqDivider
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.BisqScrollLayout
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import org.koin.compose.koinInject

@Composable
fun TakeOfferReviewTradeScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val tradeStateStrings = LocalStrings.current.bisqEasyTradeState
    val commonStrings = LocalStrings.current.common
    val presenter: ReviewTradePresenter = koinInject()

    val offer = presenter.offerListItems.collectAsState().value.first()

    MultiScreenWizardScaffold(
        commonStrings.take_offer,
        stepIndex = 2,
        stepsLength = 3,
        prevOnClick = { presenter.goBack() },
        nextOnClick = { presenter.tradeConfirmed() }
    ) {
        BisqScrollLayout(padding = PaddingValues(all = 0.dp)) {
            BisqText.h3Regular(
                text = strings.bisqEasy_tradeWizard_review_headline_taker,
                color = BisqTheme.colors.light1
            )
            Spacer(modifier = Modifier.height(32.dp))
            Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                InfoRow(
                    label1 = tradeStateStrings.bisqEasy_tradeState_header_direction,
                    value1 = if (offer.direction.isBuy)
                        strings.bisqEasy_tradeWizard_directionAndMarket_buy
                    else
                        strings.bisqEasy_tradeWizard_directionAndMarket_sell,
                    label2 = strings.bisqEasy_tradeWizard_review_paymentMethodDescription_fiat,
                    value2 = offer.baseSidePaymentMethods[0], // TODO: Show only selected payment methods
                )
                InfoRow(
                    label1 = strings.bisqEasy_tradeWizard_review_toPay,
                    value1 = offer.formattedPrice, // TODO: Show selected amount (in case offer has range)
                    label2 = strings.bisqEasy_tradeWizard_review_toReceive,
                    value2 = offer.formattedQuoteAmount
                )
            }
            BisqDivider()
            Column(verticalArrangement = Arrangement.spacedBy(32.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BisqText.largeRegular(
                        text = strings.bisqEasy_tradeWizard_review_priceDescription_taker,
                        color = BisqTheme.colors.grey2
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            BisqText.h5Regular(
                                text = "98,000.68"
                            )
                            BisqText.baseRegular(
                                text = "BTC/USD",
                                color = BisqTheme.colors.grey2
                            )
                        }
                        BisqText.smallRegular(
                            text = "Float price 1.00% above market price of 60,000 BTC/USD",
                            color = BisqTheme.colors.grey4
                        )
                    }
                }

                InfoRow(
                    label1 = "Bitcoin settlement method",
                    value1 = "Lightning",
                    label2 = "Fiat Payment",
                    value2 = "Strike",
                )
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    BisqText.largeRegular(
                        text = "Fees",
                        color = BisqTheme.colors.grey2
                    )
                    BisqText.h5Regular(
                        text = "No trade fees in Bisq Easy  :-)"
                    )
                }
            }
        }
    }
}