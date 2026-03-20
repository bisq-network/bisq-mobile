/**
 * PaymentAccountsRedesignScreen.kt — Design PoC (Issue #991)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * This feature is INTENTIONALLY HIDDEN until the MuSig protocol is finalized in Bisq2.
 * Do not expose this screen via any navigation route until MuSig is production-ready.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Redesigned main Payment Accounts screen. Replaces the current flat form
 * (PaymentAccountsScreen.kt) with a tab-based view that separates Fiat and Crypto
 * accounts — matching the two-section structure of the Bisq2 desktop.
 *
 * The existing screen only supports UserDefinedFiatAccount (free-text name + data).
 * This redesign anticipates the full payment method registry (40+ fiat rails, Monero,
 * other crypto) that will be needed for MuSig trade settlement.
 *
 * ======================================================================================
 * DESKTOP ADAPTATION
 * ======================================================================================
 * Desktop organizes payment accounts into two sub-sections under Settings > User:
 *   1. "Fiat Payment Accounts" — sortable table listing all saved fiat accounts
 *   2. "Crypto Asset Accounts" — separate table for XMR and other crypto
 *
 * Mobile consolidates both under one screen with a tab selector at the top.
 * Rationale: separate navigation destinations would require two menu entries in
 * the "More" tab, adding cognitive overhead. A tab row fits within a single screen
 * and makes the Fiat / Crypto distinction clear without extra navigation depth.
 *
 * Tab implementation uses BisqSegmentButton (SingleChoiceSegmentedButtonRow under the
 * hood) — consistent with the Buy/Sell toggle in the offerbook and the direction
 * selector in the create-offer wizard.
 *
 * ======================================================================================
 * SCREEN STATES
 * ======================================================================================
 * - Fiat tab, has accounts: scrollable list of FiatPaymentAccountCards
 * - Fiat tab, no accounts: empty-state with icon + copy + "Add Fiat Account" CTA
 * - Crypto tab, has accounts: scrollable list of CryptoPaymentAccountCards
 * - Crypto tab, no accounts: empty-state with icon + copy + "Add Crypto Account" CTA
 *
 * The "Add Account" button is always pinned at screen bottom (outside the scroll area)
 * regardless of whether accounts exist, so the add-account affordance never scrolls
 * out of reach. This matches the pattern in the existing PaymentAccountsScreen.
 *
 * ======================================================================================
 * NAVIGATION CONTRACT (proposed)
 * ======================================================================================
 * When the user taps "Add Account":
 *   - Fiat tab selected → navigate to CreateFiatAccountWizard
 *   - Crypto tab selected → navigate to CreateCryptoAccountWizard
 *
 * Both wizards navigate back to this screen on completion or cancellation.
 * Edit tap on a card → navigate to wizard in edit mode (step 2 pre-filled).
 *
 * ======================================================================================
 * I18N CONSIDERATIONS
 * ======================================================================================
 * - Tab labels ("Fiat", "Crypto") are short; no expansion risk in German/Russian.
 * - Empty-state copy strings need new i18n keys:
 *     mobile.paymentAccounts.fiat.empty.title
 *     mobile.paymentAccounts.fiat.empty.body
 *     mobile.paymentAccounts.crypto.empty.title
 *     mobile.paymentAccounts.crypto.empty.body
 *     mobile.paymentAccounts.addFiatAccount
 *     mobile.paymentAccounts.addCryptoAccount
 * - The bottom button label changes per tab — "Add Fiat Account" / "Add Crypto Account".
 *   German expansions for these are ~40% longer; the button uses fillMaxWidth so this
 *   is safe as long as we don't also add a left icon.
 */
package network.bisq.mobile.presentation.design.payment_accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqSegmentButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqScrollScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.TopBarContent
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.ui.tooling.preview.Preview

// -------------------------------------------------------------------------------------
// Tab selector model
// -------------------------------------------------------------------------------------

/**
 * The two account categories exposed in the tab selector.
 * Enum used instead of Boolean to allow future addition of a third category (e.g. Lightning)
 * without breaking callers.
 */
enum class PaymentAccountTab {
    FIAT,
    CRYPTO,
}

// -------------------------------------------------------------------------------------
// Screen composable
// -------------------------------------------------------------------------------------

/**
 * Stateless content composable for the redesigned Payment Accounts screen.
 *
 * Layout structure:
 * ┌─────────────────────────────────┐
 * │ TopBar: "Payment Accounts"      │
 * ├─────────────────────────────────┤
 * │  [  Fiat  ]  [  Crypto  ]      │  BisqSegmentButton tab row
 * ├─────────────────────────────────┤
 * │  Scrollable account list        │  or empty state
 * ├─────────────────────────────────┤
 * │  [ + Add Account ]             │  pinned bottom button
 * └─────────────────────────────────┘
 *
 * @param selectedTab Currently selected tab (controls which list is shown)
 * @param fiatAccounts List of saved fiat accounts to display
 * @param cryptoAccounts List of saved crypto accounts to display
 * @param onTabSelect Called when the user switches tabs
 * @param onAddFiatAccountClick Called when "Add Fiat Account" is tapped
 * @param onAddCryptoAccountClick Called when "Add Crypto Account" is tapped
 * @param onEditFiatAccount Called with account when user taps Edit on a fiat card
 * @param onDeleteFiatAccount Called with account when user taps Delete on a fiat card
 * @param onEditCryptoAccount Called with account when user taps Edit on a crypto card
 * @param onDeleteCryptoAccount Called with account when user taps Delete on a crypto card
 * @param topBar Slot for the TopBar composable (injected for stateless preview support)
 */
@Composable
fun PaymentAccountsRedesignContent(
    selectedTab: PaymentAccountTab,
    fiatAccounts: List<SimulatedFiatAccount>,
    cryptoAccounts: List<SimulatedCryptoAccount>,
    onTabSelect: (PaymentAccountTab) -> Unit,
    onAddFiatAccountClick: () -> Unit,
    onAddCryptoAccountClick: () -> Unit,
    onEditFiatAccount: (SimulatedFiatAccount) -> Unit,
    onDeleteFiatAccount: (SimulatedFiatAccount) -> Unit,
    onEditCryptoAccount: (SimulatedCryptoAccount) -> Unit,
    onDeleteCryptoAccount: (SimulatedCryptoAccount) -> Unit,
    topBar: @Composable () -> Unit = {},
) {
    val tabItems =
        listOf(
            PaymentAccountTab.FIAT to "Fiat",
            PaymentAccountTab.CRYPTO to "Crypto",
        )

    BisqScrollScaffold(
        topBar = topBar,
        bottomBar = {
            // Pinned add-account button — never scrolls out of reach
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(
                            horizontal = BisqUIConstants.ScreenPadding2X,
                            vertical = BisqUIConstants.ScreenPadding,
                        ),
            ) {
                BisqButton(
                    text =
                        if (selectedTab == PaymentAccountTab.FIAT) {
                            "Add Fiat Account"
                        } else {
                            "Add Crypto Account"
                        },
                    onClick =
                        if (selectedTab == PaymentAccountTab.FIAT) {
                            onAddFiatAccountClick
                        } else {
                            onAddCryptoAccountClick
                        },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
    ) {
        // Tab row — outside the scroll area so it stays pinned below the TopBar
        BisqSegmentButton(
            value = selectedTab,
            items = tabItems,
            onValueChange = { onTabSelect(it.first) },
        )

        BisqGap.V1()

        // Content per tab
        when (selectedTab) {
            PaymentAccountTab.FIAT -> {
                if (fiatAccounts.isEmpty()) {
                    FiatEmptyState()
                } else {
                    FiatAccountList(
                        accounts = fiatAccounts,
                        onEditAccount = onEditFiatAccount,
                        onDeleteAccount = onDeleteFiatAccount,
                    )
                }
            }

            PaymentAccountTab.CRYPTO -> {
                if (cryptoAccounts.isEmpty()) {
                    CryptoEmptyState()
                } else {
                    CryptoAccountList(
                        accounts = cryptoAccounts,
                        onEditAccount = onEditCryptoAccount,
                        onDeleteAccount = onDeleteCryptoAccount,
                    )
                }
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Tab content composables
// -------------------------------------------------------------------------------------

/**
 * Scrollable list of fiat account cards.
 * Each card is a [FiatPaymentAccountCard] with edit and delete actions.
 */
@Composable
private fun FiatAccountList(
    accounts: List<SimulatedFiatAccount>,
    onEditAccount: (SimulatedFiatAccount) -> Unit,
    onDeleteAccount: (SimulatedFiatAccount) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        accounts.forEach { account ->
            FiatPaymentAccountCard(
                account = account,
                onEditClick = { onEditAccount(account) },
                onDeleteClick = { onDeleteAccount(account) },
            )
        }
    }
}

/**
 * Scrollable list of crypto account cards.
 */
@Composable
private fun CryptoAccountList(
    accounts: List<SimulatedCryptoAccount>,
    onEditAccount: (SimulatedCryptoAccount) -> Unit,
    onDeleteAccount: (SimulatedCryptoAccount) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        accounts.forEach { account ->
            CryptoPaymentAccountCard(
                account = account,
                onEditClick = { onEditAccount(account) },
                onDeleteClick = { onDeleteAccount(account) },
            )
        }
    }
}

/**
 * Empty state for the Fiat tab.
 *
 * Copy strategy: explain WHY fiat accounts are useful (pre-filling trade payment data)
 * rather than just saying "no accounts". Users unfamiliar with Bisq need the "why"
 * to feel motivated to complete setup — matching the pattern in the existing
 * EmptyAccountsState in PaymentAccountsScreen.
 */
@Composable
private fun FiatEmptyState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BisqUIConstants.ScreenPadding2X),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H4LightGrey("No fiat payment accounts yet")
        BisqGap.V1()
        BisqText.H3Light("Why set up a fiat account?")
        BisqGap.VHalf()
        BisqText.BaseLight(
            "Saving a payment account pre-fills your trade payment details, " +
                "so you don't need to re-enter them for every trade. " +
                "Accounts are stored locally on your device.",
        )
        BisqGap.V1()
        BisqText.BaseLightGrey(
            "You can save accounts for SEPA, Zelle, Revolut, Wise, " +
                "and 20+ other payment rails.",
        )
    }
}

/**
 * Empty state for the Crypto tab.
 *
 * Crypto accounts are primarily receiving addresses. The copy makes this concrete
 * so users understand what data they're about to enter.
 */
@Composable
private fun CryptoEmptyState() {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = BisqUIConstants.ScreenPadding2X),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H4LightGrey("No crypto asset accounts yet")
        BisqGap.V1()
        BisqText.H3Light("Add your receiving address")
        BisqGap.VHalf()
        BisqText.BaseLight(
            "A crypto account holds the address where you want to receive " +
                "crypto assets when trading on Bisq. Monero (XMR) and other " +
                "cryptocurrencies are supported.",
        )
        BisqGap.V1()
        BisqText.BaseLightGrey(
            "Your address is only shared with your trading counterparty " +
                "after both sides confirm the trade contract.",
        )
    }
}

// -------------------------------------------------------------------------------------
// Preview helpers
// -------------------------------------------------------------------------------------

private val previewFiatAccounts =
    listOf(
        SimulatedFiatAccount(
            accountName = "Primary SEPA",
            methodId = "SEPA",
            methodDisplayName = "SEPA",
            currencies = listOf("EUR"),
            chargebackRisk = SimulatedChargebackRisk.VERY_LOW,
        ),
        SimulatedFiatAccount(
            accountName = "Revolut USD",
            methodId = "REVOLUT",
            methodDisplayName = "Revolut",
            currencies = listOf("USD", "EUR"),
            chargebackRisk = SimulatedChargebackRisk.LOW,
        ),
        SimulatedFiatAccount(
            accountName = "Zelle — Chase",
            methodId = "ZELLE",
            methodDisplayName = "Zelle",
            currencies = listOf("USD"),
            chargebackRisk = SimulatedChargebackRisk.MODERATE,
        ),
    )

private val previewCryptoAccounts =
    listOf(
        SimulatedCryptoAccount(
            accountName = "Cold Storage XMR",
            cryptoType = "XMR",
            address = "49A6bqH8sDLxpzymNFVPMzxCRnzN1FUkBHmELFUmBz3mRTymR9R9yQcEgAf6WkqmhVm",
        ),
        SimulatedCryptoAccount(
            accountName = "Litecoin Hot",
            cryptoType = "LTC",
            address = "ltc1qnxrw5d5g9h2k7m8p0q3s4t6u7v8w9x0y1z2a3b",
        ),
    )

@ExcludeFromCoverage
@Composable
private fun PreviewTopBar() {
    TopBarContent(
        title = "Payment Accounts",
        showBackButton = true,
        showUserAvatar = true,
    )
}

private val previewNoOp: () -> Unit = {}
private val previewNoOpFiat: (SimulatedFiatAccount) -> Unit = {}
private val previewNoOpCrypto: (SimulatedCryptoAccount) -> Unit = {}

@ExcludeFromCoverage
@Preview
@Composable
private fun PaymentAccountsRedesign_FiatWithAccountsPreview() {
    BisqTheme.Preview {
        PaymentAccountsRedesignContent(
            selectedTab = PaymentAccountTab.FIAT,
            fiatAccounts = previewFiatAccounts,
            cryptoAccounts = previewCryptoAccounts,
            onTabSelect = {},
            onAddFiatAccountClick = previewNoOp,
            onAddCryptoAccountClick = previewNoOp,
            onEditFiatAccount = previewNoOpFiat,
            onDeleteFiatAccount = previewNoOpFiat,
            onEditCryptoAccount = previewNoOpCrypto,
            onDeleteCryptoAccount = previewNoOpCrypto,
            topBar = { PreviewTopBar() },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PaymentAccountsRedesign_FiatEmptyPreview() {
    BisqTheme.Preview {
        PaymentAccountsRedesignContent(
            selectedTab = PaymentAccountTab.FIAT,
            fiatAccounts = emptyList(),
            cryptoAccounts = emptyList(),
            onTabSelect = {},
            onAddFiatAccountClick = previewNoOp,
            onAddCryptoAccountClick = previewNoOp,
            onEditFiatAccount = previewNoOpFiat,
            onDeleteFiatAccount = previewNoOpFiat,
            onEditCryptoAccount = previewNoOpCrypto,
            onDeleteCryptoAccount = previewNoOpCrypto,
            topBar = { PreviewTopBar() },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PaymentAccountsRedesign_CryptoWithAccountsPreview() {
    BisqTheme.Preview {
        PaymentAccountsRedesignContent(
            selectedTab = PaymentAccountTab.CRYPTO,
            fiatAccounts = previewFiatAccounts,
            cryptoAccounts = previewCryptoAccounts,
            onTabSelect = {},
            onAddFiatAccountClick = previewNoOp,
            onAddCryptoAccountClick = previewNoOp,
            onEditFiatAccount = previewNoOpFiat,
            onDeleteFiatAccount = previewNoOpFiat,
            onEditCryptoAccount = previewNoOpCrypto,
            onDeleteCryptoAccount = previewNoOpCrypto,
            topBar = { PreviewTopBar() },
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PaymentAccountsRedesign_CryptoEmptyPreview() {
    BisqTheme.Preview {
        PaymentAccountsRedesignContent(
            selectedTab = PaymentAccountTab.CRYPTO,
            fiatAccounts = emptyList(),
            cryptoAccounts = emptyList(),
            onTabSelect = {},
            onAddFiatAccountClick = previewNoOp,
            onAddCryptoAccountClick = previewNoOp,
            onEditFiatAccount = previewNoOpFiat,
            onDeleteFiatAccount = previewNoOpFiat,
            onEditCryptoAccount = previewNoOpCrypto,
            onDeleteCryptoAccount = previewNoOpCrypto,
            topBar = { PreviewTopBar() },
        )
    }
}
