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
import cafe.adriel.lyricist.LocalStrings
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.helpers.StringHelper
import network.bisq.mobile.presentation.ui.theme.BisqTheme
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import org.koin.compose.koinInject

@Composable
fun CreateOfferTradePriceSelectorScreen() {
    val bisqEasyTradeWizardStrings = LocalStrings.current.bisqEasyTradeWizard
    val commonStrings = LocalStrings.current.common
    val bisqEasyStrings = LocalStrings.current.bisqEasy

    val presenter: CreateOfferPricePresenter = koinInject()
    presenter.appStrings = LocalStrings.current // TODO find a more elegant solution
    RememberPresenterLifecycle(presenter)

    val formattedPercentagePrice by presenter.formattedPercentagePrice.collectAsState()
    val formattedPercentagePriceValid by presenter.formattedPercentagePriceValid.collectAsState()
    val formattedPrice by presenter.formattedPrice.collectAsState()
    val priceType by presenter.priceType.collectAsState()

    MultiScreenWizardScaffold(
        bisqEasyStrings.bisqEasy_takeOffer_review_price_price,
        stepIndex = 4,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = commonStrings.buttons_next,
        nextOnClick = { presenter.onNext() },
        nextDisabled = !presenter.formattedPercentagePriceValid.collectAsState().value,
    ) {
        BisqText.h3Regular(
            text = bisqEasyTradeWizardStrings.bisqEasy_price_headline,
            modifier = Modifier.align(Alignment.Start)
        )
        BisqGap.V1()
        BisqText.largeLightGrey(text = bisqEasyTradeWizardStrings.bisqEasy_tradeWizard_price_subtitle)
        Column(
            modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding2X),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
        ) {
            ToggleTab(
                options = presenter.priceTypes,
                initialOption = priceType,
                onStateChange = { priceType -> presenter.onSelectPriceType(priceType) },
                getDisplayString = { presenter.getPriceTypeDisplayString(it) },
                textWidth = StringHelper.calculateTotalWidthOfStrings(strings = presenter.priceTypes),
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
            ) {
                if (priceType == CreateOfferPresenter.PriceType.PERCENTAGE) {
                    BisqTextField(
                        label = bisqEasyTradeWizardStrings.bisqEasy_price_percentage_inputBoxText,
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
                        label = bisqEasyTradeWizardStrings.bisqEasy_price_percentage_inputBoxText,
                        value = formattedPercentagePrice,
                        onValueChange = { it, isValid -> },// Deactivated
                        indicatorColor = BisqTheme.colors.grey1
                    )
                }
            }
            NoteText(
                notes = bisqEasyTradeWizardStrings.bisqEasy_price_feedback_sentence,
                linkText = bisqEasyTradeWizardStrings.bisqEasy_price_feedback_learnWhySection_openButton,
                textAlign = TextAlign.Center
            )
        }
    }
}