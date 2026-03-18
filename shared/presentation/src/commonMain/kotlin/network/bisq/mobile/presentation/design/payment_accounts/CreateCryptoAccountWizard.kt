/**
 * CreateCryptoAccountWizard.kt — Design PoC (Issue #991)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * This feature is INTENTIONALLY HIDDEN until the MuSig protocol is finalized in Bisq2.
 * Do not expose this screen via any navigation route until MuSig is production-ready.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Mobile-adapted wizard for creating a crypto asset payment account. Covers the flow
 * for saving a receiving address for Monero (XMR) or any other cryptocurrency.
 *
 * This is intentionally simpler than the fiat wizard:
 *   - No chargeback risk (not applicable to crypto)
 *   - No payment method catalogue to browse (two choices: Monero or Other)
 *   - Monero is a first-class entry because XMR is the primary crypto traded on Bisq Easy
 *
 * The wizard has three steps:
 *   Step 1 — Select Crypto Type (Monero or Other)
 *   Step 2 — Enter Address (+ optional account name)
 *   Step 3 — Review & Confirm
 *
 * ======================================================================================
 * DESKTOP ADAPTATION
 * ======================================================================================
 * Desktop "Crypto Asset Accounts" section has:
 *   - A single dropdown to select the crypto currency
 *   - One text field for the wallet address
 *   - Optional: sub-address support for Monero
 *
 * Mobile adapts this into a 3-step wizard for consistency with the fiat wizard UX.
 * The extra steps reduce input errors by letting users confirm their address before
 * saving. Crypto addresses are long, opaque strings — a review step is especially
 * important to catch paste errors.
 *
 * Step 1 uses large, tappable "type cards" rather than a dropdown. With only two
 * options (Monero / Other), a dropdown would be needlessly indirect on mobile.
 * The type-card selection gives each option a visual footprint appropriate for
 * the importance of the choice.
 *
 * ======================================================================================
 * MONERO-SPECIFIC CONSIDERATIONS
 * ======================================================================================
 * Monero has two address types that are relevant for Bisq:
 *   - Standard address (95 chars, starting with '4')
 *   - Sub-address (95 chars, starting with '8')
 *
 * The POC shows a single address field. The real implementation should:
 *   1. Validate address format (length + prefix) and show an inline error
 *   2. Consider a "Use sub-address" hint for privacy-conscious users
 *   3. Not expose view-key fields (out of scope for account setup)
 *
 * Address validation feedback is represented in this POC by the
 * [showAddressError] parameter in Step 2.
 *
 * ======================================================================================
 * PRIVACY CONSIDERATIONS
 * ======================================================================================
 * Crypto addresses are sensitive — sharing the same address across trades creates a
 * linkability risk. The Step 3 review should warn Monero users to consider using
 * unique sub-addresses per trade for maximum privacy. This warning is included in
 * Step 3's copy but only shown for XMR.
 *
 * ======================================================================================
 * I18N CONSIDERATIONS
 * ======================================================================================
 * New i18n keys needed:
 *   mobile.paymentAccounts.crypto.selectType.title
 *   mobile.paymentAccounts.crypto.selectType.moneroDescription
 *   mobile.paymentAccounts.crypto.selectType.otherDescription
 *   mobile.paymentAccounts.crypto.form.accountName.label
 *   mobile.paymentAccounts.crypto.form.address.label
 *   mobile.paymentAccounts.crypto.form.cryptoName.label
 *   mobile.paymentAccounts.crypto.form.address.hint.monero
 *   mobile.paymentAccounts.crypto.form.address.hint.other
 *   mobile.paymentAccounts.crypto.review.privacyHint (XMR only)
 *   mobile.paymentAccounts.crypto.form.address.error.invalid
 *
 * "Monero" and "XMR" are proper nouns — do NOT translate them in i18n files.
 * Crypto currency names should always render in their original form.
 */
package network.bisq.mobile.presentation.design.payment_accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethodIcon
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.ui.tooling.preview.Preview

// -------------------------------------------------------------------------------------
// Crypto type model
// -------------------------------------------------------------------------------------

/**
 * The two crypto account types available in Bisq Easy.
 * Monero is a distinct type because:
 *   1. It has a defined address format we can validate
 *   2. It deserves privacy-specific UX copy (sub-address recommendation)
 *   3. It is the dominant crypto settlement method on Bisq Easy
 */
enum class CryptoAccountType {
    MONERO,
    OTHER,
}

// -------------------------------------------------------------------------------------
// Step 1: Select Crypto Type
// -------------------------------------------------------------------------------------

/**
 * Step 1 content — choose between Monero and Other Crypto.
 *
 * Layout:
 *   Heading + sub-copy explaining the two options
 *   Two large type-selection cards stacked vertically:
 *     [XMR icon] "Monero (XMR)" + privacy description
 *     [custom icon] "Other Cryptocurrency" + flexibility description
 *
 * Card design rationale: two large tappable cards provide:
 *   - Clear visual separation between the two options
 *   - Enough surface area to include a description blurb under each title
 *   - Clear selected state (primaryDim background, matching the payment method rows)
 *
 * Unlike the fiat wizard's Step 1 (which has a searchable list), this step has a
 * maximum of two choices — presenting them as cards removes the need for any
 * search or filter affordance.
 *
 * @param selectedType Currently selected crypto type (null if none selected)
 * @param onTypeSelect Called when the user taps a type card
 */
@Composable
fun CreateCryptoAccount_Step1_SelectType(
    selectedType: CryptoAccountType?,
    onTypeSelect: (CryptoAccountType) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H3Light("Select crypto type")
        BisqText.SmallLight(
            "Choose the type of cryptocurrency account you want to add.",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.V1()

        // Monero option card
        CryptoTypeCard(
            cryptoType = CryptoAccountType.MONERO,
            methodId = "XMR",
            title = "Monero (XMR)",
            description =
                "Privacy-preserving cryptocurrency. Recommended for Bisq Easy trades. " +
                    "Use a sub-address for each trade to avoid address reuse.",
            isSelected = selectedType == CryptoAccountType.MONERO,
            onSelect = { onTypeSelect(CryptoAccountType.MONERO) },
        )

        // Other Crypto option card
        CryptoTypeCard(
            cryptoType = CryptoAccountType.OTHER,
            methodId = "OTHER",
            title = "Other Cryptocurrency",
            description =
                "Add an account for any other supported cryptocurrency. " +
                    "You will specify the currency name and receiving address.",
            isSelected = selectedType == CryptoAccountType.OTHER,
            onSelect = { onTypeSelect(CryptoAccountType.OTHER) },
        )

        BisqGap.V1()

        // Context note — why only Monero + Other?
        BisqText.SmallLight(
            "Note: Bisq Easy currently supports crypto-to-fiat trades. " +
                "The crypto side is where you receive the asset you are buying, " +
                "or send from when you are selling.",
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

/**
 * A large selection card for a crypto account type.
 *
 * Wider than a payment method row — includes a description blurb, appropriate for
 * a choice between two fundamentally different account types rather than between
 * many similar items in a list.
 *
 * Selected state: primaryDim background + primary-colored border (1dp).
 * Unselected state: dark_grey40 background, no border.
 *
 * The primary-colored border on selection adds a secondary selection signal that
 * remains visible even on OLED screens where the primaryDim background might be
 * subtle.
 */
@Composable
private fun CryptoTypeCard(
    cryptoType: CryptoAccountType,
    methodId: String,
    title: String,
    description: String,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = if (isSelected) BisqTheme.colors.primaryDim else BisqTheme.colors.dark_grey40,
        border =
            if (isSelected) {
                androidx.compose.foundation.BorderStroke(1.dp, BisqTheme.colors.primary)
            } else {
                null
            },
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(BisqUIConstants.ScreenPadding2X),
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding2X),
        ) {
            // Icon — settlement icon for crypto (isPaymentMethod = false)
            PaymentMethodIcon(
                methodId = methodId,
                isPaymentMethod = false,
                size = 40.dp,
                contentDescription = title,
            )

            Column(modifier = Modifier.weight(1f)) {
                BisqText.BaseRegular(title)
                BisqGap.VHalf()
                BisqText.SmallLight(
                    description,
                    color = BisqTheme.colors.mid_grey20,
                )
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Step 2: Enter Address
// -------------------------------------------------------------------------------------

/**
 * Step 2 content — account name and wallet address entry.
 *
 * For MONERO: shows account name + Monero wallet address field with XMR-specific hint.
 * For OTHER: shows account name + crypto currency name field + wallet address field.
 *
 * The "Other" form needs a crypto name field (e.g. "Litecoin") because Bisq2's
 * OtherCryptoAccountPayload stores the currency code alongside the address.
 *
 * Address field is always multi-line (minLines = 2) because crypto addresses are
 * long and wrapping within the field prevents horizontal overflow. The user should
 * be able to see the full address without horizontal scrolling.
 *
 * Address validation: [showAddressError] controls whether an error state is shown.
 * Real validation is format-dependent (XMR: 95 chars, prefix 4 or 8; other: varies).
 *
 * @param cryptoType The type selected in Step 1
 * @param accountName Current account name value
 * @param cryptoName Current crypto name value (only relevant for OTHER type)
 * @param address Current wallet address value
 * @param showAddressError Whether to display an address format error
 * @param onAccountNameChange Called when account name field changes
 * @param onCryptoNameChange Called when crypto name field changes
 * @param onAddressChange Called when address field changes
 */
@Composable
fun CreateCryptoAccount_Step2_EnterAddress(
    cryptoType: CryptoAccountType,
    accountName: String,
    cryptoName: String,
    address: String,
    showAddressError: Boolean,
    onAccountNameChange: (String) -> Unit,
    onCryptoNameChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H3Light(
            if (cryptoType == CryptoAccountType.MONERO) {
                "Monero account details"
            } else {
                "Crypto account details"
            },
        )

        BisqText.SmallLight(
            if (cryptoType == CryptoAccountType.MONERO) {
                "Enter a name for this account and your Monero receiving address."
            } else {
                "Enter the cryptocurrency name and the wallet address where you will receive it."
            },
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        // Account name — appears for both types
        BisqTextFieldV0(
            value = accountName,
            onValueChange = onAccountNameChange,
            label = "Account Name",
            placeholder =
                if (cryptoType == CryptoAccountType.MONERO) {
                    "e.g. My XMR Wallet"
                } else {
                    "e.g. Litecoin Hot Wallet"
                },
        )

        // Crypto name — only for OTHER type
        if (cryptoType == CryptoAccountType.OTHER) {
            BisqTextFieldV0(
                value = cryptoName,
                onValueChange = onCryptoNameChange,
                label = "Cryptocurrency",
                placeholder = "e.g. Litecoin",
            )
        }

        // Wallet address
        BisqTextFieldV0(
            value = address,
            onValueChange = onAddressChange,
            label =
                if (cryptoType == CryptoAccountType.MONERO) {
                    "Monero Address"
                } else {
                    "Wallet Address"
                },
            placeholder =
                if (cryptoType == CryptoAccountType.MONERO) {
                    "4... or 8... (95 characters)"
                } else {
                    "Paste your receiving address"
                },
            minLines = 2,
            isError = showAddressError,
            bottomMessage =
                if (showAddressError) {
                    "Invalid address format. Please check and try again."
                } else {
                    null
                },
        )

        // Method-specific hints
        BisqText.SmallLight(
            if (cryptoType == CryptoAccountType.MONERO) {
                "Tip: For better privacy, consider using a unique sub-address for each trade. " +
                    "Sub-addresses start with '8' and are generated by your Monero wallet."
            } else {
                "Make sure to enter the correct address for the selected cryptocurrency. " +
                    "Sending to the wrong address type will result in permanent loss of funds."
            },
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

// -------------------------------------------------------------------------------------
// Step 3: Review & Confirm
// -------------------------------------------------------------------------------------

/**
 * Step 3 content — review entered crypto account data before saving.
 *
 * Presents all entered data in a read-only BisqCard. For Monero accounts,
 * an additional privacy reminder is shown about address reuse risk.
 *
 * The address is shown in FULL (not truncated) in the review step. The user must
 * verify the complete address before confirming — truncation here would defeat
 * the purpose of a review step.
 *
 * @param cryptoType The type selected in Step 1
 * @param accountName Entered account name
 * @param cryptoName Entered crypto name (OTHER type only)
 * @param address Full wallet address
 */
@Composable
fun CreateCryptoAccount_Step3_Review(
    cryptoType: CryptoAccountType,
    accountName: String,
    cryptoName: String,
    address: String,
) {
    val displayCryptoName = if (cryptoType == CryptoAccountType.MONERO) "Monero (XMR)" else cryptoName

    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H3Light("Review account")
        BisqText.SmallLight(
            "Confirm your account details before saving.",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        BisqCard {
            // Crypto type header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PaymentMethodIcon(
                    methodId = if (cryptoType == CryptoAccountType.MONERO) "XMR" else "OTHER",
                    isPaymentMethod = false,
                    size = BisqUIConstants.ScreenPadding2X,
                    contentDescription = displayCryptoName,
                )
                BisqText.BaseRegular(displayCryptoName)
            }

            BisqGap.V1()

            // Account name
            CryptoReviewRow(label = "Account Name", value = accountName)
            BisqGap.VHalf()

            // Crypto name (Other only)
            if (cryptoType == CryptoAccountType.OTHER) {
                CryptoReviewRow(label = "Cryptocurrency", value = cryptoName)
                BisqGap.VHalf()
            }

            // Full address — untruncated for verification
            CryptoReviewRow(
                label = if (cryptoType == CryptoAccountType.MONERO) "Monero Address" else "Wallet Address",
                value = address,
            )
        }

        BisqGap.V1()

        // Monero-specific privacy hint
        if (cryptoType == CryptoAccountType.MONERO) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                color = BisqTheme.colors.warning.copy(alpha = 0.10f),
            ) {
                Column(
                    modifier = Modifier.padding(BisqUIConstants.ScreenPadding),
                    verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
                ) {
                    BisqText.SmallRegular(
                        "Privacy Reminder",
                        color = BisqTheme.colors.warning,
                    )
                    BisqText.SmallLight(
                        "Using the same Monero address for multiple trades can allow an observer " +
                            "to link your trades together. Consider generating a unique sub-address " +
                            "per trade in your Monero wallet for maximum privacy.",
                        color = BisqTheme.colors.mid_grey20,
                    )
                }
            }
        }

        BisqGap.V1()

        BisqText.SmallLight(
            "This account will be saved to your device. You can edit or delete it from " +
                "Payment Accounts at any time.",
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

/**
 * A labeled key-value row in the crypto review summary card.
 */
@Composable
private fun CryptoReviewRow(
    label: String,
    value: String,
) {
    Column {
        BisqText.SmallLight(label, color = BisqTheme.colors.mid_grey20)
        BisqGap.VQuarter()
        BisqText.BaseRegular(value)
    }
}

// -------------------------------------------------------------------------------------
// Full wizard composables (for @Preview)
// -------------------------------------------------------------------------------------

/**
 * Full wizard shell for Step 1 — crypto type selection.
 */
@Composable
private fun CreateCryptoAccountWizard_Step1Preview(
    initialSelectedType: CryptoAccountType? = null,
) {
    var selectedType by remember { mutableStateOf(initialSelectedType) }

    MultiScreenWizardScaffold(
        title = "Add Crypto Account",
        stepIndex = 1,
        stepsLength = 3,
        nextButtonText = "Next",
        nextDisabled = selectedType == null,
        nextOnClick = { /* preview no-op */ },
        prevDisabled = true,
        prevOnClick = null,
        closeAction = true,
    ) {
        CreateCryptoAccount_Step1_SelectType(
            selectedType = selectedType,
            onTypeSelect = { selectedType = it },
        )
    }
}

/**
 * Full wizard shell for Step 2 — Monero form.
 */
@Composable
private fun CreateCryptoAccountWizard_Step2MoneroPreview() {
    var accountName by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }

    MultiScreenWizardScaffold(
        title = "Add Crypto Account",
        stepIndex = 2,
        stepsLength = 3,
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        CreateCryptoAccount_Step2_EnterAddress(
            cryptoType = CryptoAccountType.MONERO,
            accountName = accountName,
            cryptoName = "XMR",
            address = address,
            showAddressError = false,
            onAccountNameChange = { accountName = it },
            onCryptoNameChange = {},
            onAddressChange = { address = it },
        )
    }
}

/**
 * Full wizard shell for Step 2 — Other Crypto form with error state.
 */
@Composable
private fun CreateCryptoAccountWizard_Step2OtherPreview() {
    var accountName by remember { mutableStateOf("Litecoin Hot Wallet") }
    var cryptoName by remember { mutableStateOf("Litecoin") }
    var address by remember { mutableStateOf("invalid-address-entered-by-mistake") }

    MultiScreenWizardScaffold(
        title = "Add Crypto Account",
        stepIndex = 2,
        stepsLength = 3,
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        CreateCryptoAccount_Step2_EnterAddress(
            cryptoType = CryptoAccountType.OTHER,
            accountName = accountName,
            cryptoName = cryptoName,
            address = address,
            showAddressError = true,
            onAccountNameChange = { accountName = it },
            onCryptoNameChange = { cryptoName = it },
            onAddressChange = { address = it },
        )
    }
}

/**
 * Full wizard shell for Step 3 — Monero review.
 */
@Composable
private fun CreateCryptoAccountWizard_Step3MoneroPreview() {
    MultiScreenWizardScaffold(
        title = "Add Crypto Account",
        stepIndex = 3,
        stepsLength = 3,
        nextButtonText = "Confirm",
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        CreateCryptoAccount_Step3_Review(
            cryptoType = CryptoAccountType.MONERO,
            accountName = "Cold Storage XMR",
            cryptoName = "XMR",
            address = "49A6bqH8sDLxpzymNFVPMzxCRnzN1FUkBHmELFUmBz3mRTymR9R9yQcEgAf6WkqmhVm6FBbQ9Fhm",
        )
    }
}

// -------------------------------------------------------------------------------------
// @Preview functions
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateCryptoWizard_Step1_DefaultPreview() {
    BisqTheme.Preview {
        CreateCryptoAccountWizard_Step1Preview()
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateCryptoWizard_Step1_MoneroSelectedPreview() {
    BisqTheme.Preview {
        CreateCryptoAccountWizard_Step1Preview(
            initialSelectedType = CryptoAccountType.MONERO,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateCryptoWizard_Step2_MoneroPreview() {
    BisqTheme.Preview {
        CreateCryptoAccountWizard_Step2MoneroPreview()
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateCryptoWizard_Step2_OtherWithErrorPreview() {
    BisqTheme.Preview {
        CreateCryptoAccountWizard_Step2OtherPreview()
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateCryptoWizard_Step3_MoneroReviewPreview() {
    BisqTheme.Preview {
        CreateCryptoAccountWizard_Step3MoneroPreview()
    }
}
