package network.bisq.mobile.presentation.ui.uicases.offers.createOffer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.BisqHDivider
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferReviewOfferScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val paymentMethodStrings = LocalStrings.current.paymentMethod
    val presenter: ICreateOfferPresenter = koinInject()
    val isBuy = presenter.direction.collectAsState().value.isBuy

    val tradeStateStrings = LocalStrings.current.bisqEasyTradeState

    val offer = presenter.offerListItems.collectAsState().value.first()

    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        strings.bisqEasy_tradeWizard_review_headline_maker,
        stepIndex = 6,
        stepsLength = 6,
        prevOnClick = { presenter.goBack() },
        nextButtonText = strings.bisqEasy_tradeWizard_review_nextButton_createOffer,
        nextOnClick = { presenter.createOffer() },
        horizontalAlignment = Alignment.Start
    ) {
        BisqText.h3Regular(
            text = strings.bisqEasy_tradeWizard_review_headline_maker,
            color = BisqTheme.colors.light1,
            textAlign = TextAlign.Start,
            modifier = Modifier.fillMaxWidth()
        )
        BisqGap.V2()
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)) {
            InfoBox(
                label = tradeStateStrings.bisqEasy_tradeState_header_direction.uppercase(),
                value = if (isBuy)
                    strings.bisqEasy_tradeWizard_directionAndMarket_buy
                else
                    strings.bisqEasy_tradeWizard_directionAndMarket_sell
            )
            InfoBox(
                label = stringsBisqEasy.bisqEasy_takeOffer_review_method_fiat,
                value = "Strike, National banks, Steem cards"
            )
            InfoRow(
                label1 = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                value1 = offer.formattedPrice, // TODO: Show selected amount (in case offer has range)
                label2 = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                value2 = offer.formattedQuoteAmount
            )
            BisqHDivider()
            InfoBox(
                label = strings.bisqEasy_tradeWizard_review_priceDescription_taker,
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Regular(text = "98,000.68") // TODO: Values?
                            BisqText.baseRegular(
                                text = "BTC/USD",
                                color = BisqTheme.colors.grey2
                            ) // TODO: Values?
                        }
                        BisqText.smallRegular(
                            text = strings.bisqEasy_tradeWizard_review_priceDetails_float(
                                "1.00%",
                                "above",
                                "60,000 BTC/USD"
                            ),
                            color = BisqTheme.colors.grey4
                        )
                    }
                }
            )

            InfoBox(
                label = stringsBisqEasy.bisqEasy_takeOffer_review_method_bitcoin,
                value = paymentMethodStrings.LN_SHORT
            )
            InfoBox(
                label = stringsBisqEasy.bisqEasy_takeOffer_review_method_fiat,
                value = paymentMethodStrings.STRIKE
            )

            InfoBox(
                label = strings.bisqEasy_tradeWizard_review_feeDescription,
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        BisqText.h6Regular(text = strings.bisqEasy_tradeWizard_review_noTradeFees)
                        BisqText.smallRegular(
                            text = strings.bisqEasy_tradeWizard_review_sellerPaysMinerFeeLong,
                            color = BisqTheme.colors.grey4,
                            )
                    }
                }
            )
        }
    }
}