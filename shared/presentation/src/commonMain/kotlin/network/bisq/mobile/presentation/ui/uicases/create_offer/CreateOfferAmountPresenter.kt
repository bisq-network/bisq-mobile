package network.bisq.mobile.presentation.ui.uicases.create_offer

import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import network.bisq.mobile.domain.data.IODispatcher
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.faceValueToLong
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVOFactory.from
import network.bisq.mobile.domain.data.replicated.common.monetary.MonetaryVOExtensions.asDouble
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVOExtensions.toBaseSideMonetary
import network.bisq.mobile.domain.data.replicated.offer.DirectionEnumExtensions.isBuy
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVOExtension.id
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.formatters.AmountFormatter
import network.bisq.mobile.domain.getGroupingSeparator
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.service.reputation.ReputationServiceFacade
import network.bisq.mobile.domain.service.user_profile.UserProfileServiceFacade
import network.bisq.mobile.domain.toDoubleOrNullLocaleAware
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.DEFAULT_MIN_USD_TRADE_AMOUNT
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.MAX_USD_TRADE_AMOUNT
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.findRequiredReputationScoreByFiatAmount
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.getReputationBasedQuoteSideAmount
import network.bisq.mobile.domain.utils.BisqEasyTradeAmountLimits.withTolerance
import network.bisq.mobile.domain.utils.MonetarySlider
import network.bisq.mobile.i18n.i18n
import network.bisq.mobile.i18n.i18nPlural
import network.bisq.mobile.presentation.BasePresenter
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.BisqLinks
import network.bisq.mobile.presentation.ui.helpers.AmountValidator
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.uicases.create_offer.CreateOfferPresenter.AmountType

// TODO Create/Take offer amount preseenters are very similar a base class could be extracted
class CreateOfferAmountPresenter(
    mainPresenter: MainPresenter,
    private val marketPriceServiceFacade: MarketPriceServiceFacade,
    private val createOfferPresenter: CreateOfferPresenter,
    private val userProfileServiceFacade: UserProfileServiceFacade,
    private val reputationServiceFacade: ReputationServiceFacade,
) : BasePresenter(mainPresenter) {

    lateinit var headline: String
    lateinit var quoteCurrencyCode: String
    lateinit var formattedMinAmount: String

    private val _amountType: MutableStateFlow<AmountType> = MutableStateFlow(AmountType.FIXED_AMOUNT)
    val amountType: StateFlow<AmountType> get() = _amountType.asStateFlow()
    val amountTypes = AmountType.entries.toList()

    // FIXED_AMOUNT
    private val _fixedAmountSliderPosition: MutableStateFlow<Float> = MutableStateFlow(0.5f)
    val fixedAmountSliderPosition: StateFlow<Float> get() = _fixedAmountSliderPosition.asStateFlow()

    private val _reputationBasedMaxSliderValue: MutableStateFlow<Float?> = MutableStateFlow(null)
    val reputationBasedMaxSliderValue: StateFlow<Float?> get() = _reputationBasedMaxSliderValue.asStateFlow()

    private val _rightMarkerValue: MutableStateFlow<Float?> = MutableStateFlow(null)
    val rightMarkerSliderValue: StateFlow<Float?> get() = _rightMarkerValue.asStateFlow()

    var formattedMinAmountWithCode: String = ""
    var formattedMaxAmountWithCode: String = ""
    private val _formattedQuoteSideFixedAmount = MutableStateFlow("")
    val formattedQuoteSideFixedAmount: StateFlow<String> get() = _formattedQuoteSideFixedAmount.asStateFlow()
    private val _formattedBaseSideFixedAmount = MutableStateFlow("")
    val formattedBaseSideFixedAmount: StateFlow<String> get() = _formattedBaseSideFixedAmount.asStateFlow()

    // RANGE_AMOUNT
    private val _minRangeSliderValue: MutableStateFlow<Float> = MutableStateFlow(0.1f)
    val minRangeSliderValue: StateFlow<Float> get() = _minRangeSliderValue.asStateFlow()
    private val _maxRangeSliderValue: MutableStateFlow<Float> = MutableStateFlow(0.9f)
    val maxRangeSliderValue: StateFlow<Float> get() = _maxRangeSliderValue.asStateFlow()
    private var rangeSliderPosition: ClosedFloatingPointRange<Float> = 0.0f..1.0f
    private val _formattedQuoteSideMinRangeAmount = MutableStateFlow("")
    val formattedQuoteSideMinRangeAmount: StateFlow<String> get() = _formattedQuoteSideMinRangeAmount.asStateFlow()
    private val _formattedBaseSideMinRangeAmount = MutableStateFlow("")
    val formattedBaseSideMinRangeAmount: StateFlow<String> get() = _formattedBaseSideMinRangeAmount.asStateFlow()

    private val _formattedQuoteSideMaxRangeAmount = MutableStateFlow("")
    val formattedQuoteSideMaxRangeAmount: StateFlow<String> get() = _formattedQuoteSideMaxRangeAmount.asStateFlow()
    private val _formattedBaseSideMaxRangeAmount = MutableStateFlow("")
    val formattedBaseSideMaxRangeAmount: StateFlow<String> get() = _formattedBaseSideMaxRangeAmount.asStateFlow()
    private val _requiredReputation = MutableStateFlow<Long>(0L)
    val requiredReputation: StateFlow<Long> get() = _requiredReputation.asStateFlow()

    private val _amountLimitInfo = MutableStateFlow("")
    val amountLimitInfo: StateFlow<String> get() = _amountLimitInfo.asStateFlow()

    private val _amountLimitInfoOverlayInfo = MutableStateFlow("")
    val amountLimitInfoOverlayInfo: StateFlow<String> get() = _amountLimitInfoOverlayInfo.asStateFlow()

    private val _shouldShowWarningIcon = MutableStateFlow(false)
    val shouldShowWarningIcon: StateFlow<Boolean> get() = _shouldShowWarningIcon.asStateFlow()

    private lateinit var createOfferModel: CreateOfferPresenter.CreateOfferModel
    private var minAmount: Long = DEFAULT_MIN_USD_TRADE_AMOUNT.value
    private var maxAmount: Long = MAX_USD_TRADE_AMOUNT.value
    private lateinit var priceQuote: PriceQuoteVO
    private lateinit var quoteSideFixedAmount: FiatVO
    private lateinit var baseSideFixedAmount: CoinVO
    private lateinit var quoteSideMinRangeAmount: FiatVO
    private lateinit var baseSideMinRangeAmount: CoinVO
    private lateinit var quoteSideMaxRangeAmount: FiatVO
    private lateinit var baseSideMaxRangeAmount: CoinVO
    private val _isBuy: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val isBuy: StateFlow<Boolean> get() = _isBuy.asStateFlow()

    // Sample heavy-ish updates during drags to reduce allocation churn on main thread.
    // 32ms ~ 30 FPS. We do a leading-edge immediate update, then coalesce subsequent updates
    // within the window into a single trailing-edge update.
    private var fixedDragJob: Job? = null
    private var rangeMinDragJob: Job? = null
    private var rangeMaxDragJob: Job? = null
    private val dragUpdateSampleMs: Long = 32
    private var latestFixedPending: Float? = null
    private var latestRangeMinPending: Float? = null
    private var latestRangeMaxPending: Float? = null

    private val _formattedReputationBasedMaxAmount: MutableStateFlow<String> = MutableStateFlow("")
    val formattedReputationBasedMaxAmount: StateFlow<String> get() = _formattedReputationBasedMaxAmount.asStateFlow()

    private val _showLimitPopup: MutableStateFlow<Boolean> = MutableStateFlow(false)
    val showLimitPopup: StateFlow<Boolean> get() = _showLimitPopup.asStateFlow()
    fun setShowLimitPopup(newValue: Boolean) {
        _showLimitPopup.value = newValue
    }

    private val _amountValid: MutableStateFlow<Boolean> = MutableStateFlow(true)
    val amountValid: StateFlow<Boolean> get() = _amountValid.asStateFlow()

    // Life cycle
    init {
        createOfferModel = createOfferPresenter.createOfferModel
        quoteCurrencyCode = createOfferModel.market?.quoteCurrencyCode
            ?: throw IllegalStateException("Market must be initialized before creating amount presenter")

        _amountType.value = createOfferModel.amountType

        headline = if (createOfferModel.direction.isBuy)
            "bisqEasy.tradeWizard.amount.headline.buyer".i18n()
        else
            "bisqEasy.tradeWizard.amount.headline.seller".i18n()

        minAmount = BisqEasyTradeAmountLimits.getMinAmountValue(marketPriceServiceFacade, quoteCurrencyCode)
        maxAmount = BisqEasyTradeAmountLimits.getMaxAmountValue(marketPriceServiceFacade, quoteCurrencyCode)

        formattedMinAmount = AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode))
        formattedMinAmountWithCode =
            AmountFormatter.formatAmount(FiatVOFactory.from(minAmount, quoteCurrencyCode), true, true)
        formattedMaxAmountWithCode =
            AmountFormatter.formatAmount(FiatVOFactory.from(maxAmount, quoteCurrencyCode), true, true)

        var valueInFraction = if (createOfferModel.quoteSideFixedAmount != null) {
            quoteSideFixedAmount = createOfferModel.quoteSideFixedAmount!!
            MonetarySlider.minorToFraction(quoteSideFixedAmount.value, minAmount, maxAmount)
        } else {
            createOfferModel.fixedAmountSliderPosition
        }
        _fixedAmountSliderPosition.value = valueInFraction
        applyFixedAmountSliderValue(fixedAmountSliderPosition.value)

        rangeSliderPosition = createOfferModel.rangeSliderPosition
        applyRangeAmountSliderValue(rangeSliderPosition)
        _maxRangeSliderValue.value = rangeSliderPosition.endInclusive

        valueInFraction = if (createOfferModel.quoteSideMinRangeAmount != null) {
            quoteSideMinRangeAmount = createOfferModel.quoteSideMinRangeAmount!!
            MonetarySlider.minorToFraction(quoteSideMinRangeAmount.value, minAmount, maxAmount)
        } else {
            createOfferModel.rangeSliderPosition.start
        }
        _minRangeSliderValue.value = valueInFraction
        valueInFraction = if (createOfferModel.quoteSideMaxRangeAmount != null) {
            quoteSideMaxRangeAmount = createOfferModel.quoteSideMaxRangeAmount!!
            MonetarySlider.minorToFraction(quoteSideMaxRangeAmount.value, minAmount, maxAmount)
        } else {
            createOfferModel.rangeSliderPosition.endInclusive
        }
        _maxRangeSliderValue.value = valueInFraction
        applyRangeAmountSliderValue(_minRangeSliderValue.value.._maxRangeSliderValue.value)

        _isBuy.value = createOfferModel.direction.isBuy

        updateAmountLimitInfo(true)
    }

    // Handlers
    fun onSelectAmountType(value: AmountType) {
        _amountType.value = value
        updateAmountLimitInfo()
    }

    fun onFixedAmountTextValueChange(textInput: String) {
        val separator = getGroupingSeparator().toString()
        val v = textInput.toDoubleOrNullLocaleAware()
        if (v != null) {
            val exactMinor = FiatVOFactory.faceValueToLong(v)

            // Use the same validation logic as validateTextField to ensure consistency
            val maxAmountForValidation = getMaxAmountForValidation()
            val isInRange = exactMinor in minAmount..maxAmountForValidation
            _amountValid.value = isInRange

            // Store the UNCLAMPED value so user sees what they typed
            // This fixes issue #785: typing "5" then "0" now produces "50" instead of "60"
            quoteSideFixedAmount = FiatVOFactory.from(exactMinor, quoteCurrencyCode)
            _formattedQuoteSideFixedAmount.value = AmountFormatter.formatAmount(quoteSideFixedAmount).replace(separator, "")

            priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
            baseSideFixedAmount = priceQuote.toBaseSideMonetary(quoteSideFixedAmount) as CoinVO
            _formattedBaseSideFixedAmount.value = AmountFormatter.formatAmount(baseSideFixedAmount, false)

            // Update slider with clamped value for visual feedback
            val clampedForSlider = exactMinor.coerceIn(minAmount, maxAmountForValidation)
            _fixedAmountSliderPosition.value = MonetarySlider.minorToFraction(clampedForSlider, minAmount, maxAmount)

            updateAmountLimitInfo()
        } else {
            _formattedQuoteSideFixedAmount.value = ""
            _amountValid.value = false
        }
    }

    fun onMinAmountTextValueChange(textInput: String) {
        val separator = getGroupingSeparator().toString()
        val v = textInput.toDoubleOrNullLocaleAware()
        if (v != null) {
            val exactMinor = FiatVOFactory.faceValueToLong(v)

            // Use the same validation logic as validateTextField to ensure consistency
            val maxAmountForValidation = getMaxAmountForValidation()
            val isInRange = exactMinor in minAmount..maxAmountForValidation
            _amountValid.value = isInRange

            // Store the UNCLAMPED value so user sees what they typed
            quoteSideMinRangeAmount = FiatVOFactory.from(exactMinor, quoteCurrencyCode)
            _formattedQuoteSideMinRangeAmount.value = AmountFormatter.formatAmount(quoteSideMinRangeAmount).replace(separator, "")

            priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
            baseSideMinRangeAmount = priceQuote.toBaseSideMonetary(quoteSideMinRangeAmount) as CoinVO
            _formattedBaseSideMinRangeAmount.value = AmountFormatter.formatAmount(baseSideMinRangeAmount, false)

            // Update slider with clamped value for visual feedback
            val clampedForSlider = exactMinor.coerceIn(minAmount, maxAmountForValidation)
            _minRangeSliderValue.value = MonetarySlider.minorToFraction(clampedForSlider, minAmount, maxAmount)

            updateAmountLimitInfo()
        } else {
            _formattedQuoteSideMinRangeAmount.value = ""
            _amountValid.value = false
        }
    }

    fun onMaxAmountTextValueChange(textInput: String) {
        val separator = getGroupingSeparator().toString()
        val v = textInput.toDoubleOrNullLocaleAware()
        if (v != null) {
            val exactMinor = FiatVOFactory.faceValueToLong(v)

            // Use the same validation logic as validateTextField to ensure consistency
            val maxAmountForValidation = getMaxAmountForValidation()
            val isInRange = exactMinor in minAmount..maxAmountForValidation
            _amountValid.value = isInRange

            // Store the UNCLAMPED value so user sees what they typed
            quoteSideMaxRangeAmount = FiatVOFactory.from(exactMinor, quoteCurrencyCode)
            _formattedQuoteSideMaxRangeAmount.value = AmountFormatter.formatAmount(quoteSideMaxRangeAmount).replace(separator, "")

            priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
            baseSideMaxRangeAmount = priceQuote.toBaseSideMonetary(quoteSideMaxRangeAmount) as CoinVO
            _formattedBaseSideMaxRangeAmount.value = AmountFormatter.formatAmount(baseSideMaxRangeAmount, false)

            // Update slider with clamped value for visual feedback
            val clampedForSlider = exactMinor.coerceIn(minAmount, maxAmountForValidation)
            _maxRangeSliderValue.value = MonetarySlider.minorToFraction(clampedForSlider, minAmount, maxAmount)

            updateAmountLimitInfo()
        } else {
            _formattedQuoteSideMaxRangeAmount.value = ""
            _amountValid.value = false
        }
    }

    fun onFixedAmountSliderValueChange(value: Float) {
        if (fixedDragJob == null) {
            // Leading-edge immediate update for responsive feedback
            applyFixedAmountSliderValue(value)
            fixedDragJob = presenterScope.launch {
                // Trailing-edge coalesced update
                delay(dragUpdateSampleMs)
                latestFixedPending?.let {
                    applyFixedAmountSliderValue(it)
                    latestFixedPending = null
                }
                fixedDragJob = null
            }
        } else {
            // Coalesce subsequent updates within the sample window
            latestFixedPending = value
        }
    }

    fun onMinRangeSliderValueChange(value: Float) {
        if (rangeMinDragJob == null) {
            applyMinRangeAmountSliderValue(value)
            rangeMinDragJob = presenterScope.launch {
                delay(dragUpdateSampleMs)
                latestRangeMinPending?.let { applyMinRangeAmountSliderValue(it) }
                latestRangeMinPending = null
                rangeMinDragJob = null
            }
        } else {
            latestRangeMinPending = value
        }
    }

    fun onMaxRangeSliderValueChange(value: Float) {
        if (rangeMaxDragJob == null) {
            applyMaxRangeAmountSliderValue(value)
            rangeMaxDragJob = presenterScope.launch {
                delay(dragUpdateSampleMs)
                latestRangeMaxPending?.let { applyMaxRangeAmountSliderValue(it) }
                latestRangeMaxPending = null
                rangeMaxDragJob = null
            }
        } else {
            latestRangeMaxPending = value
        }
    }

    fun onRangeAmountSliderChanged(value: ClosedFloatingPointRange<Float>) {
        // Handle each thumb independently to preserve immediate feedback per-thumb
        if (rangeMinDragJob == null) {
            applyMinRangeAmountSliderValue(value.start)
            rangeMinDragJob = presenterScope.launch {
                delay(dragUpdateSampleMs)
                latestRangeMinPending?.let { applyMinRangeAmountSliderValue(it) }
                latestRangeMinPending = null
                rangeMinDragJob = null
            }
        } else {
            latestRangeMinPending = value.start
        }

        if (rangeMaxDragJob == null) {
            applyMaxRangeAmountSliderValue(value.endInclusive)
            rangeMaxDragJob = presenterScope.launch {
                delay(dragUpdateSampleMs)
                latestRangeMaxPending?.let { applyMaxRangeAmountSliderValue(it) }
                latestRangeMaxPending = null
                rangeMaxDragJob = null
            }
        } else {
            latestRangeMaxPending = value.endInclusive
        }
    }

    fun onBack() {
        commitToModel()
        navigateBack()
    }

    fun onSliderDragFinished() {
        // Flush any pending coalesced updates and run heavy path
        fixedDragJob?.cancel()
        rangeMinDragJob?.cancel()
        rangeMaxDragJob?.cancel()

        latestFixedPending?.let { applyFixedAmountSliderValue(it) }
        latestFixedPending = null

        latestRangeMinPending?.let { applyMinRangeAmountSliderValue(it) }
        latestRangeMaxPending?.let { applyMaxRangeAmountSliderValue(it) }
        latestRangeMinPending = null
        latestRangeMaxPending = null
        fixedDragJob = null
        rangeMinDragJob = null
        rangeMaxDragJob = null

        updateAmountLimitInfo()
    }

    fun onClose() {
        commitToModel()
        navigateToOfferbookTab()
    }

    fun onNext() {
        if (amountType.value == AmountType.RANGE_AMOUNT && quoteSideMaxRangeAmount.asDouble() < quoteSideMinRangeAmount.asDouble()) {
            showSnackbar("mobile.bisqEasy.tradeWizard.amount.range.validation.minShouldBeLessThanMax".i18n())
            return
        }
        commitToModel()
        navigateTo(NavRoute.CreateOfferPrice)
    }

    fun navigateToReputation() {
        navigateToUrl(BisqLinks.REPUTATION_WIKI_URL)
    }

    fun navigateToBuildReputation() {
        navigateToUrl(BisqLinks.BUILD_REPUTATION_WIKI_URL)
    }

    fun validateTextField(value: String): String? {
        val maxAmountForValidation = getMaxAmountForValidation()
        val validateError = AmountValidator.validate(value, minAmount, maxAmountForValidation)
        _amountValid.value = validateError == null
        return validateError
    }

    private fun getMaxAmountForValidation(): Long {
        val maxRepBasedValue = if (reputationBasedMaxSliderValue.value == null)
            0L
        else
            sliderValueToAmount(reputationBasedMaxSliderValue.value!!)
        return if (maxRepBasedValue == 0L)
            maxAmount
        else
            minOf(maxAmount, maxRepBasedValue)
    }

    // private
    private fun updateAmountLimitInfo(firstLoad: Boolean = false) {
        if (isBuy.value) {
            updateBuyersAmountLimitInfo()
        } else {
            updateSellerAmountLimitInfo(firstLoad)
        }
    }

    private fun handleAmountTextChange(
        textInput: String,
        onValueParsed: (Float) -> Unit,
        onInvalidInput: () -> Unit
    ) {
        val _value = textInput.toDoubleOrNullLocaleAware()
        if (_value != null) {
            val valueInFraction = MonetarySlider.faceValueToFraction(_value, minAmount, maxAmount, 4)
            onValueParsed(valueInFraction)
            updateAmountLimitInfo()
        } else {
            onInvalidInput()
            _amountValid.value = false
        }
    }

    private fun updateBuyersAmountLimitInfo() {
        if (!isBuy.value) {
            return
        }

        _reputationBasedMaxSliderValue.value = null
        val market = createOfferModel.market ?: return

        val fixedOrMinAmount: FiatVO = if (amountType.value == AmountType.FIXED_AMOUNT) {
            quoteSideFixedAmount
        } else {
            quoteSideMinRangeAmount
        }

        val requiredReputation: Long = findRequiredReputationScoreByFiatAmount(
            marketPriceServiceFacade,
            market,
            fixedOrMinAmount
        ) ?: 0L
        _requiredReputation.value = requiredReputation

        launchUI {
            val peersScoreByUserProfileId = withContext(IODispatcher) {
                getPeersScoreByUserProfileId()
            }
            val numPotentialTakers =
                peersScoreByUserProfileId.filter { (_, value) -> withTolerance(value) >= requiredReputation }.count()
            _shouldShowWarningIcon.value = numPotentialTakers == 0

            val numSellers = "bisqEasy.tradeWizard.amount.buyer.numSellers".i18nPlural(numPotentialTakers)
            _amountLimitInfo.value = "bisqEasy.tradeWizard.amount.buyer.limitInfo".i18n(numSellers)

            val highestScore = peersScoreByUserProfileId.maxOfOrNull { it.value } ?: 0L
            val highestPossibleAmountFromSellers =
                getReputationBasedQuoteSideAmount(marketPriceServiceFacade, market, highestScore)?.value ?: 0
            val highestPossibleAmountWithTolerance: Float = withTolerance(highestPossibleAmountFromSellers).toFloat()
            val range = maxAmount - minAmount
            _rightMarkerValue.value = (highestPossibleAmountWithTolerance - minAmount) / range

            val formattedFixedOrMinAmount =
                AmountFormatter.formatAmount(fixedOrMinAmount, useLowPrecision = true, withCode = true)
            val firstPart: String =
                "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.firstPart".i18n(
                    formattedFixedOrMinAmount,
                    requiredReputation
                )
            val secondPart = if (numPotentialTakers == 0) {
                "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.secondPart.noSellers".i18n()
            } else {
                if (numPotentialTakers == 1)
                    "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.secondPart.singular".i18n(numSellers)
                else
                    "bisqEasy.tradeWizard.amount.buyer.limitInfo.overlay.info.secondPart.plural".i18n(numSellers)
            }
            _amountLimitInfoOverlayInfo.value = firstPart + "\n\n" + secondPart + "\n\n"
        }
    }

    private fun updateSellerAmountLimitInfo(firstLoad: Boolean = false) {
        val range = maxAmount - minAmount
        launchUI {
            val userProfile: UserProfileVO = withContext(IODispatcher) {
                userProfileServiceFacade.getSelectedUserProfile()
            } ?: return@launchUI

            val reputationScore: ReputationScoreVO = withContext(IODispatcher) {
                reputationServiceFacade.getReputation(userProfile.id).getOrNull()
            } ?: return@launchUI

            _requiredReputation.value = reputationScore.totalScore
            val market = createOfferModel.market ?: return@launchUI

            val amount = getReputationBasedQuoteSideAmount(
                marketPriceServiceFacade,
                market,
                _requiredReputation.value
            ) ?: return@launchUI

            val reputationBasedMaxValue = (amount.value.toFloat() - minAmount) / range
            _reputationBasedMaxSliderValue.value = reputationBasedMaxValue
            _rightMarkerValue.value = reputationBasedMaxValue

            _formattedReputationBasedMaxAmount.value = AmountFormatter.formatAmount(
                amount,
                useLowPrecision = true,
                withCode = true
            )
            _amountLimitInfo.value =
                "bisqEasy.tradeWizard.amount.seller.limitInfo".i18n(_formattedReputationBasedMaxAmount.value)

            if (firstLoad) {
                // Reset values based on reputation
                applyFixedAmountSliderValue(reputationBasedMaxValue)
                _minRangeSliderValue.value = 0.0F
                applyRangeAmountSliderValue(0.0F..reputationBasedMaxValue)
            }
        }
    }

    private suspend fun getNumPotentialTakers(requiredReputationScore: Long): Int {
        return getPeersScoreByUserProfileId().filter { (_, value) -> withTolerance(value) >= requiredReputationScore }
            .count()
    }

    private suspend fun getPeersScoreByUserProfileId(): Map<String, Long> {
        val myProfileIds: Set<String> = userProfileServiceFacade.getUserIdentityIds().toSet()
        return reputationServiceFacade.scoreByUserProfileId
            .filterKeys { it !in myProfileIds }
            .mapValues { it.value }
    }

    private fun applyRangeAmountSliderValue(rangeSliderPosition: ClosedFloatingPointRange<Float>) {
        val separator = getGroupingSeparator().toString()
        this.rangeSliderPosition = rangeSliderPosition

        val range = maxAmount - minAmount
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)

        val min = rangeSliderPosition.start
        val roundedMinQuoteValue: Long = MonetarySlider.fractionToAmountLong(min, minAmount, maxAmount, 10000L)

        quoteSideMinRangeAmount = FiatVOFactory.from(roundedMinQuoteValue, quoteCurrencyCode)
        // iOS specific Fix: Removing the grouping separator (,), to keep displayed value, typed valid in sync,
        // to avoid BasicTextField text reset issue
        _formattedQuoteSideMinRangeAmount.value =
            AmountFormatter.formatAmount(quoteSideMinRangeAmount).replace(separator, "")

        baseSideMinRangeAmount =
            priceQuote.toBaseSideMonetary(quoteSideMinRangeAmount) as CoinVO
        _formattedBaseSideMinRangeAmount.value = AmountFormatter.formatAmount(baseSideMinRangeAmount, false)

        val max = rangeSliderPosition.endInclusive
        val roundedMaxQuoteValue: Long = MonetarySlider.fractionToAmountLong(max, minAmount, maxAmount, 10000L)

        quoteSideMaxRangeAmount = FiatVOFactory.from(roundedMaxQuoteValue, quoteCurrencyCode)
        _formattedQuoteSideMaxRangeAmount.value =
            AmountFormatter.formatAmount(quoteSideMaxRangeAmount).replace(separator, "")

        baseSideMaxRangeAmount =
            priceQuote.toBaseSideMonetary(quoteSideMaxRangeAmount) as CoinVO
        _formattedBaseSideMaxRangeAmount.value = AmountFormatter.formatAmount(baseSideMaxRangeAmount, false)
    }


    private fun applyMinRangeAmountSliderValue(amount: Float) {
        val separator = getGroupingSeparator().toString()
        _minRangeSliderValue.value = amount
        quoteSideMinRangeAmount =
            FiatVOFactory.from(sliderValueToAmount(minRangeSliderValue.value), quoteCurrencyCode)
        _formattedQuoteSideMinRangeAmount.value =
            AmountFormatter.formatAmount(quoteSideMinRangeAmount).replace(separator, "")
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
        baseSideMinRangeAmount = priceQuote.toBaseSideMonetary(quoteSideMinRangeAmount) as CoinVO
        _formattedBaseSideMinRangeAmount.value = AmountFormatter.formatAmount(baseSideMinRangeAmount, false)
    }

    private fun applyMaxRangeAmountSliderValue(amount: Float) {
        val separator = getGroupingSeparator().toString()
        _maxRangeSliderValue.value = amount
        quoteSideMaxRangeAmount =
            FiatVOFactory.from(sliderValueToAmount(maxRangeSliderValue.value), quoteCurrencyCode)
        _formattedQuoteSideMaxRangeAmount.value =
            AmountFormatter.formatAmount(quoteSideMaxRangeAmount).replace(separator, "")
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
        baseSideMaxRangeAmount = priceQuote.toBaseSideMonetary(quoteSideMaxRangeAmount) as CoinVO
        _formattedBaseSideMaxRangeAmount.value = AmountFormatter.formatAmount(baseSideMaxRangeAmount, false)
    }

    private fun applyFixedAmountSliderValue(amount: Float) {
        val separator = getGroupingSeparator().toString()
        _fixedAmountSliderPosition.value = amount
        quoteSideFixedAmount =
            FiatVOFactory.from(sliderValueToAmount(fixedAmountSliderPosition.value), quoteCurrencyCode)
        _formattedQuoteSideFixedAmount.value = AmountFormatter.formatAmount(quoteSideFixedAmount).replace(separator, "")
        priceQuote = createOfferPresenter.getMostRecentPriceQuote(createOfferModel.market!!)
        baseSideFixedAmount = priceQuote.toBaseSideMonetary(quoteSideFixedAmount) as CoinVO
        _formattedBaseSideFixedAmount.value = AmountFormatter.formatAmount(baseSideFixedAmount, false)
    }

    private fun sliderValueToAmount(amount: Float): Long {
        return MonetarySlider.fractionToAmountLong(amount, minAmount, maxAmount, 10_000L)
    }

    private fun getFractionForFiat(value: Double): Float {
        return MonetarySlider.faceValueToFraction(value, minAmount, maxAmount, 4)
    }

    private fun commitToModel() {
        if (amountType.value == AmountType.RANGE_AMOUNT && quoteSideMinRangeAmount.asDouble() == quoteSideMaxRangeAmount.asDouble()) {
            quoteSideFixedAmount = quoteSideMinRangeAmount
            _amountType.value = AmountType.FIXED_AMOUNT
        }
        createOfferPresenter.commitAmount(
            amountType.value,
            quoteSideFixedAmount,
            baseSideFixedAmount,
            quoteSideMinRangeAmount,
            baseSideMinRangeAmount,
            quoteSideMaxRangeAmount,
            baseSideMaxRangeAmount
        )
    }
}
