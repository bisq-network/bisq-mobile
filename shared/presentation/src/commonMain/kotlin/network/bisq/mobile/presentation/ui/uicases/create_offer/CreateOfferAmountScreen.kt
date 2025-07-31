package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.button.BisqIconButton
import network.bisq.mobile.presentation.ui.components.atoms.icons.CloseIcon
import network.bisq.mobile.presentation.ui.components.atoms.icons.WarningIconLightGrey
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
import network.bisq.mobile.presentation.ui.components.molecules.RangeAmountSelector
import network.bisq.mobile.presentation.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.ui.components.organisms.create_offer.ReputationBasedBuyerLimitsPopup
import network.bisq.mobile.presentation.ui.components.organisms.create_offer.ReputationBasedSellerLimitsPopup
import network.bisq.mobile.presentation.ui.helpers.RememberPresenterLifecycle
import network.bisq.mobile.presentation.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter.AmountType
import org.koin.compose.koinInject

@Composable
fun CreateOfferAmountSelectorScreen() {
    val presenter: CreateOfferAmountPresenter = koinInject()
    RememberPresenterLifecycle(presenter)

    val isBuy by presenter.isBuy.collectAsState()
    val reputation by presenter.requiredReputation.collectAsState()
    val hintText by presenter.amountLimitInfo.collectAsState()
    val reputationBasedMaxSellAmount by presenter.formattedReputationBasedMaxAmount.collectAsState()
    val showLimitPopup by presenter.showLimitPopup.collectAsState()
    val shouldShowWarningIcon by presenter.shouldShowWarningIcon.collectAsState()

    MultiScreenWizardScaffold(
        "bisqEasy.openTrades.table.quoteAmount".i18n(),
        stepIndex = 3,
        stepsLength = 7,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() },
        nextDisabled = !presenter.amountValid.collectAsState().value,
        snackbarHostState = presenter.getSnackState(),
        shouldBlurBg = showLimitPopup,
        showUserAvatar = false,
        extraActions = {
            BisqIconButton(onClick = {
                presenter.onClose()
            }, size = BisqUIConstants.topBarAvatarSize){
                CloseIcon()
            }
        },
    ) {

        BisqText.h3Regular(
            text = presenter.headline,
            modifier = Modifier.align(Alignment.Start)
        )

        BisqGap.V2()

        ToggleTab(
            options = AmountType.entries,
            initialOption = presenter.amountType.value,
            onStateChange = { value ->
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

        if (presenter.amountType.collectAsState().value == AmountType.FIXED_AMOUNT) {
            BisqAmountSelector(
                quoteCurrencyCode = presenter.quoteCurrencyCode,
                formattedMinAmount = presenter.formattedMinAmountWithCode,
                formattedMaxAmount = presenter.formattedMaxAmountWithCode,
                sliderPosition = presenter.fixedAmountSliderPosition.collectAsState().value,
                maxSliderValue = presenter.reputationBasedMaxSliderValue.collectAsState().value,
                rightMarkerSliderValue = presenter.rightMarkerSliderValue.collectAsState().value,
                formattedFiatAmount = presenter.formattedQuoteSideFixedAmount.collectAsState().value,
                formattedBtcAmount = presenter.formattedBaseSideFixedAmount.collectAsState().value,
                onSliderValueChange = { presenter.onFixedAmountSliderValueChange(it) },
                onTextValueChange = { presenter.onFixedAmountTextValueChange(it) },
                validateTextField = { presenter.validateTextField(it) },
            )
        } else {
            RangeAmountSelector(
                formattedMinAmount = presenter.formattedMinAmountWithCode,
                formattedMaxAmount = presenter.formattedMaxAmountWithCode,
                quoteCurrencyCode = presenter.quoteCurrencyCode,
                minRangeSliderValue = presenter.minRangeSliderValue.collectAsState().value,
                onMinRangeSliderValueChange = { presenter.onMinRangeSliderValueChange(it) },
                maxRangeSliderValue = presenter.maxRangeSliderValue.collectAsState().value,
                onMaxRangeSliderValueChange = { presenter.onMaxRangeSliderValueChange(it) },
                maxSliderValue = presenter.reputationBasedMaxSliderValue.collectAsState().value,
                rightMarkerSliderValue = presenter.rightMarkerSliderValue.collectAsState().value,
                formattedQuoteSideMinRangeAmount = presenter.formattedQuoteSideMinRangeAmount.collectAsState().value,
                formattedBaseSideMinRangeAmount = presenter.formattedBaseSideMinRangeAmount.collectAsState().value,
                formattedQuoteSideMaxRangeAmount = presenter.formattedQuoteSideMaxRangeAmount.collectAsState().value,
                formattedBaseSideMaxRangeAmount = presenter.formattedBaseSideMaxRangeAmount.collectAsState().value,
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
                linkText = "bisqEasy.tradeWizard.amount.buyer.limitInfo.more".i18n(),
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
                onBuildRepLinkClick = presenter::navigateToBuildReputation
            )
        }
    }
}
