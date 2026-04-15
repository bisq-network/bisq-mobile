package network.bisq.mobile.presentation.offer.create_offer.price

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.data.utils.toDoubleOrNullLocaleAware
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldColors
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.atoms.slider.BisqSlider
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.common.ui.components.organisms.create_offer.WhyHighPricePopup
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferCoordinator
import org.koin.compose.koinInject

private const val MIN_ALLOWED_PERCENTAGE_FRACTION = -10f
private const val MAX_ALLOWED_PERCENTAGE_FRACTION = 50f

@ExcludeFromCoverage
@Composable
fun CreateOfferPriceScreen() {
    val presenter: CreateOfferPricePresenter = koinInject()
    val createCoordinator: CreateOfferCoordinator = koinInject()
    RememberPresenterLifecycle(presenter)

    val formattedPercentagePrice by presenter.formattedPercentagePrice.collectAsState()
    val formattedPercentagePriceValid by presenter.formattedPercentagePriceValid.collectAsState()
    val formattedPrice by presenter.formattedPrice.collectAsState()
    val priceType by presenter.priceType.collectAsState()
    val isBuy by presenter.isBuy.collectAsState()
    val showWhyPopup by presenter.showWhyPopup.collectAsState()
    val hintText by presenter.hintText.collectAsState()

    val min = MIN_ALLOWED_PERCENTAGE_FRACTION
    val max = MAX_ALLOWED_PERCENTAGE_FRACTION
    var percentageError by remember(priceType) { mutableStateOf<String?>(null) }
    var fixedPriceError by remember(priceType) { mutableStateOf<String?>(null) }

    val percentagePrice = formattedPercentagePrice.toDoubleOrNullLocaleAware()?.toFloat() ?: 0f
    val sliderPosition = ((percentagePrice - min) / (max - min)).coerceIn(0f, 1f)

    fun onSliderValueChange(newValue: Float) {
        val price = min + newValue * (max - min)
        presenter.onPercentagePriceChanged(price.toString(), true)
    }

    val validateFormattedPercentagePrice =
        remember {
            { it: Double? ->
                when {
                    it == null -> "mobile.validation.valueCannotBeEmpty".i18n()
                    it < -10 -> "mobile.bisqEasy.tradeWizard.price.tradePrice.type.percentage.validation.shouldBeGreaterThanMarketPrice".i18n()
                    it > 50 -> "mobile.bisqEasy.tradeWizard.price.tradePrice.type.percentage.validation.shouldBeLessThanMarketPrice".i18n()
                    else -> null
                }
            }
        }

    val onFormattedPercentagePriceChange =
        remember {
            { it: String ->
                val parsedValue = it.toDoubleOrNullLocaleAware()
                percentageError = validateFormattedPercentagePrice(parsedValue)
                presenter.onPercentagePriceChanged(it, percentageError == null)
            }
        }

    val onFormattedPriceChange =
        remember {
            { it: String ->
                fixedPriceError =
                    if (it.toDoubleOrNullLocaleAware() == null) "mobile.validation.valueCannotBeEmpty".i18n() else null
                if (fixedPriceError == null) {
                    val parsedPercent = presenter.calculatePercentageForFixedValue(it) * 100
                    fixedPriceError = validateFormattedPercentagePrice(parsedPercent)
                }
                presenter.onFixPriceChanged(it, fixedPriceError == null)
            }
        }

    // todo: following LaunchedEffect needs to be removed. requires refactor of CreateOfferPriceScreen and it's presenter
    LaunchedEffect(Unit) {
        // the following is only fine because the value is initially set at init in presenter, before it's collected
        onFormattedPercentagePriceChange(formattedPercentagePrice)
        onFormattedPriceChange(formattedPrice)
    }

    MultiScreenWizardScaffold(
        "bisqEasy.takeOffer.review.price.price".i18n(),
        stepIndex = if (createCoordinator.skipCurrency) 3 else 4,
        stepsLength = if (createCoordinator.skipCurrency) 6 else 7,
        prevOnClick = { presenter.onBack() },
        nextButtonText = "action.next".i18n(),
        nextOnClick = { presenter.onNext() },
        nextDisabled = !formattedPercentagePriceValid,
        shouldBlurBg = showWhyPopup,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose,
    ) {
        BisqText.H3Light(
            text = "mobile.bisqEasy.tradeWizard.price.title".i18n(),
            modifier = Modifier.align(Alignment.Start),
        )
        BisqGap.V1()
        Column(
            modifier = Modifier.padding(vertical = BisqUIConstants.ScreenPadding2X),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            ToggleTab(
                options = presenter.priceTypes,
                selectedOption = priceType,
                onOptionSelect = { priceType -> presenter.onSelectPriceType(priceType) },
                getDisplayString = { presenter.getPriceTypeDisplayString(it) },
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                if (priceType == CreateOfferCoordinator.PriceType.PERCENTAGE) {
                    BisqTextFieldV0(
                        label = "bisqEasy.price.percentage.inputBoxText".i18n(),
                        value = formattedPercentagePrice,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        onValueChange = onFormattedPercentagePriceChange,
                        suffix = { BisqText.BaseLightGrey("%") },
                        isError = percentageError != null,
                        bottomMessage = percentageError,
                    )

                    BisqTextFieldV0(
                        label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        enabled = false,
                        colors =
                            BisqTextFieldColors.default(
                                unfocusedIndicatorColor = BisqTheme.colors.mid_grey10,
                                focusedIndicatorColor = BisqTheme.colors.mid_grey10,
                            ),
                    )
                } else {
                    BisqTextFieldV0(
                        label = presenter.fixPriceDescription,
                        value = formattedPrice,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        onValueChange = onFormattedPriceChange,
                        isError = fixedPriceError != null,
                        bottomMessage = fixedPriceError,
                    )
                    BisqTextFieldV0(
                        label = "bisqEasy.price.percentage.inputBoxText".i18n(),
                        value = formattedPercentagePrice,
                        enabled = false,
                        colors =
                            BisqTextFieldColors.default(
                                unfocusedIndicatorColor = BisqTheme.colors.mid_grey10,
                                focusedIndicatorColor = BisqTheme.colors.mid_grey10,
                            ),
                        suffix = { BisqText.BaseLightGrey("%") },
                    )
                }

                BisqGap.V1()

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    BisqSlider(
                        value = sliderPosition,
                        onValueChange = { onSliderValueChange(it) },
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 6.dp),
                    ) {
                        BisqText.SmallLightGrey("Min: ${min.toInt()}%")
                        BisqText.SmallLightGrey("Max: ${max.toInt()}%")
                    }
                }
            }

            if (isBuy) {
                BisqGap.V1()

                Column(modifier = Modifier.padding(horizontal = 20.dp)) {
                    NoteText(
                        notes = hintText,
                        linkText = "bisqEasy.price.feedback.learnWhySection.openButton".i18n(),
                        textAlign = TextAlign.Center,
                        onLinkClick = {
                            presenter.setShowWhyPopup(true)
                        },
                    )
                }
            }
        }
    }

    if (showWhyPopup) {
        WhyHighPricePopup(onDismiss = { presenter.setShowWhyPopup(false) })
    }
}
