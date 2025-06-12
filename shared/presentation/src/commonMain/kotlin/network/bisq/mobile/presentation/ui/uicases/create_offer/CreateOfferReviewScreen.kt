package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnum
import network.bisq.mobile.i18n.i18n
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
    val presenter: CreateOfferReviewPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        "bisqEasy.tradeWizard.review.headline.maker".i18n(),
        stepIndex = 6,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "bisqEasy.tradeWizard.review.nextButton.createOffer".i18n(),
        nextOnClick = { presenter.onCreateOffer() },
        isInteractive = presenter.isInteractive.collectAsState().value,
    ) {
        BisqText.h3Regular("bisqEasy.tradeWizard.review.headline.maker".i18n())
        BisqGap.V2()
        Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X)) {
            InfoBox(
                label = "bisqEasy.tradeState.header.direction".i18n().uppercase(),
                value = presenter.headLine,
            )
            InfoBox(
                label = "bisqEasy.takeOffer.review.method.fiat".i18n(),
                value = presenter.quoteSidePaymentMethodDisplayString,
            )
            InfoBox(
                label = "bisqEasy.takeOffer.review.method.bitcoin".i18n(),
                value = presenter.baseSidePaymentMethodDisplayString
            )
            if (presenter.isRangeOffer) {
                if (presenter.direction == DirectionEnum.BUY) {
                    InfoBox(
                        label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                        value = presenter.amountToPay
                    )
                    InfoBox(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        valueComposable = {
                            Row {
                                BtcSatsText(
                                    presenter.formattedBaseRangeMinAmount,
                                    noCode = true,
                                    fontSize = FontSize.H6
                                )
                                BisqText.baseRegular(" - ")
                                BtcSatsText(presenter.formattedBaseRangeMaxAmount, fontSize = FontSize.H6)
                            }
                        }
                    )
                } else {
                    InfoBox(
                        label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                        value = presenter.amountToReceive
                    )
                    InfoBox(
                        label = "bisqEasy.tradeWizard.review.toSend".i18n().uppercase(),
                        valueComposable = {
                            Row {
                                BtcSatsText(
                                    presenter.formattedBaseRangeMinAmount,
                                    noCode = true,
                                    fontSize = FontSize.H6
                                )
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
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = presenter.amountToPay,
                        )
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = presenter.amountToReceive,
                        )
                    }
                } else {
                    InfoRowContainer {
                        InfoBoxSats(
                            label = "bisqEasy.tradeWizard.review.toPay".i18n().uppercase(),
                            value = presenter.amountToPay
                        )
                        InfoBox(
                            label = "bisqEasy.tradeWizard.review.toReceive".i18n().uppercase(),
                            value = presenter.amountToReceive,
                        )
                    }
                }
            }

            BisqHDivider()

            InfoBox(
                label = "bisqEasy.tradeWizard.review.priceDescription.taker".i18n(),
                valueComposable = {
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        BisqText.h6Regular(presenter.formattedPrice)
                        BisqText.baseRegularGrey(presenter.marketCodes)
                    }
                },
                subvalue = presenter.priceDetails
            )

            InfoBox(
                label = "bisqEasy.tradeWizard.review.feeDescription".i18n(),
                value = presenter.fee,
                subvalue = presenter.feeDetails,
            )
        }
    }
}