/**
 * PaymentAccountReviewWithDetailsDesign.kt — Design PoC
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * Companion to the MuSig payment-rails work on `PaymentAccountReviewScreen` /
 * `ZelleAccountReviewContent` / `MoneroAccountReviewContent`.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Adapts the desktop "payment-method details" layout (two sections separated by
 * uppercase headers + a thin rule, "PAYMENT ACCOUNT DATA" and "DETAILS") to mobile.
 * The existing mobile review only shows the user-entered fields. Desktop also exposes
 * a DETAILS section with account metadata (creation date, age, chargeback risk, max
 * trade amount, max trade duration) — that part is missing on mobile today.
 *
 * ======================================================================================
 * DESIGN DECISIONS
 * ======================================================================================
 * 1. **Single card** — Mobile is vertical-scarce. Nesting two or three cards forces
 *    extra scrolling. Inside one BisqCard we use uppercase section labels with a thin
 *    horizontal rule to get the same hierarchy desktop achieves with full-width rules.
 *
 * 2. **Section header component** — `AccountReviewSectionHeader` (uppercase, mid-grey,
 *    SmallLight) + a 1.dp horizontal rule. Reusable across review screens.
 *
 * 3. **Field row reuse** — uses the existing `AccountReviewFieldRow` pattern (label on
 *    top in mid-grey SmallLight, value below in white BaseRegular). Stays consistent
 *    with `ZelleAccountReviewContent` / `MoneroAccountReviewContent`.
 *
 * 4. **DETAILS section content** — mirrors desktop:
 *      - Account creation date (formatted localized date)
 *      - Account age (e.g. "3 months", or "N/A" for fresh accounts)
 *      - Chargeback risk (text label here; the colored badge stays in fiat-only flows)
 *      - Max. trade amount (formatted with trade currency)
 *      - Max. trade duration (humanized e.g. "8 days")
 *    The DETAILS section is OPTIONAL — composable params allow showing only the
 *    PAYMENT ACCOUNT DATA section for accounts that don't have metadata yet (e.g.
 *    immediately after creation, before persistence).
 *
 * 5. **Chargeback risk in DETAILS** — desktop shows it as plain text "Moderate". The
 *    existing mobile flow uses `FiatChargebackRiskBadge` (colored bar + text) for fiat
 *    accounts. The PoC keeps both:
 *      - In the field row inside DETAILS, show the risk text only (matches desktop).
 *      - Below the section, render the existing color badge as a final reminder
 *        (matches the existing mobile review pattern).
 *
 * 6. **Option B alternative** — one card per section, included as a preview at the
 *    bottom for comparison. Lighter visual weight, more breathing room, but adds
 *    vertical real estate. Recommended pick: Option A.
 *
 * ======================================================================================
 * INTEGRATION NOTES (for the implementing dev)
 * ======================================================================================
 * The component to extract into `presentation/create_payment_account/account_review/
 * ui/core/` is `AccountReviewSectionHeader`. Then update `ZelleAccountReviewContent`
 * and `MoneroAccountReviewContent` to:
 *   - Wrap the user-entered fields under an `AccountReviewSectionHeader("Payment account data")`
 *   - Add an `AccountReviewSectionHeader("Details")` followed by the metadata rows
 *     (date, age, risk, limits, duration)
 *
 * The metadata fields exist on `PaymentAccount` already (`creationDate`,
 * `tradeLimitInfo`, `tradeDuration`). If `creationDate` is null (account not yet
 * persisted), skip the DETAILS section entirely.
 *
 * i18n keys to add (sentence-case as elsewhere in this app):
 *   - `mobile.user.paymentAccounts.review.section.accountData` = "Payment account data"
 *   - `mobile.user.paymentAccounts.review.section.details` = "Details"
 *   - `mobile.user.paymentAccounts.review.field.accountAge` = "Account age"
 *   - `mobile.user.paymentAccounts.review.field.creationDate` = "Account creation date"
 *   - `mobile.user.paymentAccounts.review.field.maxTradeAmount` = "Max. trade amount"
 *   - `mobile.user.paymentAccounts.review.field.maxTradeDuration` = "Max. trade duration"
 *   - `mobile.user.paymentAccounts.review.field.chargebackRisk` = "Chargeback risk"
 *   - `mobile.user.paymentAccounts.review.field.notAvailable` = "N/A"
 */

@file:Suppress("MagicNumber")

package network.bisq.mobile.presentation.design.payment_accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

/**
 * Lightweight stand-ins so the PoC is independent of domain types. Mirrors the shape
 * of `PaymentAccount` + payment-method VOs without pulling in their dependencies.
 */
@ExcludeFromCoverage
data class SimulatedReviewMethod(
    val displayName: String,
    val secondaryLabel: String, // currency code for fiat, asset code for crypto
    val iconLetter: String, // first letter as a stand-in for PaymentAccountTypeIcon
    val iconColor: Color,
    val restrictions: String = "",
)

@ExcludeFromCoverage
enum class SimulatedDetailsRisk(
    val text: String,
    val color: Color,
) {
    VERY_LOW("Very low", Color(0xFF7AC74F)),
    LOW("Low", Color(0xFFB7D14F)),
    MODERATE("Moderate", Color(0xFFD1A14F)),
    HIGH("High", Color(0xFFD15F4F)),
    VERY_HIGH("Very high", Color(0xFFC72F2F)),
}

@ExcludeFromCoverage
data class SimulatedReviewFields(
    val pairs: List<Pair<String, String>>,
)

@ExcludeFromCoverage
data class SimulatedReviewDetails(
    val creationDate: String, // pre-formatted; production would use a date formatter
    val accountAge: String, // e.g. "N/A", "3 months", "1 year 2 months"
    val chargebackRisk: SimulatedDetailsRisk?,
    val maxTradeAmount: String, // pre-formatted with currency
    val maxTradeDuration: String, // e.g. "8 days"
)

// =====================================================================================
// CORE COMPONENTS — what would be extracted to production
// =====================================================================================

/**
 * Section header used inside a review card to demarcate logical groups
 * (PAYMENT ACCOUNT DATA, DETAILS, …). Renders an uppercase label in mid-grey
 * followed by a 1.dp horizontal rule that spans the available width. Visually
 * matches the desktop pattern from `PaymentMethodDetailsView`.
 */
@Composable
fun AccountReviewSectionHeader(label: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        BisqText.SmallLight(
            text = label.uppercase(),
            color = BisqTheme.colors.mid_grey20,
        )
        BisqGap.VHalf()
        Surface(
            modifier = Modifier.fillMaxWidth().height(1.dp),
            color = BisqTheme.colors.mid_grey10,
            content = {},
        )
    }
}

@Composable
fun AccountReviewFieldRow(
    label: String,
    value: String,
) {
    Column {
        BisqText.SmallLight(label, color = BisqTheme.colors.mid_grey20)
        BisqGap.VQuarter()
        BisqText.BaseRegular(value)
    }
}

// =====================================================================================
// OPTION A — single card, sections separated by header + rule (RECOMMENDED)
// =====================================================================================

/**
 * Mobile review with the desktop section pattern adapted into a single card.
 *
 * Visual hierarchy:
 *  - Method icon + name + secondary (currency / asset code) at top
 *  - PAYMENT ACCOUNT DATA section header + rule, then user-entered field rows
 *  - DETAILS section header + rule, then metadata rows
 *  - Optional restrictions row at the bottom of DETAILS when set
 */
@Composable
fun PaymentAccountReviewSectionsCard(
    method: SimulatedReviewMethod,
    fields: SimulatedReviewFields,
    details: SimulatedReviewDetails?,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey40,
    ) {
        Column {
            // Method header row — same as today's ZelleAccountReviewContent
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                        .padding(BisqUIConstants.ScreenPadding)
                        .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                // Stand-in for PaymentAccountTypeIcon
                Surface(
                    modifier = Modifier.height(BisqUIConstants.ScreenPadding2X),
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                    color = method.iconColor,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BisqText.BaseRegular(method.iconLetter, color = BisqTheme.colors.white)
                    }
                }
                Column {
                    BisqText.BaseRegular(method.displayName)
                    BisqText.BaseRegularGrey(method.secondaryLabel)
                }
            }

            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                AccountReviewSectionHeader("Payment account data")
                fields.pairs.forEach { (label, value) ->
                    AccountReviewFieldRow(label = label, value = value)
                }

                if (details != null) {
                    AccountReviewSectionHeader("Details")
                    AccountReviewFieldRow(
                        label = "Account creation date",
                        value = details.creationDate,
                    )
                    AccountReviewFieldRow(
                        label = "Account age",
                        value = details.accountAge,
                    )
                    details.chargebackRisk?.let { risk ->
                        AccountReviewFieldRow(
                            label = "Chargeback risk",
                            value = risk.text,
                        )
                    }
                    AccountReviewFieldRow(
                        label = "Max. trade amount",
                        value = details.maxTradeAmount,
                    )
                    AccountReviewFieldRow(
                        label = "Max. trade duration",
                        value = details.maxTradeDuration,
                    )
                    if (method.restrictions.isNotEmpty()) {
                        AccountReviewFieldRow(
                            label = "Restrictions",
                            value = method.restrictions,
                        )
                    }
                }
            }
        }
    }
}

// =====================================================================================
// OPTION B — one card per section (alternate, included for comparison)
// =====================================================================================

/**
 * Alternate layout: header card + a card per section with the section title rendered
 * outside the card (above it). Closer to the desktop layout's "labeled region" feel
 * but uses more vertical space. Not recommended unless the team wants stronger
 * visual separation.
 */
@Composable
fun PaymentAccountReviewSectionsStacked(
    method: SimulatedReviewMethod,
    fields: SimulatedReviewFields,
    details: SimulatedReviewDetails?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding)) {
        // Method header card
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
            color = BisqTheme.colors.dark_grey40,
        ) {
            Row(
                modifier =
                    Modifier
                        .padding(BisqUIConstants.ScreenPadding)
                        .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                Surface(
                    modifier = Modifier.height(BisqUIConstants.ScreenPadding2X),
                    shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                    color = method.iconColor,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        BisqText.BaseRegular(method.iconLetter, color = BisqTheme.colors.white)
                    }
                }
                Column {
                    BisqText.BaseRegular(method.displayName)
                    BisqText.BaseRegularGrey(method.secondaryLabel)
                }
            }
        }

        // Account data section
        BisqText.SmallLight(
            "PAYMENT ACCOUNT DATA",
            color = BisqTheme.colors.mid_grey20,
        )
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
            color = BisqTheme.colors.dark_grey40,
        ) {
            Column(
                modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                fields.pairs.forEach { (label, value) ->
                    AccountReviewFieldRow(label = label, value = value)
                }
            }
        }

        // Details section
        if (details != null) {
            BisqText.SmallLight("DETAILS", color = BisqTheme.colors.mid_grey20)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                color = BisqTheme.colors.dark_grey40,
            ) {
                Column(
                    modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                ) {
                    AccountReviewFieldRow("Account creation date", details.creationDate)
                    AccountReviewFieldRow("Account age", details.accountAge)
                    details.chargebackRisk?.let {
                        AccountReviewFieldRow("Chargeback risk", it.text)
                    }
                    AccountReviewFieldRow("Max. trade amount", details.maxTradeAmount)
                    AccountReviewFieldRow("Max. trade duration", details.maxTradeDuration)
                    if (method.restrictions.isNotEmpty()) {
                        AccountReviewFieldRow("Restrictions", method.restrictions)
                    }
                }
            }
        }
    }
}

// =====================================================================================
// PREVIEWS
// =====================================================================================

private val zelleMethod =
    SimulatedReviewMethod(
        displayName = "Zelle",
        secondaryLabel = "USD",
        iconLetter = "Z",
        iconColor = Color(0xFF8C7AE6),
        restrictions = "Max. trade amount: 5000.00 / Max. trade duration: 4 days",
    )

private val zelleFields =
    SimulatedReviewFields(
        pairs =
            listOf(
                "Country" to "United States",
                "Account owner full name" to "Alice Doe",
                "Email or mobile number" to "alice@example.com",
            ),
    )

private val zelleDetails =
    SimulatedReviewDetails(
        creationDate = "Apr 3, 2026",
        accountAge = "27 days",
        chargebackRisk = SimulatedDetailsRisk.LOW,
        maxTradeAmount = "5000.00 USD",
        maxTradeDuration = "4 days",
    )

private val cashByMailMethod =
    SimulatedReviewMethod(
        displayName = "Cash By Mail",
        secondaryLabel = "CNY (Chinese Yuan)",
        iconLetter = "✉",
        iconColor = Color(0xFFD13F4F),
        restrictions = "",
    )

private val cashByMailFields =
    SimulatedReviewFields(
        pairs =
            listOf(
                "Postal address" to "1231",
                "Contact info" to "Test Test",
                "Additional information" to "test",
            ),
    )

private val cashByMailDetails =
    SimulatedReviewDetails(
        creationDate = "Apr 3, 2026",
        accountAge = "N/A",
        chargebackRisk = SimulatedDetailsRisk.MODERATE,
        maxTradeAmount = "5000.00",
        maxTradeDuration = "8 days",
    )

@ExcludeFromCoverage
@Preview
@Composable
private fun OptionA_Zelle_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H6Regular("Review account")
            BisqGap.V1()
            AccountReviewFieldRow(label = "Account name", value = "Alice Doe")
            BisqGap.V1()
            PaymentAccountReviewSectionsCard(
                method = zelleMethod,
                fields = zelleFields,
                details = zelleDetails,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OptionA_CashByMail_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H6Regular("Review account")
            BisqGap.V1()
            AccountReviewFieldRow(label = "Account name", value = "My Cash by Mail")
            BisqGap.V1()
            PaymentAccountReviewSectionsCard(
                method = cashByMailMethod,
                fields = cashByMailFields,
                details = cashByMailDetails,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OptionA_NoDetails_Preview() {
    // Use case: account has just been entered but not yet persisted, so there are
    // no metadata fields to show. The DETAILS section is hidden in this state.
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H6Regular("Review account")
            BisqGap.V1()
            AccountReviewFieldRow(label = "Account name", value = "Alice Doe")
            BisqGap.V1()
            PaymentAccountReviewSectionsCard(
                method = zelleMethod,
                fields = zelleFields,
                details = null,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OptionB_Zelle_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H6Regular("Review account")
            BisqGap.V1()
            AccountReviewFieldRow(label = "Account name", value = "Alice Doe")
            BisqGap.V1()
            PaymentAccountReviewSectionsStacked(
                method = zelleMethod,
                fields = zelleFields,
                details = zelleDetails,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun OptionB_CashByMail_Preview() {
    BisqTheme.Preview {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .background(BisqTheme.colors.backgroundColor)
                    .padding(BisqUIConstants.ScreenPadding),
        ) {
            BisqText.H6Regular("Review account")
            BisqGap.V1()
            AccountReviewFieldRow(label = "Account name", value = "My Cash by Mail")
            BisqGap.V1()
            PaymentAccountReviewSectionsStacked(
                method = cashByMailMethod,
                fields = cashByMailFields,
                details = cashByMailDetails,
            )
        }
    }
}
