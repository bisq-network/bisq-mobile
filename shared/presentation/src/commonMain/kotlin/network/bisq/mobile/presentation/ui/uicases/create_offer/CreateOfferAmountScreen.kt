package network.bisq.mobile.presentation.ui.uicases.create_offer

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.ui.components.molecules.BisqAmountSelector
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
    val isBuy by presenter.isBuy.collectAsState()
    val reputation by presenter.reputation.collectAsState()
    val hintText by presenter.hintText.collectAsState()
    val reputationBasedMaxSellAmount by presenter.formattedReputationBasedMaxSellAmount.collectAsState()
    val showLimitPopup by presenter.showLimitPopup.collectAsState()
    val takersCount by presenter.takersCount.collectAsState()
    RememberPresenterLifecycle(presenter)

    MultiScreenWizardScaffold(
        "bisqEasy.openTrades.table.quoteAmount".i18n(),
        stepIndex = 3,
        stepsLength = 6,
        prevOnClick = { presenter.onBack() },
        nextOnClick = { presenter.onNext() }
    ) {

        BisqText.h3Regular(
            text = presenter.headline,
            textAlign = TextAlign.Center,
            modifier = Modifier.align(Alignment.CenterHorizontally)
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
                presenter.quoteCurrencyCode,
                presenter.formattedMinAmountWithCode,
                presenter.formattedMaxAmountWithCode,
                presenter.fixedAmountSliderPosition,
                presenter.formattedQuoteSideFixedAmount,
                presenter.formattedBaseSideFixedAmount,
                { presenter.onFixedAmountSliderChanged(it) },
                { presenter.onFixedAmountTextValueChanged(it) }
            )
        } else {
            RangeAmountSelector(
                presenter.formattedMinAmountWithCode,
                presenter.formattedMaxAmountWithCode,
                presenter.quoteCurrencyCode,
                presenter.rangeSliderPosition,
                presenter.formattedQuoteSideMinRangeAmount,
                presenter.formattedBaseSideMinRangeAmount,
                presenter.formattedQuoteSideMaxRangeAmount,
                presenter.formattedBaseSideMaxRangeAmount,
                { presenter.onRangeAmountSliderChanged(it) },
                { presenter.onMinAmountTextValueChanged(it) },
                { presenter.onMaxAmountTextValueChanged(it) }
            )
        }

        BisqGap.V2()

        NoteText(
            notes = hintText,
            linkText = "bisqEasy.tradeWizard.amount.buyer.limitInfo.learnMore".i18n(),
            onLinkClick = { presenter.setShowLimitPopup(true) }
        )

    }

    if (showLimitPopup) {
        if (isBuy) {
            ReputationBasedBuyerLimitsPopup(
                onDismiss = { presenter.setShowLimitPopup(false) },
                reputationScore = reputation.toString(),
                maxBuyAmount = presenter.maxBuyAmount.collectAsState().value,
                takersCount = takersCount,
                onRepLinkClick = presenter::navigateToReputation
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