package network.bisq.mobile.presentation.ui.uicases.offers.takeOffer

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.domain.data.model.OfferListItem
import network.bisq.mobile.presentation.ViewPresenter
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

interface ITakeOfferReviewTradePresenter : ViewPresenter {
    // TODO: Update later to refer to a single OfferListItem
    val offerListItems: StateFlow<List<OfferListItem>>
    fun tradeConfirmed()
}

@Composable
fun TakeOfferReviewTradeScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val stringsTradeState = LocalStrings.current.bisqEasyTradeState
    val commonStrings = LocalStrings.current.common
    val presenter: ITakeOfferReviewTradePresenter = koinInject()

    val offer = presenter.offerListItems.collectAsState().value.first()

    MultiScreenWizardScaffold(
        stringsBisqEasy.bisqEasy_takeOffer_progress_review,
        stepIndex = 3,
        stepsLength = 3,
        prevOnClick = { presenter.goBack() },
        nextButtonText = stringsBisqEasy.bisqEasy_takeOffer_review_takeOffer,
        nextOnClick = { presenter.tradeConfirmed() }
    ) {
        BisqText.h3Regular(
            text = stringsBisqEasy.bisqEasy_takeOffer_progress_review,
            color = BisqTheme.colors.light1
        )
        BisqGap.V2()
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)) {
            InfoRow(
                label1 = stringsTradeState.bisqEasy_tradeState_header_direction.uppercase(),
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
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Regular(text = "98,000.68") // TODO: Values?
                            BisqText.baseRegular(text = "BTC/USD", color = BisqTheme.colors.grey2) // TODO: Values?
                        }
                        BisqText.smallRegular(
                            text = strings.bisqEasy_tradeWizard_review_priceDetails_float("1.00%", "above", "60,000 BTC/USD"),
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