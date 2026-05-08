/**
 * WiseFormSimulatedData.kt — Shared simulated data for Wise form design POCs.
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * Contains the Wise currency catalogue (matching Bisq2's WiseAccountPayload coverage)
 * and UiState/UiAction definitions shared across both variant files.
 */

@file:Suppress("MagicNumber")

package network.bisq.mobile.presentation.design.payment_accounts.wise

// -------------------------------------------------------------------------------------
// Full Wise currency catalogue
// Mirrors the ~40 currencies Bisq2's desktop WiseFormView lists as checkboxes.
// -------------------------------------------------------------------------------------

/** All currencies Wise supports, as (code, display name) pairs, sorted by display name. */
internal val WISE_CURRENCIES: List<Pair<String, String>> =
    listOf(
        "ARS" to "Argentine Peso",
        "AUD" to "Australian Dollar",
        "BDT" to "Bangladeshi Taka",
        "BWP" to "Botswanan Pula",
        "BRL" to "Brazilian Real",
        "GBP" to "British Pound",
        "CAD" to "Canadian Dollar",
        "CLP" to "Chilean Peso",
        "CNY" to "Chinese Yuan",
        "CRC" to "Costa Rican Colon",
        "CZK" to "Czech Koruna",
        "DKK" to "Danish Krone",
        "EGP" to "Egyptian Pound",
        "EUR" to "Euro",
        "FJD" to "Fijian Dollar",
        "GEL" to "Georgian Lari",
        "GHS" to "Ghanaian Cedi",
        "HKD" to "Hong Kong Dollar",
        "HUF" to "Hungarian Forint",
        "INR" to "Indian Rupee",
        "IDR" to "Indonesian Rupiah",
        "ILS" to "Israeli New Shekel",
        "JPY" to "Japanese Yen",
        "KES" to "Kenyan Shilling",
        "MYR" to "Malaysian Ringgit",
        "MXN" to "Mexican Peso",
        "MAD" to "Moroccan Dirham",
        "NPR" to "Nepalese Rupee",
        "NZD" to "New Zealand Dollar",
        "NGN" to "Nigerian Naira",
        "NOK" to "Norwegian Krone",
        "PHP" to "Philippine Peso",
        "PLN" to "Polish Zloty",
        "RON" to "Romanian Leu",
        "SGD" to "Singapore Dollar",
        "ZAR" to "South African Rand",
        "SEK" to "Swedish Krona",
        "CHF" to "Swiss Franc",
        "THB" to "Thai Baht",
        "TRY" to "Turkish Lira",
        "UAH" to "Ukrainian Hryvnia",
        "USD" to "United States Dollar",
        "VND" to "Vietnamese Dong",
    )

/**
 * "Popular" currencies shown expanded by default in the tiered design.
 * Chosen by global trading volume + Bisq user demographics.
 */
internal val WISE_POPULAR_CURRENCY_CODES: Set<String> =
    setOf(
        "USD",
        "EUR",
        "GBP",
        "JPY",
        "BRL",
        "AUD",
        "CAD",
        "INR",
        "MXN",
        "ARS",
    )

// -------------------------------------------------------------------------------------
// UiState / UiAction for Wise form (shared by both design variants)
// -------------------------------------------------------------------------------------

/**
 * Immutable state snapshot for the Wise account data form.
 *
 * [selectedCurrencyCodes] is the authoritative selection set. Both variants drive
 * their UI purely from this value — no local checkbox state.
 *
 * [currencyError] is non-null when the user attempted Next with an empty selection.
 * [holderNameError] and [emailError] are non-null when the respective field fails validation.
 */
data class WiseFormUiState(
    val holderName: String = "",
    val email: String = "",
    val selectedCurrencyCodes: Set<String> = WISE_CURRENCIES.map { it.first }.toSet(),
    val holderNameError: String? = null,
    val emailError: String? = null,
    val currencyError: String? = null,
    val isCurrencyPickerOpen: Boolean = false,
    val currencySearchQuery: String = "",
)

/** Actions the Wise form view can emit. */
sealed class WiseFormUiAction {
    data class HolderNameChanged(
        val value: String,
    ) : WiseFormUiAction()

    data class EmailChanged(
        val value: String,
    ) : WiseFormUiAction()

    data class CurrencyToggled(
        val code: String,
    ) : WiseFormUiAction()

    data object SelectAllCurrencies : WiseFormUiAction()

    data object ClearAllCurrencies : WiseFormUiAction()

    data object OpenCurrencyPicker : WiseFormUiAction()

    data object CloseCurrencyPicker : WiseFormUiAction()

    data class CurrencySearchChanged(
        val query: String,
    ) : WiseFormUiAction()

    data object NextTapped : WiseFormUiAction()
}

// -------------------------------------------------------------------------------------
// Helpers
// -------------------------------------------------------------------------------------

/** Returns the display name for a currency code, or the code itself as fallback. */
internal fun wiseCurrencyName(code: String): String = WISE_CURRENCIES.firstOrNull { it.first == code }?.second ?: code

/**
 * Returns the filtered Wise currency list for the bottom-sheet search field.
 * Matches against both code and display name.
 */
internal fun filteredWiseCurrencies(query: String): List<Pair<String, String>> =
    if (query.isBlank()) {
        WISE_CURRENCIES
    } else {
        WISE_CURRENCIES.filter { (code, name) ->
            code.contains(query, ignoreCase = true) || name.contains(query, ignoreCase = true)
        }
    }
