package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferTradePriceSelectorScreen() {

    val presenter: CreateOfferPricePresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val formattedPercentagePrice by presenter.formattedPercentagePrice.collectAsState()
    val formattedPercentagePriceValid by presenter.formattedPercentagePriceValid.collectAsState()
    val formattedPrice by presenter.formattedPrice.collectAsState()
    val priceType by presenter.priceType.collectAsState()

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.review.price.price".i18n(),
        stepIndex = 4,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onNext() },
        nextDisabled = !presenter.formattedPercentagePriceValid.collectAsState().value,
    ) {
        BisqText.h3Regular(
            text = "bisqEasy.price.headline".i18n(),
            modifier = Modifier.align(Alignment.Start)
        )
        BisqGap.V1()
        BisqText.largeLightGrey("bisqEasy.tradeWizard.price.subtitle".i18n())
        Column(
            modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding2X),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
        ) {
            ToggleTab(
                options = presenter.priceTypes,
                initialOption = priceType,
                onStateChange = { priceType -> presenter.onSelectPriceType(priceType) },
                getDisplayString = { presenter.getPriceTypeDisplayString(it) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
            ) {
                if (priceType == CreateOfferPresenter.PriceType.PERCENTAGE) {
                    BisqTextField(
                        label = "bisqEasy.price.percentage.inputBoxText".i18n(),
                        value = formattedPercentagePrice,
                        keyboardType = KeyboardType.Decimal,
                        onValueChange = { it, isValid -> presenter.onPercentagePriceChanged(it, isValid) },
                        validation = {
                            val parsedValue = it.toDoubleOrNull()
                            if (parsedValue == null) {
                                return@BisqTextField "Value cannot be empty"
                            } else if (parsedValue < -10) {
                                return@BisqTextField "Min: -10%"
                            } else if (parsedValue > 50) {
                                return@BisqTextField "Max: 50%"
                            }
                            return@BisqTextField null
                        }
                    )
                    BisqTextField(
                        label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        onValueChange = { it, isValid -> }, // Deactivated
                        indicatorColor = BisqTheme.colors.grey1
                    )
                } else {
                    BisqTextField(
                        label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        keyboardType = KeyboardType.Decimal,
                        onValueChange = { it, isValid -> presenter.onFixPriceChanged(it, isValid) },
                        validation = {
                            val parsedValue = it.toDoubleOrNull()
                            if (parsedValue == null) {
                                return@BisqTextField "Value cannot be empty"
                            }
                            val parsedPercent = formattedPercentagePrice.toDoubleOrNull()
                            if (parsedPercent != null) {
                                if (parsedPercent < -10) {
                                    return@BisqTextField "Trade price should be greater than -10% of market price"
                                } else if (parsedPercent > 50) {
                                    return@BisqTextField "Trade price should be lesser than 50% of market price"
                                }
                            }
                            return@BisqTextField null
                        }
                    )
                    BisqTextField(
                        label = "bisqEasy.price.percentage.inputBoxText".i18n(),
                        value = formattedPercentagePrice,
                        onValueChange = { it, isValid -> },// Deactivated
                        indicatorColor = BisqTheme.colors.grey1
                    )
                }
            }

            val tempStatus = "bisqEasy.price.feedback.sentence.some".i18n()
            NoteText(
                notes = "bisqEasy.price.feedback.sentence".i18n(tempStatus),
                linkText = "bisqEasy.price.feedback.learnWhySection.openButton".i18n(),
                textAlign = TextAlign.Center,
            )
        }
    }
}