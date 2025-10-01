package network.bisq.mobile.presentation.ui.uicases.take_offer

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import network.bisq.mobile.domain.data.replicated.common.currency.MarketVO
import network.bisq.mobile.domain.data.replicated.common.monetary.CoinVO
import network.bisq.mobile.domain.data.replicated.common.monetary.FiatVO
import network.bisq.mobile.domain.data.replicated.common.monetary.PriceQuoteVO
import network.bisq.mobile.domain.data.replicated.offer.amount.spec.RangeAmountSpecVO
import network.bisq.mobile.domain.data.replicated.offer.bisq_easy.BisqEasyOfferVO
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationDto
import network.bisq.mobile.domain.data.replicated.presentation.offerbook.OfferItemPresentationModel
import network.bisq.mobile.domain.data.replicated.user.profile.UserProfileVO
import network.bisq.mobile.domain.data.replicated.user.reputation.ReputationScoreVO
import network.bisq.mobile.domain.service.market_price.MarketPriceServiceFacade
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.i18n.I18nSupport
import network.bisq.mobile.presentation.MainPresenter
import network.bisq.mobile.presentation.ui.helpers.AmountValidator
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for TakeOfferAmountPresenter text input behavior.
 *
 * Issue #785: When user types "5" followed by "0" to enter "50 AUD" (min: 6, max: 500),
 * the first digit "5" was being auto-corrected to "6", resulting in "60" instead of "50".
 *
 * These tests verify that:
 * 1. Validation logic correctly identifies out-of-range values
 * 2. Validation shows errors but doesn't modify the input
 * 3. Out-of-range values are properly validated
 *
 * Note: These tests focus on the AmountValidator which is the core validation logic.
 * Full presenter tests would require complex mocking of domain services.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TakeOfferAmountInputTest {

    private val testDispatcher = UnconfinedTestDispatcher()

    // Test data: min = 6 AUD (60000 minor), max = 500 AUD (5000000 minor)
    private val minAmount = 60000L  // 6.0 AUD
    private val maxAmount = 5000000L  // 500.0 AUD

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        startKoin {
            modules(
                module {
                    single { CoroutineExceptionHandlerSetup() }
                    factory<CoroutineJobsManager> {
                        DefaultCoroutineJobsManager().apply {
                            get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
                        }
                    }
                }
            )
        }
        I18nSupport.initialize("en")
    }

    @AfterTest
    fun tearDown() {
        stopKoin()
        Dispatchers.resetMain()
    }

    // ===== Validation Tests =====
    // These tests verify that AmountValidator correctly identifies valid/invalid amounts
    // without auto-correcting the user's input

    @Test
    fun testValidation_BelowMin_ReturnsErrorMessage() {
        // Validate "5" (below min of 6)
        val error = AmountValidator.validate("5", minAmount, maxAmount)

        // Should return an error message
        assertTrue(error != null && error.isNotEmpty(), "Validation should return error for value below minimum")
        assertTrue(error!!.contains("greater"), "Error message should mention 'greater than'")
    }

    @Test
    fun testValidation_AboveMax_ReturnsErrorMessage() {
        // Validate "501" (above max of 500)
        val error = AmountValidator.validate("501", minAmount, maxAmount)

        // Should return an error message
        assertTrue(error != null && error.isNotEmpty(), "Validation should return error for value above maximum")
        assertTrue(error!!.contains("less"), "Error message should mention 'less than'")
    }

    @Test
    fun testValidation_WithinRange_ReturnsNull() {
        // Validate "50" (within range)
        val error = AmountValidator.validate("50", minAmount, maxAmount)

        // Should return null (no error)
        assertEquals(null, error, "Validation should return null for valid value")
    }

    @Test
    fun testValidation_AtMinimum_ReturnsNull() {
        // Validate "6" (exactly at minimum)
        val error = AmountValidator.validate("6", minAmount, maxAmount)

        // Should return null (no error)
        assertEquals(null, error, "Validation should return null for value at minimum")
    }

    @Test
    fun testValidation_AtMaximum_ReturnsNull() {
        // Validate "500" (exactly at maximum)
        val error = AmountValidator.validate("500", minAmount, maxAmount)

        // Should return null (no error)
        assertEquals(null, error, "Validation should return null for value at maximum")
    }

    @Test
    fun testValidation_EmptyInput_ReturnsErrorMessage() {
        // Validate empty string
        val error = AmountValidator.validate("", minAmount, maxAmount)

        // Should return an error message
        assertTrue(error != null && error.isNotEmpty(), "Validation should return error for empty input")
    }

    @Test
    fun testValidation_NonNumericInput_ReturnsErrorMessage() {
        // Validate non-numeric text
        val error = AmountValidator.validate("abc", minAmount, maxAmount)

        // Should return an error message
        assertTrue(error != null && error.isNotEmpty(), "Validation should return error for non-numeric input")
    }

    @Test
    fun testValidation_PartialInput_Five_ReturnsErrorMessage() {
        // This is the key test for issue #785
        // When user types "5" (intending to type "50"), validation should show error
        // but the input should NOT be auto-corrected to "6"
        val error = AmountValidator.validate("5", minAmount, maxAmount)

        // Should return an error message (5 < 6)
        assertTrue(error != null && error.isNotEmpty(),
            "Validation should return error for '5' which is below minimum of 6")
        assertTrue(error!!.contains("greater"),
            "Error message should indicate value should be greater than minimum")
    }
}

