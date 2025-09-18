package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.AmountSlider
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.BisqTextField
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.CloseIcon
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
    val createPresenter: CreateOfferPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val formattedPercentagePrice by presenter.formattedPercentagePrice.collectAsState()
    val formattedPercentagePriceValid by presenter.formattedPercentagePriceValid.collectAsState()
    val formattedPrice by presenter.formattedPrice.collectAsState()
    val priceType by presenter.priceType.collectAsState()
    val isBuy by presenter.isBuy.collectAsState()
    val showWhyPopup by presenter.showWhyPopup.collectAsState()
    val hintText by presenter.hintText.collectAsState()

    val min = -10f
    val max = 50f

    val percentagePrice = formattedPercentagePrice.toDoubleOrNullLocaleAware()?.toFloat() ?: 0f
    val sliderPosition = ((percentagePrice - min) / (max - min)).coerceIn(0f, 1f)

    fun onSliderValueChange(newValue: Float) {
        val price = min + newValue * (max - min)
        presenter.onPercentagePriceChanged(price.toString(), true)
    }

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.review.price.price".i18n(),
        stepIndex = if (createPresenter.skipCurrency) 3 else 4,
        stepsLength = if (createPresenter.skipCurrency) 6 else 7,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onNext() },
        nextDisabled = !formattedPercentagePriceValid,
        shouldBlurBg = showWhyPopup,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose,
    ) {
        BisqText.h3Light(
            text = "mobile.bisqEasy.tradeWizard.price.title".i18n(), modifier = Modifier.align(Alignment.Start)
        )
        BisqGap.V1()
        Column(
            modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding2X),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
        ) {
            ToggleTab(
                options = presenter.priceTypes,
                selectedOption = priceType,
                onOptionSelected = { priceType -> presenter.onSelectPriceType(priceType) },
                getDisplayString = { presenter.getPriceTypeDisplayString(it) },
                modifier = Modifier.align(Alignment.CenterHorizontally)
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)
            ) {
                if (priceType == CreateOfferPresenter.PriceType.PERCENTAGE) {
                    BisqTextField(label = "bisqEasy.price.percentage.inputBoxText".i18n(),
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
                        })

                    BisqTextField(
                        label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        disabled = true,
                        onValueChange = { it, isValid -> }, // Deactivated
                        indicatorColor = BisqTheme.colors.mid_grey10
                    )
                } else {
                    BisqTextField(label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        keyboardType = KeyboardType.Decimal,
                        onValueChange = { it, isValid -> presenter.onFixPriceChanged(it, isValid) },
                        validation = {
                            it.toDoubleOrNullLocaleAware()
                                ?: return@BisqTextField "mobile.bisqEasy.tradeWizard.price.tradePrice.type.fixed.validation.cannotBeEmpty".i18n()
                            val parsedPercent = presenter.calculatePercentageForFixedValue(it)
                            if (parsedPercent < -10) {
                                return@BisqTextField "mobile.bisqEasy.tradeWizard.price.tradePrice.type.fixed.validation.shouldBeGreaterThanMarketPrice".i18n()
                            } else if (parsedPercent > 50) {
                                return@BisqTextField "mobile.bisqEasy.tradeWizard.price.tradePrice.type.fixed.validation.shouldBeLessThanMarketPrice".i18n()
                            }
                            return@BisqTextField null
                        })
                    BisqTextField(
                        label = "bisqEasy.price.percentage.inputBoxText".i18n(),
                        value = formattedPercentagePrice,
                        onValueChange = { it, isValid -> },// Deactivated
                        disabled = true,
                        indicatorColor = BisqTheme.colors.mid_grey10,
                        valueSuffix = "%",
                    )
                }

                BisqGap.V1()

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    AmountSlider(value = sliderPosition, onValueChange = { onSliderValueChange(it) })

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp)
                    ) {
                        BisqText.smallLightGrey("Min: ${min.toInt()}%")
                        BisqText.smallLightGrey("Max: ${max.toInt()}%")
                    }
                }
            }

            if (isBuy) {
                BisqGap.V1()

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    NoteText(notes = hintText,
                        linkText = "bisqEasy.price.feedback.learnWhySection.openButton".i18n(),
                        textAlign = TextAlign.Center,
                        onLinkClick = {
                            presenter.setShowWhyPopup(true)
                        })
                }
            }
        }
    }

    if (showWhyPopup) {
        WhyHighPricePopup(onDismiss = { presenter.setShowWhyPopup(false) })
    }
}
