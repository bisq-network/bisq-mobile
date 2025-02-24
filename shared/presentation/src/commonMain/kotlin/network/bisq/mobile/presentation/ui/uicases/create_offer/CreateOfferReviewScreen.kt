package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BtcSatsText
import network.bisq.mobile.presentation.ui.components.atoms.FontSize
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqHDivider
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBox
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoBoxSats
import network.bisq.mobile.presentation.ui.components.molecules.info.InfoRowContainer
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferReviewOfferScreen() {
    val strings = LocalStrings.current.bisqEasyTradeWizard
    val stringsBisqEasy = LocalStrings.current.bisqEasy
    val tradeStateStrings = LocalStrings.current.bisqEasyTradeState
    val presenter: CreateOfferReviewPresenter = koinInject()
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution
    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        strings.bisqEasy_tradeWizard_review_headline_maker,
        stepIndex = 6,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = strings.bisqEasy_tradeWizard_review_nextButton_createOffer,
        nextOnClick = { presenter.onCreateOffer() },
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        BisqText.h3Regular(text = strings.bisqEasy_tradeWizard_review_headline_maker)
        BisqGap.V2()
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)) {
            InfoBox(
                label = tradeStateStrings.bisqEasy_tradeState_header_direction.uppercase(),
                value = presenter.headLine,
            )
            InfoBox(
                label = stringsBisqEasy.bisqEasy_takeOffer_review_method_fiat,
                value = presenter.quoteSidePaymentMethodDisplayString,
            )
            if (presenter.isRangeOffer) {
                if (presenter.direction == DirectionEnum.BUY) {
                    InfoBox(
                        label = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                        value = presenter.amountToPay
                    )
                    InfoBox(
                        label = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                        valueComposable = {
                            Row {
                                BtcSatsText(presenter.formattedBaseRangeMinAmount, noCode = true, fontSize = FontSize.H6)
                                BisqText.baseRegular(" - ")
                                BtcSatsText(presenter.formattedBaseRangeMaxAmount, fontSize = FontSize.H6)
                            }
                        }
                    )
                } else {
                    InfoBox(
                        label = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                        value = presenter.amountToReceive
                    )
                    InfoBox(
                        label = strings.bisqEasy_tradeWizard_review_toSend.uppercase(),
                        valueComposable = {
                            Row {
                                BtcSatsText(presenter.formattedBaseRangeMinAmount, noCode = true, fontSize = FontSize.H6)
                                BisqText.baseRegular(" - ")
                                BtcSatsText(presenter.formattedBaseRangeMaxAmount, fontSize = FontSize.H6)
                            }
                        }
                    )
                }
            } else {
                if (presenter.direction == DirectionEnum.BUY) {
                    InfoRowContainer {
                        InfoBox(
                            label = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                            value = presenter.amountToPay,
                        )
                        InfoBoxSats(
                            label = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                            value = presenter.amountToReceive,
                        )
                    }
                } else {
                    InfoRowContainer {
                        InfoBoxSats(
                            label = strings.bisqEasy_tradeWizard_review_toPay.uppercase(),
                            value = presenter.amountToPay
                        )
                        InfoBox(
                            label = strings.bisqEasy_tradeWizard_review_toReceive.uppercase(),
                            value = presenter.amountToPay,
                        )
                    }
                }
            }

            BisqHDivider()

            InfoBox(
                label = strings.bisqEasy_tradeWizard_review_priceDescription_taker,
                valueComposable = {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BisqText.h6Regular(text = presenter.formattedPrice)
                        BisqText.baseRegularGrey(text = presenter.marketCodes)
                    }
                },
                subvalue = presenter.priceDetails
            )

            InfoBox(
                label = stringsBisqEasy.bisqEasy_takeOffer_review_method_bitcoin,
                value = presenter.quoteSidePaymentMethodDisplayString
            )
            InfoBox(
                label = stringsBisqEasy.bisqEasy_takeOffer_review_method_fiat,
                value = presenter.baseSidePaymentMethodDisplayString
            )

            InfoBox(
                label = strings.bisqEasy_tradeWizard_review_feeDescription,
                value = presenter.fee,
                subvalue = presenter.feeDetails,
            )
        }
    }
}