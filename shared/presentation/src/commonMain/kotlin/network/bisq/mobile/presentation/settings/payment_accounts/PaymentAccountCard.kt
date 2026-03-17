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
 * PaymentAccountsRedesignScreen list. Replaces the flat dropdown-plus-text-field pattern
 * in the current PaymentAccountsScreen with visually scannable, action-capable cards.
 *
 * One card per account; two visual variants:
 *   - Fiat account: shows method icon, account name, method label, currencies, chargeback
 *     risk badge (color-coded), and edit/delete inline actions.
 *   - Crypto account: shows method icon, account name, crypto type label, address
 *     (truncated), and edit/delete inline actions. No chargeback risk badge.
 *
 * ======================================================================================
 * DESKTOP ADAPTATION
 * ======================================================================================
 * Desktop shows payment accounts in a table (name | type | currencies | country | age).
 * Mobile replaces the table with a card list because:
 *   - Single-column layout accommodates thumb-zone interaction better than row taps
 *   - Cards group related metadata visually without horizontal scroll
 *   - Inline edit/delete avoids a separate selection + action-button pattern
 *
 * The chargeback risk indicator (VERY_LOW / LOW / MODERATE) is surfaced on the card
 * because desktop buries it inside the "create account" wizard table. For mobile users
 * who may have added accounts long ago, the card is the persistent reminder of risk level.
 *
 * ======================================================================================
 * COMPONENT DESIGN
 * ======================================================================================
 * Layout (top to bottom inside BisqCard):
 *   Row: [PaymentMethodIcon 36dp] [Column: account name + method + currencies] [action buttons]
 *   Optional row (fiat only): chargeback risk badge with color-coded background
 *
 * Icon sizing: 36dp — larger than the 20dp used in offer rows to ensure account identity
 * is immediately readable in the account management context (not a dense list).
 *
 * Action buttons: Edit (Outline) and Delete (Danger) placed at card-bottom as a row,
 * full width. Rationale: Android guidelines prefer bottom placement for destructive
 * actions; users building muscle memory won't accidentally hit Delete while scanning.
 *
 * ======================================================================================
 * I18N CONSIDERATIONS
 * ======================================================================================
 * - Method labels use existing i18n system for payment method names where available.
 *   The POC uses hardcoded English strings — real implementation wires to i18n keys.
 * - Risk level strings ("Very Low", "Low", "Moderate") need new i18n keys:
 *     mobile.paymentAccounts.risk.veryLow
 *     mobile.paymentAccounts.risk.low
 *     mobile.paymentAccounts.risk.moderate
 * - Crypto address truncation is display-only; full address available on detail/edit screen.
 * - Account name field supports up to ~50 chars; German/Russian names tend to be longer —
 *   the weight(1f) column grows naturally without truncation risk.
 */
package network.bisq.mobile.presentation.settings.payment_accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethodIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.ui.tooling.preview.Preview

// -------------------------------------------------------------------------------------
// Domain models (primitives only — no presenter dependency)
// -------------------------------------------------------------------------------------

/**
 * Chargeback risk levels for fiat payment methods.
 * Maps to desktop's ChargebackRisk enum (VERY_LOW, LOW, MODERATE).
 * No HIGH risk methods are included in Bisq Easy's supported rails.
 */
enum class SimulatedChargebackRisk {
    VERY_LOW,
    LOW,
    MODERATE,
}

/**
 * Simulated fiat account data for preview use.
 * Real implementation would use domain VOs from the replicated package.
 */
data class SimulatedFiatAccount(
    val accountName: String,
    val methodId: String,
    val methodDisplayName: String,
    val currencies: List<String>,
    val chargebackRisk: SimulatedChargebackRisk,
)

/**
 * Simulated crypto account data for preview use.
 */
data class SimulatedCryptoAccount(
    val accountName: String,
    val cryptoType: String,
    val address: String,
)

// -------------------------------------------------------------------------------------
// Helper functions
// -------------------------------------------------------------------------------------

/**
 * Maps a chargeback risk level to the appropriate theme color.
 *
 * Color semantics follow established traffic-light conventions:
 * - VERY_LOW → primary (green) — safe to proceed without concern
 * - LOW → warning (orange) — minor awareness required
 * - MODERATE → danger (red) — user should understand the trade-off before using
 *
 * We deliberately skip VERY_LOW → grey because users would confuse "grey badge" with
 * "disabled" or "unknown". Green actively signals trustworthiness.
 */
@Composable
private fun chargebackRiskColor(risk: SimulatedChargebackRisk): Color =
    when (risk) {
        SimulatedChargebackRisk.VERY_LOW -> BisqTheme.colors.primary
        SimulatedChargebackRisk.LOW -> BisqTheme.colors.warning
        SimulatedChargebackRisk.MODERATE -> BisqTheme.colors.danger
    }

private fun chargebackRiskLabel(risk: SimulatedChargebackRisk): String =
    when (risk) {
        SimulatedChargebackRisk.VERY_LOW -> "Chargeback risk: Very Low"
        SimulatedChargebackRisk.LOW -> "Chargeback risk: Low"
        SimulatedChargebackRisk.MODERATE -> "Chargeback risk: Moderate"
    }

/**
 * Truncates a crypto address to first 8 + "..." + last 6 chars for compact display.
 * The pattern is familiar to crypto users from wallet apps and block explorers.
 */
private fun truncateAddress(address: String): String =
    if (address.length > 16) {
        "${address.take(8)}...${address.takeLast(6)}"
    } else {
        address
    }

// -------------------------------------------------------------------------------------
// Shared inner components
// -------------------------------------------------------------------------------------

/**
 * The edit/delete action row rendered at the bottom of every account card.
 *
 * Two-button layout: Edit (Outline) + Delete (Danger). Using distinct visual weights
 * ensures the destructive action is recognizable without being accidentally triggered.
 * Both buttons are equally sized to avoid thumb-zone guessing.
 */
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

// -------------------------------------------------------------------------------------
// Public composables
// -------------------------------------------------------------------------------------

/**
 * Card displaying a saved fiat payment account.
 *
 * Information hierarchy (top to bottom):
 * 1. Account identity row: icon + name (H3) + method type label
 * 2. Supported currencies (SmallLight, subdued)
 * 3. Chargeback risk badge (color-coded pill) — educates user at a glance
 * 4. Action buttons: Edit / Delete
 *
 * The risk badge is placed ABOVE the actions intentionally. It should be seen
 * before the user decides to edit or delete — the edit flow might show risk context.
 *
 * @param account Fiat account data to display
 * @param onEditClick Called when user taps Edit
 * @param onDeleteClick Called when user taps Delete
 */
@Composable
fun FiatPaymentAccountCard(
    account: SimulatedFiatAccount,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    BisqCard(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        // Identity row: icon + name + method
        Row(
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
                BisqText.SmallLight(
                    account.methodDisplayName,
                    color = BisqTheme.colors.mid_grey20,
                )
            }
        }

        // Currency chips row
        if (account.currencies.isNotEmpty()) {
            Row(horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf)) {
                account.currencies.forEach { currency ->
                    CurrencyBadge(currency)
                }
            }
        }

        // Chargeback risk badge
        ChargebackRiskBadge(risk = account.chargebackRisk)

        BisqGap.VHalf()

        AccountCardActions(
            onEditClick = onEditClick,
            onDeleteClick = onDeleteClick,
        )
    }
}

/**
 * Card displaying a saved crypto payment account.
 *
 * Simpler layout than the fiat card — no chargeback risk applies to crypto.
 * The address is truncated for display; full address accessible via Edit.
 *
 * @param account Crypto account data to display
 * @param onEditClick Called when user taps Edit
 * @param onDeleteClick Called when user taps Delete
 */
@Composable
fun CryptoPaymentAccountCard(
    account: SimulatedCryptoAccount,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
) {
    BisqCard(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
    ) {
        // Identity row: icon + name + crypto type
        Row(
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
        }

        // Truncated address — lets user confirm they're looking at the right account
        // without exposing the full address in a potentially screenshot-visible list
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

// -------------------------------------------------------------------------------------
// Sub-components (internal)
// -------------------------------------------------------------------------------------

/**
 * Small rounded pill showing a currency code (e.g. "EUR", "USD").
 * Uses dark_grey50 background to avoid competing with the card background.
 */
@Composable
private fun CurrencyBadge(currency: String) {
    Surface(
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = BisqTheme.colors.dark_grey50,
    ) {
        BisqText.SmallRegular(
            currency,
            color = BisqTheme.colors.white,
            modifier = Modifier.padding(
                horizontal = BisqUIConstants.ScreenPaddingHalf,
                vertical = BisqUIConstants.ScreenPaddingQuarter,
            ),
        )
    }
}

/**
 * Inline risk badge: colored left-border accent + risk label text.
 *
 * Why a left-border rather than a full background fill? Full fills fight with
 * the card's dark_grey40 background. A colored accent is enough to draw the
 * eye while remaining legible on OLED screens.
 *
 * Implementation uses a Surface with a colored border side + inner text row.
 * The color is determined by [chargebackRiskColor].
 */
@Composable
private fun ChargebackRiskBadge(risk: SimulatedChargebackRisk) {
    val riskColor = chargebackRiskColor(risk)

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = riskColor.copy(alpha = 0.12f),
    ) {
        Row(
            modifier = Modifier
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
// Previews
// -------------------------------------------------------------------------------------

private val previewOnClick: () -> Unit = {}

@ExcludeFromCoverage
@Preview
@Composable
private fun FiatCard_SepaPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            FiatPaymentAccountCard(
                account = SimulatedFiatAccount(
                    accountName = "My SEPA Account",
                    methodId = "SEPA",
                    methodDisplayName = "SEPA",
                    currencies = listOf("EUR"),
                    chargebackRisk = SimulatedChargebackRisk.VERY_LOW,
                ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun FiatCard_ZelleModerateRiskPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            FiatPaymentAccountCard(
                account = SimulatedFiatAccount(
                    accountName = "Zelle — Chase Bank",
                    methodId = "ZELLE",
                    methodDisplayName = "Zelle",
                    currencies = listOf("USD"),
                    chargebackRisk = SimulatedChargebackRisk.MODERATE,
                ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun FiatCard_CustomPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            FiatPaymentAccountCard(
                account = SimulatedFiatAccount(
                    accountName = "PayPal Personal",
                    methodId = "CUSTOM",
                    methodDisplayName = "Custom",
                    currencies = listOf("USD", "EUR", "GBP"),
                    chargebackRisk = SimulatedChargebackRisk.LOW,
                ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CryptoCard_MoneroPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            CryptoPaymentAccountCard(
                account = SimulatedCryptoAccount(
                    accountName = "Main XMR Wallet",
                    cryptoType = "XMR",
                    address = "49A6bqH8sDLxpzymNFVPMzxCRnzN1FUkBHmELFUmBz3mRTymR9R9yQcEgAf6WkqmhVm",
                ),
                onEditClick = previewOnClick,
                onDeleteClick = previewOnClick,
            )
        }
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CryptoCard_OtherPreview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            CryptoPaymentAccountCard(
                account = SimulatedCryptoAccount(
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
