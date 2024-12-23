package network.bisq.mobile.presentation.ui.uicases.trade.take_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun TakeOfferReviewTradeScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val stringsTradeState = LocalStrings.current.bisqEasyTradeState
    val commonStrings = LocalStrings.current.common
    val presenter: TakeOfferReviewPresenter = koinInject()
    RememberPresenterLifecycle(presenter)
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution

    MultiScreenWizardScaffold(
        stringsBisqEasy.bisqEasy_takeOffer_progress_review,
        stepIndex = 3,
        stepsLength = 3,
        prevOnClick = { presenter.onBack() },
        nextButtonText = stringsBisqEasy.bisqEasy_takeOffer_review_takeOffer,
        nextOnClick = { presenter.onTakeOffer() }
    ) {
        BisqText.h3Regular(
            text = stringsBisqEasy.bisqEasy_takeOffer_progress_review,
            color = BisqTheme.colors.light1
        )
        BisqGap.V2()
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)) {
            InfoRow(
                label1 = stringsTradeState.bisqEasy_tradeState_header_direction.uppercase(),
                value1 = presenter.headLine,
                label2 = strings.bisqEasy_tradeWizard_review_paymentMethodDescription_fiat.uppercase(),
                value2 = presenter.quoteSidePaymentMethodDisplayString,
            )
            InfoRow(
                label1 = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                value1 = presenter.amountToPay,
                label2 = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                value2 = presenter.amountToReceive
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
                            BisqText.h6Regular(text = presenter.price)
                            BisqText.baseRegular(
                                text = presenter.marketCodes,
                                color = BisqTheme.colors.grey2
                            )
                        }
                        BisqText.smallRegular(
                            text = presenter.priceDetails,
                            color = BisqTheme.colors.grey4
                        )
                    }
                }
            )

            InfoRow(
                label1 = strings.bisqEasy_tradeWizard_review_paymentMethodDescription_fiat,
                value1 = presenter.quoteSidePaymentMethodDisplayString,
                label2 = strings.bisqEasy_tradeWizard_review_paymentMethodDescription_btc,
                value2 = presenter.baseSidePaymentMethodDisplayString,
            )

            InfoBox(
                label = strings.bisqEasy_tradeWizard_review_feeDescription,
                valueComposable = {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.Bottom,
                            horizontalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            BisqText.h6Regular(text = presenter.fee)
                        }
                        BisqText.smallRegular(
                            text = presenter.feeDetails,
                            color = BisqTheme.colors.grey4
                        )
                    }
                }
            )
        }
    }
}