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
import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.components.organisms.create_offer.WhyHighPricePopup
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
    val isBuy by presenter.isBuy.collectAsState()
    val showWhyPopup by presenter.showWhyPopup.collectAsState()
    val hintText by presenter.hintText.collectAsState()

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.review.price.price".i18n(),
        stepIndex = 4,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onNext() },
        nextDisabled = !presenter.formattedPercentagePriceValid.collectAsState().value,
        shouldBlurBg = showWhyPopup,
    ) {
        BisqText.h3Regular(
            text = "mobile.bisqEasy.tradeWizard.price.title".i18n(),
            modifier = Modifier.align(Alignment.Start)
        )
        BisqGap.V1()
        BisqText.largeLightGrey("mobile.bisqEasy.tradeWizard.price.title".i18n())
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
                        numberWithTwoDecimals = true,
                        valueSuffix = "%",
                        validation = {
                            val parsedValue = it.toDoubleOrNullLocaleAware()
                            if (parsedValue == null) {
                                return@BisqTextField "mobile.bisqEasy.tradeWizard.price.tradePrice.type.percentage.validation.cannotBeEmpty".i18n()
                            } else if (parsedValue < -10) {
                                return@BisqTextField "mobile.bisqEasy.tradeWizard.price.tradePrice.type.percentage.validation.shouldBeGreaterThanMarketPrice".i18n()
                            } else if (parsedValue > 50) {
                                return@BisqTextField "mobile.bisqEasy.tradeWizard.price.tradePrice.type.percentage.validation.shouldBeLessThanMarketPrice".i18n()
                            }
                            return@BisqTextField null
                        }
                    )
                    BisqTextField(
                        label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        onValueChange = { it, isValid -> }, // Deactivated
                        indicatorColor = BisqTheme.colors.mid_grey10
                    )
                } else {
                    BisqTextField(
                        label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        keyboardType = KeyboardType.Decimal,
                        onValueChange = { it, isValid -> presenter.onFixPriceChanged(it, isValid) },
                        validation = {
                            val parsedValue = it.toDoubleOrNullLocaleAware() ?: return@BisqTextField "mobile.bisqEasy.tradeWizard.price.tradePrice.type.fixed.validation.cannotBeEmpty".i18n()
                            val parsedPercent = presenter.calculatePercentageForFixedValue(it)
                            if (parsedPercent < -10) {
                                return@BisqTextField "mobile.bisqEasy.tradeWizard.price.tradePrice.type.fixed.validation.shouldBeGreaterThanMarketPrice".i18n()
                            } else if (parsedPercent > 50) {
                                return@BisqTextField "mobile.bisqEasy.tradeWizard.price.tradePrice.type.fixed.validation.shouldBeLessThanMarketPrice".i18n()
                            }
                            return@BisqTextField null
                        }
                    )
                    BisqTextField(
                        label = "bisqEasy.price.percentage.inputBoxText".i18n(),
                        value = formattedPercentagePrice,
                        onValueChange = { it, isValid -> },// Deactivated
                        indicatorColor = BisqTheme.colors.mid_grey10,
                        valueSuffix = "%",
                    )
                }
            }

            if (isBuy) {
                NoteText(
                    notes = hintText,
                    linkText = "bisqEasy.price.feedback.learnWhySection.openButton".i18n(),
                    textAlign = TextAlign.Center,
                    onLinkClick = {
                        presenter.setShowWhyPopup(true)
                    }
                )
            }
        }
    }

    if (showWhyPopup) {
        WhyHighPricePopup(
            onDismiss = { presenter.setShowWhyPopup(false) }
        )
    }
}