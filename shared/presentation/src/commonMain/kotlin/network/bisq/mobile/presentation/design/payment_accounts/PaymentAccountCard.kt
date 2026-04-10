/**
 * PaymentAccountCard.kt — Design PoC (Issue #991)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * This feature is INTENTIONALLY HIDDEN until the MuSig protocol is finalized in Bisq2.
 * Do not expose this screen via any navigation route until MuSig is production-ready.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Reusable card component for displaying a single saved payment account in the
 * PaymentAccountsRedesignScreen list. Cards are **expandable**: collapsed shows just
 * enough to identify the account (icon + name + method + country summary + risk);
 * expanded reveals full country availability, currencies, and edit/delete actions.
 * This lets users skim a list of 5-10 accounts quickly and only drill into the one
 * they care about.
 *
 * Two visual variants:
 *   - Fiat account: method icon, account name, method label + brief country summary,
 *     chargeback risk badge. Expanded: full country availability subtitle with "+N more"
 *     tappable link, currency badges with flag icons, edit/delete actions.
 *   - Crypto account: method icon, account name, crypto type.
 *     Expanded: truncated address, edit/delete actions.
 *
 * ======================================================================================
 * COUNTRY AVAILABILITY (Hybrid approach)
 * ======================================================================================
 * Each fiat account carries a [SimulatedCountryAvailability] describing where the
 * payment method can be used (single country, regional list, or worldwide).
 *
 * **Collapsed header** shows a brief one-liner next to the method name:
 *   - "SEPA · 37 countries"
 *   - "Zelle · United States"
 *   - "Wise · Worldwide"
 *
 * **Expanded section** shows the full [CountryAvailabilitySubtitle] (reused from
 * CreateFiatAccountWizard) with the "+N more" tappable link that opens a
 * [CountryListBottomSheet] with the complete sorted country list.
 *
 * This hybrid approach lets users distinguish accounts at a glance (e.g., "is this
 * my US-only or my worldwide account?") while keeping the collapsed card compact.
 *
 * ======================================================================================
 * DESKTOP ADAPTATION
 * ======================================================================================
 * Desktop shows payment accounts in a table (name | type | currencies | country | age).
 * Mobile replaces the table with expandable cards because:
 *   - Single-column layout accommodates thumb-zone interaction
 *   - Collapsed cards enable fast scanning; expanded cards show full detail
 *   - Inline edit/delete avoids a separate selection + action-button pattern
 *
 * ======================================================================================
 * ASSETS USED
 * ======================================================================================
 * - Payment method icons: files/payment/fiat/{method_id}.png via PaymentMethodIcon
 * - Currency flag icons: files/markets/fiat/market_{code}.png via DynamicImage
 * - Both are existing assets already used in the offerbook and create-offer flows.
 *
 * ======================================================================================
 * I18N CONSIDERATIONS
 * ======================================================================================
 * - Risk level strings need new i18n keys:
 *     mobile.paymentAccounts.risk.veryLow / .low / .moderate
 * - "Edit" / "Delete" use existing action.edit / paymentAccounts.deleteAccount keys.
 * - Currency codes are ISO 4217 — no localization needed.
 * - Country summary strings (e.g., "N countries", "Worldwide") need i18n keys:
 *     mobile.paymentAccounts.countries.count (parameterized: "{0} countries")
 *     mobile.paymentAccounts.countries.worldwide
 */
package network.bisq.mobile.presentation.design.payment_accounts

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.DynamicImage
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethodIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// -------------------------------------------------------------------------------------
// Domain models (primitives only — no presenter dependency)
// -------------------------------------------------------------------------------------

enum class SimulatedChargebackRisk {
    VERY_LOW,
    LOW,
    MODERATE,
}

data class SimulatedFiatAccount(
    val accountName: String,
    val methodId: String,
    val methodDisplayName: String,
    val currencies: List<String>,
    val chargebackRisk: SimulatedChargebackRisk,
    val countryAvailability: SimulatedCountryAvailability = SimulatedCountryAvailability.Worldwide,
)

data class SimulatedCryptoAccount(
    val accountName: String,
    val cryptoType: String,
    val address: String,
)

// -------------------------------------------------------------------------------------
// Helper functions
// -------------------------------------------------------------------------------------

@Composable
internal fun chargebackRiskColor(risk: SimulatedChargebackRisk): Color =
    when (risk) {
        SimulatedChargebackRisk.VERY_LOW -> BisqTheme.colors.primary
        SimulatedChargebackRisk.LOW -> BisqTheme.colors.warning
        SimulatedChargebackRisk.MODERATE -> BisqTheme.colors.danger
    }

internal fun chargebackRiskLabel(risk: SimulatedChargebackRisk): String =
    when (risk) {
        SimulatedChargebackRisk.VERY_LOW -> "Chargeback risk: Very Low"
        SimulatedChargebackRisk.LOW -> "Chargeback risk: Low"
        SimulatedChargebackRisk.MODERATE -> "Chargeback risk: Moderate"
    }

/**
 * Brief one-liner summary of country availability for the collapsed card header.
 * Shows: "37 countries", "United States", or "Worldwide".
 */
private fun countrySummary(availability: SimulatedCountryAvailability): String =
    when (availability) {
        is SimulatedCountryAvailability.Worldwide -> "Worldwide"
        is SimulatedCountryAvailability.Countries -> {
            val codes = availability.codes
            when {
                codes.size == 1 -> countryName(codes.first())
                else -> "${codes.size} countries"
            }
        }
    }

private fun truncateAddress(address: String): String =
    if (address.length > 16) {
        "${address.take(8)}...${address.takeLast(6)}"
    } else {
        address
    }

// -------------------------------------------------------------------------------------
// Shared inner components
// -------------------------------------------------------------------------------------

@Composable
private fun AccountCardActions(
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        BisqButton(
            text = "Edit",
            type = BisqButtonType.Outline,
            onClick = onEditClick,
            modifier = Modifier.weight(1f),
        )
        BisqButton(
            text = "Delete",
            type = BisqButtonType.Danger,
            onClick = onDeleteClick,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Currency badge with flag icon from existing market assets.
 * Uses DynamicImage to load files/markets/fiat/market_{code}.png.
 * Falls back to text-only if the image is not available.
 */
@Composable
private fun CurrencyBadge(currency: String) {
    Surface(
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey50,
    ) {
        Row(
            modifier =
                Modifier.padding(
                    horizontal = BisqUIConstants.ScreenPaddingHalf,
                    vertical = BisqUIConstants.ScreenPaddingQuarter,
                ),
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingQuarter),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            DynamicImage(
                path = "files/markets/fiat/market_${currency.lowercase()}.png",
                modifier = Modifier.size(16.dp),
            )
            BisqText.SmallRegular(
                currency,
                color = BisqTheme.colors.white,
            )
        }
    }
}

@Composable
internal fun ChargebackRiskBadge(risk: SimulatedChargebackRisk) {
    val riskColor = chargebackRiskColor(risk)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = riskColor.copy(alpha = 0.12f),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding,
                        vertical = BisqUIConstants.ScreenPaddingHalf,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            Surface(
                modifier = Modifier.size(width = 3.dp, height = 16.dp),
                shape = RoundedCornerShape(2.dp),
                color = riskColor,
            ) {}
            BisqText.SmallRegular(
                chargebackRiskLabel(risk),
                color = riskColor,
            )
        }
    }
}

// -------------------------------------------------------------------------------------
// Public composables
// -------------------------------------------------------------------------------------

/**
 * Expandable fiat payment account card.
 *
 * **Collapsed** (default): icon + account name + method label with brief country summary
 * (e.g., "SEPA · 37 countries") + chargeback risk badge + expand chevron.
 * Enough to identify the account, its geographic scope, and risk level at a glance.
 *
 * **Expanded**: adds full country availability subtitle with tappable "+N more" link,
 * currency badges with flag icons, and edit/delete action buttons.
 * Tapping "+N more" opens a [CountryListBottomSheet] with the complete country list.
 *
 * Tap anywhere on the card header to toggle expansion.
 *
 * @param account Fiat account data (includes country availability)
 * @param initiallyExpanded Whether the card starts expanded (useful for single-account view)
 * @param onEditClick Called when user taps Edit
 * @param onDeleteClick Called when user taps Delete
 */
@Composable
fun FiatPaymentAccountCard(
    account: SimulatedFiatAccount,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    initiallyExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }
    // Track which method's country list bottom sheet is open (null = closed)
    var showCountrySheet by remember { mutableStateOf(false) }

    BisqCard(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        // Collapsed header: always visible — tappable to toggle
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            PaymentMethodIcon(
                methodId = account.methodId,
                isPaymentMethod = true,
                size = 36.dp,
                contentDescription = account.methodDisplayName,
            )
            Column(modifier = Modifier.weight(1f)) {
                BisqText.H4Regular(account.accountName)
                BisqGap.VQuarter()
                // Method name + brief country summary (e.g., "SEPA · 37 countries")
                BisqText.SmallLight(
                    "${account.methodDisplayName} · ${countrySummary(account.countryAvailability)}",
                    color = BisqTheme.colors.mid_grey20,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = BisqTheme.colors.mid_grey20,
                modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
            )
        }

        // Chargeback risk — always visible (key info for scanning)
        ChargebackRiskBadge(risk = account.chargebackRisk)

        // Expanded content: country detail + currencies + actions
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                // Full country availability with "+N more" tappable link
                CountryAvailabilitySubtitle(
                    availability = account.countryAvailability,
                    onShowFullList = { showCountrySheet = true },
                )

                // Currency badges with flag icons
                if (account.currencies.isNotEmpty()) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                    ) {
                        account.currencies.forEach { currency ->
                            CurrencyBadge(currency)
                        }
                    }
                }

                BisqGap.VHalf()

                AccountCardActions(
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                )
            }
        }
    }

    // Country list bottom sheet
    if (showCountrySheet) {
        val availability = account.countryAvailability
        if (availability is SimulatedCountryAvailability.Countries) {
            CountryListBottomSheet(
                methodName = account.methodDisplayName,
                countryCodes = availability.codes,
                onDismiss = { showCountrySheet = false },
            )
        }
    }
}

/**
 * Expandable crypto payment account card.
 *
 * **Collapsed**: icon + account name + crypto type + expand chevron.
 * **Expanded**: truncated address + edit/delete actions.
 */
@Composable
fun CryptoPaymentAccountCard(
    account: SimulatedCryptoAccount,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    initiallyExpanded: Boolean = false,
) {
    var expanded by remember { mutableStateOf(initiallyExpanded) }

    BisqCard(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        // Collapsed header: always visible
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            PaymentMethodIcon(
                methodId = account.cryptoType,
                isPaymentMethod = false,
                size = 36.dp,
                contentDescription = account.cryptoType,
            )
            Column(modifier = Modifier.weight(1f)) {
                BisqText.H4Regular(account.accountName)
                BisqGap.VQuarter()
                BisqText.SmallLight(
                    account.cryptoType,
                    color = BisqTheme.colors.mid_grey20,
                )
            }
            Icon(
                imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                contentDescription = if (expanded) "Collapse" else "Expand",
                tint = BisqTheme.colors.mid_grey20,
                modifier = Modifier.size(BisqUIConstants.ScreenPadding2X),
            )
        }

        // Expanded content: address + actions
        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(),
            exit = shrinkVertically(),
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                BisqText.SmallRegular(
                    truncateAddress(account.address),
                    color = BisqTheme.colors.mid_grey30,
                )

                BisqGap.VHalf()

                AccountCardActions(
                    onEditClick = onEditClick,
                    onDeleteClick = onDeleteClick,
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Previews
// -------------------------------------------------------------------------------------

private val previewOnClick: () -> Unit = {}

/**
 * Preview: SEPA account collapsed — shows country summary ("37 countries") and risk badge
 * visible even when collapsed.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun FiatCard_Sepa_CollapsedPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            FiatPaymentAccountCard(
                account =
                    SimulatedFiatAccount(
                        accountName = "My SEPA Account",
                        methodId = "SEPA",
                        methodDisplayName = "SEPA",
                        currencies = listOf("EUR"),
                        chargebackRisk = SimulatedChargebackRisk.VERY_LOW,
                        countryAvailability = SimulatedCountryAvailability.Countries(sepaCountries),
                    ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
            )
        }
    }
}

/**
 * Preview: SEPA account expanded — shows full country availability subtitle with "+N more",
 * currency badges with flag icons, and actions.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun FiatCard_Sepa_ExpandedPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            FiatPaymentAccountCard(
                account =
                    SimulatedFiatAccount(
                        accountName = "My SEPA Account",
                        methodId = "SEPA",
                        methodDisplayName = "SEPA",
                        currencies = listOf("EUR"),
                        chargebackRisk = SimulatedChargebackRisk.VERY_LOW,
                        countryAvailability = SimulatedCountryAvailability.Countries(sepaCountries),
                    ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
                initiallyExpanded = true,
            )
        }
    }
}

/**
 * Preview: Zelle with moderate risk — single country (US), red risk badge.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun FiatCard_Zelle_ExpandedPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            FiatPaymentAccountCard(
                account =
                    SimulatedFiatAccount(
                        accountName = "Zelle — Chase Bank",
                        methodId = "ZELLE",
                        methodDisplayName = "Zelle",
                        currencies = listOf("USD"),
                        chargebackRisk = SimulatedChargebackRisk.MODERATE,
                        countryAvailability = SimulatedCountryAvailability.Countries(listOf("US")),
                    ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
                initiallyExpanded = true,
            )
        }
    }
}

/**
 * Preview: Wise worldwide account expanded — "Worldwide" in collapsed, full detail expanded.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun FiatCard_Wise_ExpandedPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            FiatPaymentAccountCard(
                account =
                    SimulatedFiatAccount(
                        accountName = "Wise Multi-Currency",
                        methodId = "WISE",
                        methodDisplayName = "Wise",
                        currencies = listOf("USD", "EUR", "GBP", "CHF"),
                        chargebackRisk = SimulatedChargebackRisk.LOW,
                        countryAvailability = SimulatedCountryAvailability.Worldwide,
                    ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
                initiallyExpanded = true,
            )
        }
    }
}

/**
 * Preview: multiple cards in a list — shows the scanning experience with country summaries.
 * First card expanded (SEPA, 37 countries), rest collapsed showing brief country info.
 * Demonstrates: regional (SEPA), single-country (Zelle/US), multi-country (Revolut).
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun FiatCards_ListPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            FiatPaymentAccountCard(
                account =
                    SimulatedFiatAccount(
                        accountName = "My SEPA Account",
                        methodId = "SEPA",
                        methodDisplayName = "SEPA",
                        currencies = listOf("EUR"),
                        chargebackRisk = SimulatedChargebackRisk.VERY_LOW,
                        countryAvailability = SimulatedCountryAvailability.Countries(sepaCountries),
                    ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
                initiallyExpanded = true,
            )
            FiatPaymentAccountCard(
                account =
                    SimulatedFiatAccount(
                        accountName = "Zelle — Chase Bank",
                        methodId = "ZELLE",
                        methodDisplayName = "Zelle",
                        currencies = listOf("USD"),
                        chargebackRisk = SimulatedChargebackRisk.MODERATE,
                        countryAvailability = SimulatedCountryAvailability.Countries(listOf("US")),
                    ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
            )
            FiatPaymentAccountCard(
                account =
                    SimulatedFiatAccount(
                        accountName = "Revolut EUR",
                        methodId = "REVOLUT",
                        methodDisplayName = "Revolut",
                        currencies = listOf("EUR", "USD", "GBP"),
                        chargebackRisk = SimulatedChargebackRisk.LOW,
                        countryAvailability = SimulatedCountryAvailability.Countries(revolutCountries),
                    ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
            )
        }
    }
}

/**
 * Preview: Monero crypto card expanded — shows truncated address.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun CryptoCard_Monero_ExpandedPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            CryptoPaymentAccountCard(
                account =
                    SimulatedCryptoAccount(
                        accountName = "Main XMR Wallet",
                        cryptoType = "XMR",
                        address = "49A6bqH8sDLxpzymNFVPMzxCRnzN1FUkBHmELFUmBz3mRTymR9R9yQcEgAf6WkqmhVm",
                    ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
                initiallyExpanded = true,
            )
        }
    }
}

/**
 * Preview: Crypto card collapsed — compact scanning view.
 */
@ExcludeFromCoverage
@Preview
@Composable
private fun CryptoCard_Ltc_CollapsedPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            CryptoPaymentAccountCard(
                account =
                    SimulatedCryptoAccount(
                        accountName = "Litecoin Hot Wallet",
                        cryptoType = "LTC",
                        address = "ltc1qnxrw5d5g9h2k7m8p0q3s4t6u7v8w9x0y1z2a3b",
                    ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
            )
        }
    }
}
