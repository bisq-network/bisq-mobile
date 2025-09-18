package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIconLightGrey
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.rememberConfirmCloseState
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.components.molecules.ConfirmCloseAction
import network.bisq.mobile.presentation.ui.components.molecules.ConfirmCloseOverlay
import network.bisq.mobile.presentation.ui.components.molecules.RangeAmountSelector
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.components.organisms.create_offer.ReputationBasedBuyerLimitsPopup
import network.bisq.mobile.presentation.ui.components.organisms.create_offer.ReputationBasedSellerLimitsPopup
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter.AmountType
import org.koin.compose.koinInject

@Composable
fun CreateOfferAmountSelectorScreen() {
    val presenter: CreateOfferAmountPresenter = koinInject()
    val createPresenter: CreateOfferPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isBuy by presenter.isBuy.collectAsState()
    val reputation by presenter.requiredReputation.collectAsState()
    val hintText by presenter.amountLimitInfo.collectAsState()
    val reputationBasedMaxSellAmount by presenter.formattedReputationBasedMaxAmount.collectAsState()
    val showLimitPopup by presenter.showLimitPopup.collectAsState()
    val shouldShowWarningIcon by presenter.shouldShowWarningIcon.collectAsState()
    val amountValid by presenter.amountValid.collectAsState()
    val amountType by presenter.amountType.collectAsState()
    val fixedAmountSliderPosition by presenter.fixedAmountSliderPosition.collectAsState()
    val reputationBasedMaxSliderValue by presenter.reputationBasedMaxSliderValue.collectAsState()
    val rightMarkerSliderValue by presenter.rightMarkerSliderValue.collectAsState()
    val formattedQuoteSideFixedAmount by presenter.formattedQuoteSideFixedAmount.collectAsState()
    val formattedBaseSideFixedAmount by presenter.formattedBaseSideFixedAmount.collectAsState()
    val minRangeSliderValue by presenter.minRangeSliderValue.collectAsState()
    val maxRangeSliderValue by presenter.maxRangeSliderValue.collectAsState()
    val formattedQuoteSideMinRangeAmount by presenter.formattedQuoteSideMinRangeAmount.collectAsState()
    val formattedBaseSideMinRangeAmount by presenter.formattedBaseSideMinRangeAmount.collectAsState()
    val formattedQuoteSideMaxRangeAmount by presenter.formattedQuoteSideMaxRangeAmount.collectAsState()
    val formattedBaseSideMaxRangeAmount by presenter.formattedBaseSideMaxRangeAmount.collectAsState()

    MultiScreenWizardScaffold(
        "bisqEasy.openTrades.table.quoteAmount".i18n(),
        stepIndex = if (createPresenter.skipCurrency) 2 else 3,
        stepsLength = if (createPresenter.skipCurrency) 6 else 7,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        nextDisabled = !amountValid,
        snackbarHostState = presenter.getSnackState(),
        isInteractive = !showLimitPopup,
        shouldBlurBg = showLimitPopup,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = presenter::onClose
    ) {

        BisqText.h3Light(
            text = presenter.headline,
            modifier = Modifier.align(Alignment.Start)
        )

        BisqGap.V2()

        ToggleTab(
            options = presenter.amountTypes,
            selectedOption = amountType,
            onOptionSelected = { value ->
                presenter.onSelectAmountType(value)
            },
            getDisplayString = { direction ->
                if (direction == AmountType.FIXED_AMOUNT)
                    "bisqEasy.tradeWizard.amount.amountModel.fixedAmount".i18n()
                else
                    "bisqEasy.tradeWizard.amount.amountModel.rangeAmount".i18n()
            },
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )

        BisqGap.V2()

        if (amountType == AmountType.FIXED_AMOUNT) {
            BisqAmountSelector(
                quoteCurrencyCode = presenter.quoteCurrencyCode,
                formattedMinAmount = presenter.formattedMinAmountWithCode,
                formattedMaxAmount = presenter.formattedMaxAmountWithCode,
                sliderPosition = fixedAmountSliderPosition,
                maxSliderValue = reputationBasedMaxSliderValue,
                rightMarkerSliderValue = rightMarkerSliderValue,
                formattedFiatAmount = formattedQuoteSideFixedAmount,
                formattedBtcAmount = formattedBaseSideFixedAmount,
                onSliderValueChange = { presenter.onFixedAmountSliderValueChange(it) },
                onTextValueChange = { presenter.onFixedAmountTextValueChange(it) },
                validateTextField = { presenter.validateTextField(it) },
            )
        } else {
            RangeAmountSelector(
                formattedMinAmount = presenter.formattedMinAmountWithCode,
                formattedMaxAmount = presenter.formattedMaxAmountWithCode,
                quoteCurrencyCode = presenter.quoteCurrencyCode,
                minRangeSliderValue = minRangeSliderValue,
                onMinRangeSliderValueChange = { presenter.onMinRangeSliderValueChange(it) },
                maxRangeSliderValue = maxRangeSliderValue,
                onMaxRangeSliderValueChange = { presenter.onMaxRangeSliderValueChange(it) },
                maxSliderValue = reputationBasedMaxSliderValue,
                rightMarkerSliderValue = rightMarkerSliderValue,
                formattedQuoteSideMinRangeAmount = formattedQuoteSideMinRangeAmount,
                formattedBaseSideMinRangeAmount = formattedBaseSideMinRangeAmount,
                formattedQuoteSideMaxRangeAmount = formattedQuoteSideMaxRangeAmount,
                formattedBaseSideMaxRangeAmount = formattedBaseSideMaxRangeAmount,
                onMinAmountTextValueChange = { presenter.onMinAmountTextValueChange(it) },
                onMaxAmountTextValueChange = { presenter.onMaxAmountTextValueChange(it) },
                validateRangeMinTextField = { presenter.validateTextField(it) },
                validateRangeMaxTextField = { presenter.validateTextField(it) },
            )
        }

        BisqGap.V2()

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (shouldShowWarningIcon) {
                WarningIconLightGrey(modifier = Modifier.size(18.dp))
            }
            NoteText(
                notes = hintText,
                linkText = "bisqEasy.tradeWizard.amount.buyer.limitInfo.learnMore".i18n(),
                onLinkClick = { presenter.setShowLimitPopup(true) }
            )
        }
    }

    if (showLimitPopup) {
        if (isBuy) {
            ReputationBasedBuyerLimitsPopup(
                onDismiss = { presenter.setShowLimitPopup(false) },
                onRepLinkClick = presenter::navigateToReputation,
                amountLimitInfoOverlayInfo = presenter.amountLimitInfoOverlayInfo
            )
        } else {
            ReputationBasedSellerLimitsPopup(
                onDismiss = { presenter.setShowLimitPopup(false) },
                reputationScore = reputation.toString(),
                maxSellAmount = reputationBasedMaxSellAmount,
                onRepLinkClick =  presenter::navigateToReputation,
                onBuildRepLinkClick = presenter::navigateToBuildReputation
            )
        }
    }
}
