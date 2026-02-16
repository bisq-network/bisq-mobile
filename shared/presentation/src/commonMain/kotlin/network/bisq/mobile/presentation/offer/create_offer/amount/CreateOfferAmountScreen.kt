package network.bisq.mobile.presentation.offer.create_offer.amount

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.NoteText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.WarningIconLightGrey
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.ToggleTab
import network.bisq.mobile.presentation.common.ui.components.molecules.amountSelector.BisqAmountSelector
import network.bisq.mobile.presentation.common.ui.components.molecules.amountSelector.BisqRangeAmountSelector
import network.bisq.mobile.presentation.common.ui.components.organisms.create_offer.ReputationBasedBuyerLimitsPopup
import network.bisq.mobile.presentation.common.ui.components.organisms.create_offer.ReputationBasedSellerLimitsPopup
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.utils.RememberPresenterLifecycle
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferPresenter
import network.bisq.mobile.presentation.offer.create_offer.CreateOfferPresenter.AmountType
import org.jetbrains.compose.ui.tooling.preview.Preview
import org.koin.compose.koinInject

@Composable
fun CreateOfferAmountScreen() {
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
    val formattedQuoteSideFixedAmount by presenter.formattedQuoteSideFixedAmount.collectAsState()
    val formattedBaseSideFixedAmount by presenter.formattedBaseSideFixedAmount.collectAsState()
    val minRangeSliderValue by presenter.minRangeSliderValue.collectAsState()
    val maxRangeSliderValue by presenter.maxRangeSliderValue.collectAsState()
    val formattedQuoteSideMinRangeAmount by presenter.formattedQuoteSideMinRangeAmount.collectAsState()
    val formattedBaseSideMinRangeAmount by presenter.formattedBaseSideMinRangeAmount.collectAsState()
    val formattedQuoteSideMaxRangeAmount by presenter.formattedQuoteSideMaxRangeAmount.collectAsState()
    val formattedBaseSideMaxRangeAmount by presenter.formattedBaseSideMaxRangeAmount.collectAsState()
    val isMinRangeAmountError by presenter.isMinRangeAmountError.collectAsState()
    val isMaxRangeAmountError by presenter.isMaxRangeAmountError.collectAsState()

    CreateOfferAmountContent(
        isBuy = isBuy,
        reputation = reputation,
        hintText = hintText,
        reputationBasedMaxSellAmount = reputationBasedMaxSellAmount,
        showLimitPopup = showLimitPopup,
        shouldShowWarningIcon = shouldShowWarningIcon,
        amountValid = amountValid,
        amountType = amountType,
        fixedAmountSliderPosition = fixedAmountSliderPosition,
        reputationBasedMaxSliderValue = reputationBasedMaxSliderValue,
        formattedQuoteSideFixedAmount = formattedQuoteSideFixedAmount,
        formattedBaseSideFixedAmount = formattedBaseSideFixedAmount,
        minRangeSliderValue = minRangeSliderValue,
        maxRangeSliderValue = maxRangeSliderValue,
        formattedQuoteSideMinRangeAmount = formattedQuoteSideMinRangeAmount,
        formattedBaseSideMinRangeAmount = formattedBaseSideMinRangeAmount,
        formattedQuoteSideMaxRangeAmount = formattedQuoteSideMaxRangeAmount,
        formattedBaseSideMaxRangeAmount = formattedBaseSideMaxRangeAmount,
        isMinRangeAmountError = isMinRangeAmountError,
        isMaxRangeAmountError = isMaxRangeAmountError,
        headline = presenter.headline,
        quoteCurrencyCode = presenter.quoteCurrencyCode,
        formattedMinAmountWithCode = presenter.formattedMinAmountWithCode,
        formattedMaxAmountWithCode = if (!isBuy) reputationBasedMaxSellAmount else presenter.formattedMaxAmountWithCode,
        amountLimitInfoOverlayInfo = presenter.amountLimitInfoOverlayInfo,
        stepIndex = if (createPresenter.skipCurrency) 2 else 3,
        stepsLength = if (createPresenter.skipCurrency) 6 else 7,
        snackbarHostState = presenter.getSnackState(),
        onSelectAmountType = presenter::onSelectAmountType,
        onFixedAmountSliderValueChange = presenter::onFixedAmountSliderValueChange,
        onMinRangeSliderValueChange = presenter::onMinRangeSliderValueChange,
        onMaxRangeSliderValueChange = presenter::onMaxRangeSliderValueChange,
        onFixedAmountTextValueChange = presenter::onFixedAmountTextValueChange,
        onMinAmountTextValueChange = presenter::onMinAmountTextValueChange,
        onMaxAmountTextValueChange = presenter::onMaxAmountTextValueChange,
        onSliderDragFinish = presenter::onSliderDragFinished,
        onBack = presenter::onBack,
        onNext = presenter::onNext,
        onClose = presenter::onClose,
        setShowLimitPopup = presenter::setShowLimitPopup,
        onReputationLinkClick = presenter::navigateToReputation,
        onBuildReputationLinkClick = presenter::navigateToBuildReputation,
    )
}

@Composable
fun CreateOfferAmountContent(
    isBuy: Boolean,
    reputation: Long,
    hintText: String,
    reputationBasedMaxSellAmount: String,
    showLimitPopup: Boolean,
    shouldShowWarningIcon: Boolean,
    amountValid: Boolean,
    amountType: AmountType,
    fixedAmountSliderPosition: Float,
    reputationBasedMaxSliderValue: Float?,
    formattedQuoteSideFixedAmount: String,
    formattedBaseSideFixedAmount: String,
    minRangeSliderValue: Float,
    maxRangeSliderValue: Float,
    formattedQuoteSideMinRangeAmount: String,
    formattedBaseSideMinRangeAmount: String,
    formattedQuoteSideMaxRangeAmount: String,
    formattedBaseSideMaxRangeAmount: String,
    isMinRangeAmountError: Boolean,
    isMaxRangeAmountError: Boolean,
    headline: String,
    quoteCurrencyCode: String,
    formattedMinAmountWithCode: String,
    formattedMaxAmountWithCode: String,
    amountLimitInfoOverlayInfo: StateFlow<String>,
    stepIndex: Int,
    stepsLength: Int,
    snackbarHostState: SnackbarHostState,
    onSelectAmountType: (AmountType) -> Unit,
    onFixedAmountSliderValueChange: (Float) -> Unit,
    onMinRangeSliderValueChange: (Float) -> Unit,
    onMaxRangeSliderValueChange: (Float) -> Unit,
    onFixedAmountTextValueChange: (String) -> Unit,
    onMinAmountTextValueChange: (String) -> Unit,
    onMaxAmountTextValueChange: (String) -> Unit,
    onSliderDragFinish: () -> Unit,
    onBack: () -> Unit,
    onNext: () -> Unit,
    onClose: () -> Unit,
    setShowLimitPopup: (Boolean) -> Unit,
    onReputationLinkClick: () -> Unit,
    onBuildReputationLinkClick: () -> Unit,
) {
    val amountTypes = remember { AmountType.entries.toList() }

    MultiScreenWizardScaffold(
        "bisqEasy.openTrades.table.quoteAmount".i18n(),
        stepIndex = stepIndex,
        stepsLength = stepsLength,
        prevOnClick = onBack,
        nextOnClick = onNext,
        nextDisabled = (!amountValid && amountType == AmountType.FIXED_AMOUNT) || ((isMinRangeAmountError || isMaxRangeAmountError) && amountType == AmountType.RANGE_AMOUNT),
        snackbarHostState = snackbarHostState,
        isInteractive = !showLimitPopup,
        shouldBlurBg = showLimitPopup,
        showUserAvatar = false,
        closeAction = true,
        onConfirmedClose = onClose,
    ) {
        BisqText.H3Light(
            text = headline,
            modifier = Modifier.align(Alignment.Start),
        )

        BisqGap.V2()

        ToggleTab(
            options = amountTypes,
            selectedOption = amountType,
            onOptionSelect = onSelectAmountType,
            getDisplayString = { direction ->
                if (direction == AmountType.FIXED_AMOUNT) {
                    "bisqEasy.tradeWizard.amount.amountModel.fixedAmount".i18n()
                } else {
                    "bisqEasy.tradeWizard.amount.amountModel.rangeAmount".i18n()
                }
            },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        BisqGap.V2()

        if (amountType == AmountType.FIXED_AMOUNT) {
            BisqAmountSelector(
                quoteCurrencyCode = quoteCurrencyCode,
                formattedMinAmount = formattedMinAmountWithCode,
                formattedMaxAmount = formattedMaxAmountWithCode,
                sliderPosition = fixedAmountSliderPosition,
                maxSliderValue = reputationBasedMaxSliderValue ?: 1f,
                formattedFiatAmount = formattedQuoteSideFixedAmount,
                formattedBtcAmount = formattedBaseSideFixedAmount,
                onSliderValueChange = onFixedAmountSliderValueChange,
                onTextValueChange = onFixedAmountTextValueChange,
                onSliderValueChangeFinish = onSliderDragFinish,
                isError = !amountValid,
            )
        } else {
            BisqRangeAmountSelector(
                quoteCurrencyCode = quoteCurrencyCode,
                formattedMinAmount = formattedMinAmountWithCode,
                formattedMaxAmount = formattedMaxAmountWithCode,
                sliderRange = minRangeSliderValue..maxRangeSliderValue,
                onSliderRangeChange = { range ->
                    onMinRangeSliderValueChange(range.start)
                    onMaxRangeSliderValueChange(range.endInclusive)
                },
                formattedQuoteSideMinRangeAmount = formattedQuoteSideMinRangeAmount,
                formattedQuoteSideMaxRangeAmount = formattedQuoteSideMaxRangeAmount,
                formattedBaseSideMinRangeAmount = formattedBaseSideMinRangeAmount,
                formattedBaseSideMaxRangeAmount = formattedBaseSideMaxRangeAmount,
                onMinAmountTextValueChange = onMinAmountTextValueChange,
                onMaxAmountTextValueChange = onMaxAmountTextValueChange,
                sliderValueRange = 0f..(reputationBasedMaxSliderValue ?: 1f),
                onSliderRangeChangeFinish = onSliderDragFinish,
                isMinError = isMinRangeAmountError,
                isMaxError = isMaxRangeAmountError,
            )
        }

        BisqGap.V2()

        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (shouldShowWarningIcon) {
                WarningIconLightGrey(modifier = Modifier.size(18.dp))
            }
            NoteText(
                notes = hintText,
                linkText = "bisqEasy.tradeWizard.amount.buyer.limitInfo.learnMore".i18n(),
                onLinkClick = { setShowLimitPopup(true) },
            )
        }
    }

    if (showLimitPopup) {
        if (isBuy) {
            ReputationBasedBuyerLimitsPopup(
                onDismiss = { setShowLimitPopup(false) },
                onRepLinkClick = onReputationLinkClick,
                amountLimitInfoOverlayInfo = amountLimitInfoOverlayInfo,
            )
        } else {
            ReputationBasedSellerLimitsPopup(
                onDismiss = { setShowLimitPopup(false) },
                reputationScore = reputation.toString(),
                maxSellAmount = reputationBasedMaxSellAmount,
                onRepLinkClick = onReputationLinkClick,
                onBuildRepLinkClick = onBuildReputationLinkClick,
            )
        }
    }
}

@Preview
@Composable
private fun CreateOfferAmountScreen_FixedAmount_Buyer_Preview() {
    BisqTheme.Preview {
        CreateOfferAmountContent(
            isBuy = true,
            reputation = 5000,
            hintText = "Your maximum selling amount is 250 USD.",
            reputationBasedMaxSellAmount = "",
            showLimitPopup = false,
            shouldShowWarningIcon = false,
            amountValid = true,
            amountType = AmountType.FIXED_AMOUNT,
            fixedAmountSliderPosition = 0.5f,
            reputationBasedMaxSliderValue = null,
            formattedQuoteSideFixedAmount = "500",
            formattedBaseSideFixedAmount = "0.0050",
            minRangeSliderValue = 0.1f,
            maxRangeSliderValue = 0.9f,
            formattedQuoteSideMinRangeAmount = "100",
            formattedBaseSideMinRangeAmount = "0.0010",
            formattedQuoteSideMaxRangeAmount = "900",
            formattedBaseSideMaxRangeAmount = "0.0090",
            isMinRangeAmountError = false,
            isMaxRangeAmountError = false,
            headline = "How much do you want to buy?",
            quoteCurrencyCode = "USD",
            formattedMinAmountWithCode = "6 USD",
            formattedMaxAmountWithCode = "600 USD",
            amountLimitInfoOverlayInfo = remember { MutableStateFlow("To trade 500 USD, the seller needs 5000 reputation points.\n\nThere are 3 sellers with sufficient reputation who could take your offer.") },
            stepIndex = 3,
            stepsLength = 7,
            snackbarHostState = remember { SnackbarHostState() },
            onSelectAmountType = {},
            onFixedAmountSliderValueChange = {},
            onMinRangeSliderValueChange = {},
            onMaxRangeSliderValueChange = {},
            onFixedAmountTextValueChange = {},
            onMinAmountTextValueChange = {},
            onMaxAmountTextValueChange = {},
            onSliderDragFinish = {},
            onBack = {},
            onNext = {},
            onClose = {},
            setShowLimitPopup = {},
            onReputationLinkClick = {},
            onBuildReputationLinkClick = {},
        )
    }
}

@Preview
@Composable
private fun CreateOfferAmountScreen_RangeAmount_Seller_Preview() {
    BisqTheme.Preview {
        CreateOfferAmountContent(
            isBuy = false,
            reputation = 30000,
            hintText = "Maximum trade amount: 6000 USD",
            reputationBasedMaxSellAmount = "6000 USD",
            showLimitPopup = false,
            shouldShowWarningIcon = false,
            amountValid = true,
            amountType = AmountType.RANGE_AMOUNT,
            fixedAmountSliderPosition = 0.5f,
            reputationBasedMaxSliderValue = 0.95f,
            formattedQuoteSideFixedAmount = "500",
            formattedBaseSideFixedAmount = "0.0050",
            minRangeSliderValue = 0.2f,
            maxRangeSliderValue = 0.7f,
            formattedQuoteSideMinRangeAmount = "1200",
            formattedBaseSideMinRangeAmount = "0.0120",
            formattedQuoteSideMaxRangeAmount = "4200",
            formattedBaseSideMaxRangeAmount = "0.0420",
            isMinRangeAmountError = false,
            isMaxRangeAmountError = false,
            headline = "How much do you want to sell?",
            quoteCurrencyCode = "USD",
            formattedMinAmountWithCode = "6 USD",
            formattedMaxAmountWithCode = "6000 USD",
            amountLimitInfoOverlayInfo = remember { MutableStateFlow("") },
            stepIndex = 3,
            stepsLength = 7,
            snackbarHostState = remember { SnackbarHostState() },
            onSelectAmountType = {},
            onFixedAmountSliderValueChange = {},
            onMinRangeSliderValueChange = {},
            onMaxRangeSliderValueChange = {},
            onFixedAmountTextValueChange = {},
            onMinAmountTextValueChange = {},
            onMaxAmountTextValueChange = {},
            onSliderDragFinish = {},
            onBack = {},
            onNext = {},
            onClose = {},
            setShowLimitPopup = {},
            onReputationLinkClick = {},
            onBuildReputationLinkClick = {},
        )
    }
}

@Preview
@Composable
private fun CreateOfferAmountScreen_WithWarningIcon_Preview() {
    BisqTheme.Preview {
        CreateOfferAmountContent(
            isBuy = true,
            reputation = 50000,
            hintText = "No sellers found for this amount. Increase your range or wait for sellers to gain reputation.",
            reputationBasedMaxSellAmount = "",
            showLimitPopup = false,
            shouldShowWarningIcon = true,
            amountValid = true,
            amountType = AmountType.FIXED_AMOUNT,
            fixedAmountSliderPosition = 0.9f,
            reputationBasedMaxSliderValue = null,
            formattedQuoteSideFixedAmount = "5400",
            formattedBaseSideFixedAmount = "0.0540",
            minRangeSliderValue = 0.1f,
            maxRangeSliderValue = 0.9f,
            formattedQuoteSideMinRangeAmount = "600",
            formattedBaseSideMinRangeAmount = "0.0060",
            formattedQuoteSideMaxRangeAmount = "5400",
            formattedBaseSideMaxRangeAmount = "0.0540",
            isMinRangeAmountError = false,
            isMaxRangeAmountError = false,
            headline = "How much do you want to buy?",
            quoteCurrencyCode = "USD",
            formattedMinAmountWithCode = "6 USD",
            formattedMaxAmountWithCode = "6000 USD",
            amountLimitInfoOverlayInfo = remember { MutableStateFlow("To trade 5400 USD, the seller needs 50000 reputation points.\n\nNo sellers currently have sufficient reputation.") },
            stepIndex = 3,
            stepsLength = 7,
            snackbarHostState = remember { SnackbarHostState() },
            onSelectAmountType = {},
            onFixedAmountSliderValueChange = {},
            onMinRangeSliderValueChange = {},
            onMaxRangeSliderValueChange = {},
            onFixedAmountTextValueChange = {},
            onMinAmountTextValueChange = {},
            onMaxAmountTextValueChange = {},
            onSliderDragFinish = {},
            onBack = {},
            onNext = {},
            onClose = {},
            setShowLimitPopup = {},
            onReputationLinkClick = {},
            onBuildReputationLinkClick = {},
        )
    }
}

@Preview
@Composable
private fun CreateOfferAmountScreen_WithLimitPopup_Buyer_Preview() {
    BisqTheme.Preview {
        CreateOfferAmountContent(
            isBuy = true,
            reputation = 5000,
            hintText = "Up to 3 sellers can take offers with this amount.",
            reputationBasedMaxSellAmount = "",
            showLimitPopup = true,
            shouldShowWarningIcon = false,
            amountValid = true,
            amountType = AmountType.FIXED_AMOUNT,
            fixedAmountSliderPosition = 0.5f,
            reputationBasedMaxSliderValue = null,
            formattedQuoteSideFixedAmount = "500",
            formattedBaseSideFixedAmount = "0.0050",
            minRangeSliderValue = 0.1f,
            maxRangeSliderValue = 0.9f,
            formattedQuoteSideMinRangeAmount = "100",
            formattedBaseSideMinRangeAmount = "0.0010",
            formattedQuoteSideMaxRangeAmount = "900",
            formattedBaseSideMaxRangeAmount = "0.0090",
            isMinRangeAmountError = false,
            isMaxRangeAmountError = false,
            headline = "How much do you want to buy?",
            quoteCurrencyCode = "USD",
            formattedMinAmountWithCode = "6 USD",
            formattedMaxAmountWithCode = "600 USD",
            amountLimitInfoOverlayInfo = remember { MutableStateFlow("To trade 500 USD, the seller needs 5000 reputation points.\n\nThere are 3 sellers with sufficient reputation who could take your offer.") },
            stepIndex = 3,
            stepsLength = 7,
            snackbarHostState = remember { SnackbarHostState() },
            onSelectAmountType = {},
            onFixedAmountSliderValueChange = {},
            onMinRangeSliderValueChange = {},
            onMaxRangeSliderValueChange = {},
            onFixedAmountTextValueChange = {},
            onMinAmountTextValueChange = {},
            onMaxAmountTextValueChange = {},
            onSliderDragFinish = {},
            onBack = {},
            onNext = {},
            onClose = {},
            setShowLimitPopup = {},
            onReputationLinkClick = {},
            onBuildReputationLinkClick = {},
        )
    }
}

@Preview
@Composable
private fun CreateOfferAmountScreen_WithLimitPopup_Seller_Preview() {
    BisqTheme.Preview {
        CreateOfferAmountContent(
            isBuy = false,
            reputation = 30000,
            hintText = "Maximum trade amount: 6000 USD",
            reputationBasedMaxSellAmount = "6000 USD",
            showLimitPopup = true,
            shouldShowWarningIcon = false,
            amountValid = true,
            amountType = AmountType.RANGE_AMOUNT,
            fixedAmountSliderPosition = 0.5f,
            reputationBasedMaxSliderValue = 0.95f,
            formattedQuoteSideFixedAmount = "500",
            formattedBaseSideFixedAmount = "0.0050",
            minRangeSliderValue = 0.2f,
            maxRangeSliderValue = 0.7f,
            formattedQuoteSideMinRangeAmount = "1200",
            formattedBaseSideMinRangeAmount = "0.0120",
            formattedQuoteSideMaxRangeAmount = "4200",
            formattedBaseSideMaxRangeAmount = "0.0420",
            isMinRangeAmountError = false,
            isMaxRangeAmountError = false,
            headline = "How much do you want to sell?",
            quoteCurrencyCode = "USD",
            formattedMinAmountWithCode = "6 USD",
            formattedMaxAmountWithCode = "6000 USD",
            amountLimitInfoOverlayInfo = remember { MutableStateFlow("") },
            stepIndex = 3,
            stepsLength = 7,
            snackbarHostState = remember { SnackbarHostState() },
            onSelectAmountType = {},
            onFixedAmountSliderValueChange = {},
            onMinRangeSliderValueChange = {},
            onMaxRangeSliderValueChange = {},
            onFixedAmountTextValueChange = {},
            onMinAmountTextValueChange = {},
            onMaxAmountTextValueChange = {},
            onSliderDragFinish = {},
            onBack = {},
            onNext = {},
            onClose = {},
            setShowLimitPopup = {},
            onReputationLinkClick = {},
            onBuildReputationLinkClick = {},
        )
    }
}

@Preview
@Composable
private fun CreateOfferAmountScreen_InvalidAmount_Preview() {
    BisqTheme.Preview {
        CreateOfferAmountContent(
            isBuy = true,
            reputation = 5000,
            hintText = "Amount exceeds maximum limit.",
            reputationBasedMaxSellAmount = "",
            showLimitPopup = false,
            shouldShowWarningIcon = true,
            amountValid = false,
            amountType = AmountType.FIXED_AMOUNT,
            fixedAmountSliderPosition = 1.2f,
            reputationBasedMaxSliderValue = null,
            formattedQuoteSideFixedAmount = "9999",
            formattedBaseSideFixedAmount = "0.0999",
            minRangeSliderValue = 0.1f,
            maxRangeSliderValue = 0.9f,
            formattedQuoteSideMinRangeAmount = "100",
            formattedBaseSideMinRangeAmount = "0.0010",
            formattedQuoteSideMaxRangeAmount = "900",
            formattedBaseSideMaxRangeAmount = "0.0090",
            isMinRangeAmountError = false,
            isMaxRangeAmountError = false,
            headline = "How much do you want to buy?",
            quoteCurrencyCode = "USD",
            formattedMinAmountWithCode = "6 USD",
            formattedMaxAmountWithCode = "600 USD",
            amountLimitInfoOverlayInfo = remember { MutableStateFlow("") },
            stepIndex = 3,
            stepsLength = 7,
            snackbarHostState = remember { SnackbarHostState() },
            onSelectAmountType = {},
            onFixedAmountSliderValueChange = {},
            onMinRangeSliderValueChange = {},
            onMaxRangeSliderValueChange = {},
            onFixedAmountTextValueChange = {},
            onMinAmountTextValueChange = {},
            onMaxAmountTextValueChange = {},
            onSliderDragFinish = {},
            onBack = {},
            onNext = {},
            onClose = {},
            setShowLimitPopup = {},
            onReputationLinkClick = {},
            onBuildReputationLinkClick = {},
        )
    }
}
