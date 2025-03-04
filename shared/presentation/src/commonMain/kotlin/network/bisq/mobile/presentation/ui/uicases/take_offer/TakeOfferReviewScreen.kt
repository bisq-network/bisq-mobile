package network.bisq.mobile.presentation.ui.uicases.take_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRow
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.ui.components.organisms.offer.TakeOfferProgressDialog
import network.bisq.mobile.presentation.ui.components.organisms.offer.TakeOfferSuccessDialog
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
    val showProgressDialog = presenter.showTakeOfferProgressDialog.collectAsState().value
    val showSuccessDialog = presenter.showTakeOfferSuccessDialog.collectAsState().value

    RememberPresenterLifecycle(presenter)
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution

    MultiScreenWizardScaffold(
        stringsBisqEasy.bisqEasy_takeOffer_progress_review,
        stepIndex = 3,
        stepsLength = 3,
        prevOnClick = { presenter.onBack() },
        nextButtonText = stringsBisqEasy.bisqEasy_takeOffer_review_takeOffer,
        nextOnClick = { presenter.onTakeOffer() },
        snackbarHostState = presenter.getSnackState(),
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        BisqText.h3Regular(text = stringsBisqEasy.bisqEasy_takeOffer_progress_review)
        BisqGap.V2()
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)) {
            InfoRow(
                label1 = stringsTradeState.bisqEasy_tradeState_header_direction.uppercase(),
                value1 = presenter.headLine,
                label2 = strings.bisqEasy_tradeWizard_review_paymentMethodDescription_fiat.uppercase(),
                value2 = presenter.quoteSidePaymentMethodDisplayString,
            )
            if (presenter.takersDirection.isBuy) {
                if (presenter.isSmallScreen()) {
                    InfoBox(
                        label = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                        value = presenter.amountToPay,
                    )
                    InfoBoxSats(
                        label = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                        value = presenter.amountToReceive,
                        rightAlign = true
                    )
                } else {
                    InfoRowContainer {
                        InfoBox(
                            label = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                            value = presenter.amountToPay,
                        )
                        InfoBoxSats(
                            label = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                            value = presenter.amountToReceive,
                            rightAlign = true
                        )
                    }
                }
            } else {
                if (presenter.isSmallScreen()) {
                    InfoBoxSats(
                        label = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                        value = presenter.amountToPay,
                    )
                    InfoBox(
                        label = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                        value = presenter.amountToReceive,
                        rightAlign = true
                    )
                } else {
                    InfoRowContainer {
                        InfoBoxSats(
                            label = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                            value = presenter.amountToPay,
                        )
                        InfoBox(
                            label = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                            value = presenter.amountToReceive,
                            rightAlign = true
                        )
                    }
                }
            }
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
                            BisqText.baseRegularGrey(text = presenter.marketCodes)
                        }
                        BisqText.smallRegularGrey(text = presenter.priceDetails)
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
                        BisqText.smallRegularGrey(text = presenter.feeDetails)
                    }
                }
            )
        }

        if (showProgressDialog) {
            TakeOfferProgressDialog()
        }

        if (showSuccessDialog) {
            TakeOfferSuccessDialog(
                onShowTrades = { presenter.onGoToOpenTrades() }
            )
        }

    }
}