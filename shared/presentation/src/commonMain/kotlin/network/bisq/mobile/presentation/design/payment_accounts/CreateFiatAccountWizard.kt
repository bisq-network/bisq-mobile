/**
 * CreateFiatAccountWizard.kt — Design PoC (Issue #991)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 * This feature is INTENTIONALLY HIDDEN until the MuSig protocol is finalized in Bisq2.
 * Do not expose this screen via any navigation route until MuSig is production-ready.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Mobile-adapted wizard for creating a fiat payment account. Covers the full creation
 * flow from payment method selection through account data entry to a review summary.
 *
 * The wizard is composed of three steps rendered in a MultiScreenWizardScaffold:
 *   Step 1 — Select Payment Method
 *   Step 2 — Enter Account Data
 *   Step 3 — Review & Confirm
 *
 * ======================================================================================
 * DESKTOP ADAPTATION
 * ======================================================================================
 * Desktop has a 4-step wizard:
 *   1. Select payment method (table: name | currencies | countries | chargeback risk)
 *   2. Enter method-specific account data
 *   3. Payment options (optional: which currencies/countries to offer)
 *   4. Summary & confirm
 *
 * Mobile merges steps 3 and 4 by embedding the optional payment options inline on the
 * account data step. Rationale:
 *   - Most users on mobile don't customize per-trade currency/country filters
 *   - Four full-screen wizard steps feels heavy for a settings task (compare: iOS
 *     Contacts app creates a contact in one scrollable form)
 *   - The merged step 2 shows fields progressively (advanced options collapsed by default)
 *
 * Step 1 adaptation: desktop shows a sortable table. Mobile replaces this with:
 *   - A BisqSearchField at the top for text search
 *   - Filter chips below the search field for risk level (All / Very Low / Low / Moderate)
 *   - Vertical list of PaymentTypeCard rows (existing component, consistent with
 *     the create-offer wizard's payment method selection step)
 *
 * Step 2 adaptation: desktop renders a method-specific form via polymorphism (each
 * payment method has its own Form class). Mobile POC shows two representative forms:
 *   - CUSTOM: matches current behavior (name + free-text accountData)
 *   - Structured (e.g., SEPA): account holder name + account identifier field
 *     with method-specific label and keyboard type (IBAN → text, email → email, etc.)
 *
 * ======================================================================================
 * PAYMENT METHOD CATALOGUE (Step 1 data)
 * ======================================================================================
 * The POC hardcodes a representative subset of FiatPaymentRailEnum values:
 *   SEPA, SEPA_INSTANT, ZELLE, REVOLUT, WISE, NATIONAL_BANK, ACH_TRANSFER,
 *   PIX, FASTER_PAYMENTS, STRIKE, CASH_APP, INTERAC_E_TRANSFER, BIZUM, CUSTOM
 *
 * Each entry carries: methodId, displayName, currencies, chargebackRisk.
 * In production, this list is driven by the backend's supported payment rails.
 *
 * ======================================================================================
 * CHARGEBACK RISK IN STEP 1
 * ======================================================================================
 * Chargeback risk is displayed as a color-coded badge on each row in Step 1. This
 * surfaces the risk BEFORE the user commits to a payment method, matching the intent
 * of the desktop table but surfacing it more prominently.
 *
 * Decision: show risk in the selection list, NOT hidden in a detail view. Users must
 * see risk level when choosing a method, not after entering account data.
 *
 * ======================================================================================
 * I18N CONSIDERATIONS
 * ======================================================================================
 * - Step headings ("Select Payment Method", "Account Details", "Review") are short.
 * - Payment method names are rendered via i18NPaymentMethod utility in production.
 *   POC hardcodes English; real implementation uses the existing payment method i18n
 *   infrastructure from the create-offer wizard.
 * - Form field labels ("Account Name", "Account Holder", "IBAN / Account Number") will
 *   need method-specific i18n keys. Consider a namespace:
 *     mobile.paymentAccounts.form.{methodId}.holderLabel
 *     mobile.paymentAccounts.form.{methodId}.identifierLabel
 *     mobile.paymentAccounts.form.{methodId}.identifierHint
 * - Risk filter chip labels need 3 i18n keys (see PaymentAccountCard.kt)
 * - Search field placeholder: re-use existing "action.search" key
 * - "Back" and "Next" buttons inherit from MultiScreenWizardScaffold defaults
 *   ("action.back" / "action.next") — already localized in 14 languages.
 */
package network.bisq.mobile.presentation.design.payment_accounts

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqCard
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqTextFieldV0
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.MultiScreenWizardScaffold
import network.bisq.mobile.presentation.common.ui.components.molecules.PaymentMethodIcon
import network.bisq.mobile.presentation.common.ui.components.molecules.inputfield.BisqSearchField
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.ui.tooling.preview.Preview

// -------------------------------------------------------------------------------------
// Simulated data for preview purposes
// -------------------------------------------------------------------------------------

/**
 * Risk-level filter options for Step 1 search/filter row.
 * "ALL" means no filter applied.
 */
enum class RiskFilter {
    ALL,
    VERY_LOW,
    LOW,
    MODERATE,
}

/**
 * Simulated payment method entry for the Step 1 selection list.
 * Production implementation sources this from FiatPaymentRailEnum + backend API.
 */
data class SimulatedPaymentMethod(
    val methodId: String,
    val displayName: String,
    val currencies: List<String>,
    val chargebackRisk: SimulatedChargebackRisk,
)

/**
 * Simulated form field descriptor for Step 2.
 * In production each payment method has a specific form schema.
 */
data class SimulatedFormField(
    val fieldId: String,
    val label: String,
    val placeholder: String,
    val isMultiLine: Boolean = false,
)

// Sample data used across multiple preview variants
private val allPaymentMethods =
    listOf(
        SimulatedPaymentMethod("SEPA", "SEPA", listOf("EUR"), SimulatedChargebackRisk.VERY_LOW),
        SimulatedPaymentMethod("SEPA_INSTANT", "SEPA Instant", listOf("EUR"), SimulatedChargebackRisk.VERY_LOW),
        SimulatedPaymentMethod("FASTER_PAYMENTS", "Faster Payments", listOf("GBP"), SimulatedChargebackRisk.VERY_LOW),
        SimulatedPaymentMethod("PIX", "PIX", listOf("BRL"), SimulatedChargebackRisk.VERY_LOW),
        SimulatedPaymentMethod("BIZUM", "Bizum", listOf("EUR"), SimulatedChargebackRisk.VERY_LOW),
        SimulatedPaymentMethod("REVOLUT", "Revolut", listOf("EUR", "USD", "GBP"), SimulatedChargebackRisk.LOW),
        SimulatedPaymentMethod("WISE", "Wise", listOf("EUR", "USD", "GBP"), SimulatedChargebackRisk.LOW),
        SimulatedPaymentMethod("INTERAC_E_TRANSFER", "Interac e-Transfer", listOf("CAD"), SimulatedChargebackRisk.LOW),
        SimulatedPaymentMethod("STRIKE", "Strike", listOf("USD"), SimulatedChargebackRisk.LOW),
        SimulatedPaymentMethod("ZELLE", "Zelle", listOf("USD"), SimulatedChargebackRisk.MODERATE),
        SimulatedPaymentMethod("ACH_TRANSFER", "ACH Transfer", listOf("USD"), SimulatedChargebackRisk.MODERATE),
        SimulatedPaymentMethod("CASH_APP", "Cash App", listOf("USD"), SimulatedChargebackRisk.MODERATE),
        SimulatedPaymentMethod("NATIONAL_BANK", "National Bank Transfer", listOf("*"), SimulatedChargebackRisk.LOW),
        SimulatedPaymentMethod("CUSTOM", "Custom", listOf("*"), SimulatedChargebackRisk.LOW),
    )

private val sepaFormFields =
    listOf(
        SimulatedFormField("accountHolder", "Account Holder Name", "Full name as on bank account"),
        SimulatedFormField("iban", "IBAN", "e.g. DE89 3704 0044 0532 0130 00"),
        SimulatedFormField("bic", "BIC / SWIFT", "e.g. COBADEFFXXX"),
    )

private val customFormFields =
    listOf(
        SimulatedFormField("accountName", "Account Name", "e.g. My PayPal"),
        SimulatedFormField(
            "accountData",
            "Account Data",
            "Enter your payment details here…",
            isMultiLine = true,
        ),
    )

// -------------------------------------------------------------------------------------
// Step 1: Select Payment Method
// -------------------------------------------------------------------------------------

/**
 * Step 1 content — payment method selection list with search and risk filter.
 *
 * Layout:
 *   BisqSearchField (full width)
 *   FlowRow of risk filter chips: [All] [Very Low] [Low] [Moderate]
 *   Vertical list of PaymentTypeCard rows (one per matching method)
 *
 * The filter chips use RiskFilterChip with outline/active state toggling.
 * The "All" chip acts as a reset — selecting any risk-level chip deactivates "All",
 * and selecting "All" clears the risk-level selection.
 *
 * PaymentMethodSelectionRow extends the PaymentTypeCard visual pattern by adding a
 * trailing chargeback-risk badge. This avoids modifying the shared PaymentTypeCard
 * component while providing risk context inline.
 *
 * @param methods Full list of available payment methods to display
 * @param searchQuery Current search query string
 * @param activeRiskFilter Currently active risk filter
 * @param selectedMethodId Currently selected method ID (null if none selected)
 * @param onSearchChange Called when the user types in the search field
 * @param onRiskFilterChange Called when the user selects a risk filter chip
 * @param onMethodSelect Called with the methodId when user selects a method row
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CreateFiatAccount_Step1_SelectMethod(
    methods: List<SimulatedPaymentMethod>,
    searchQuery: String,
    activeRiskFilter: RiskFilter,
    selectedMethodId: String?,
    onSearchChange: (String) -> Unit,
    onRiskFilterChange: (RiskFilter) -> Unit,
    onMethodSelect: (String) -> Unit,
) {
    val filteredMethods =
        methods
            .filter { method ->
                (searchQuery.isBlank() || method.displayName.contains(searchQuery, ignoreCase = true)) &&
                    (
                        activeRiskFilter == RiskFilter.ALL || method.chargebackRisk ==
                            when (activeRiskFilter) {
                                RiskFilter.VERY_LOW -> SimulatedChargebackRisk.VERY_LOW
                                RiskFilter.LOW -> SimulatedChargebackRisk.LOW
                                RiskFilter.MODERATE -> SimulatedChargebackRisk.MODERATE
                                RiskFilter.ALL -> null
                            }
                    )
            }

    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H3Light("Select a payment method")
        BisqText.SmallLight(
            "Choose how your trading partner will send you fiat. " +
                "Methods with lower chargeback risk are preferred.",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        // Search field
        BisqSearchField(
            value = searchQuery,
            onValueChange = { value, _ -> onSearchChange(value) },
            placeholder = "Search payment methods",
        )

        // Risk filter chips
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            RiskFilterChip(
                label = "All",
                isSelected = activeRiskFilter == RiskFilter.ALL,
                onClick = { onRiskFilterChange(RiskFilter.ALL) },
            )
            RiskFilterChip(
                label = "Very Low Risk",
                isSelected = activeRiskFilter == RiskFilter.VERY_LOW,
                color = BisqTheme.colors.primary,
                onClick = { onRiskFilterChange(RiskFilter.VERY_LOW) },
            )
            RiskFilterChip(
                label = "Low Risk",
                isSelected = activeRiskFilter == RiskFilter.LOW,
                color = BisqTheme.colors.warning,
                onClick = { onRiskFilterChange(RiskFilter.LOW) },
            )
            RiskFilterChip(
                label = "Moderate Risk",
                isSelected = activeRiskFilter == RiskFilter.MODERATE,
                color = BisqTheme.colors.danger,
                onClick = { onRiskFilterChange(RiskFilter.MODERATE) },
            )
        }

        BisqGap.VHalf()

        // Payment method list
        if (filteredMethods.isEmpty()) {
            BisqText.BaseLight(
                "No payment methods match your search.",
                color = BisqTheme.colors.mid_grey20,
            )
        } else {
            Column(
                verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
            ) {
                filteredMethods.forEach { method ->
                    PaymentMethodSelectionRow(
                        method = method,
                        isSelected = method.methodId == selectedMethodId,
                        onSelect = { onMethodSelect(method.methodId) },
                    )
                }
            }
        }
    }
}

/**
 * A single row in the payment method selection list.
 *
 * Extends PaymentTypeCard's visual pattern by adding a trailing chargeback risk
 * badge. The badge provides the critical risk signal at the point of selection —
 * it should not require the user to tap into a detail view to discover risk level.
 *
 * The row uses dark_grey50 background (same as PaymentTypeCard) and primaryDim
 * when selected for consistency with the create-offer wizard.
 */
@Composable
private fun PaymentMethodSelectionRow(
    method: SimulatedPaymentMethod,
    isSelected: Boolean,
    onSelect: () -> Unit,
) {
    val riskColor =
        when (method.chargebackRisk) {
            SimulatedChargebackRisk.VERY_LOW -> BisqTheme.colors.primary
            SimulatedChargebackRisk.LOW -> BisqTheme.colors.warning
            SimulatedChargebackRisk.MODERATE -> BisqTheme.colors.danger
        }

    // Outer clickable card — wraps PaymentTypeCard pattern but adds trailing badge
    Surface(
        onClick = onSelect,
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = if (isSelected) BisqTheme.colors.primaryDim else BisqTheme.colors.dark_grey50,
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(
                        horizontal = BisqUIConstants.ScreenPadding2X,
                        vertical = BisqUIConstants.ScreenPadding,
                    ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
        ) {
            // Method icon (20dp — consistent with create-offer payment step)
            PaymentMethodIcon(
                methodId = method.methodId,
                isPaymentMethod = true,
                size = BisqUIConstants.ScreenPadding2X,
                contentDescription = method.displayName,
            )

            // Method name + currencies
            Column(modifier = Modifier.weight(1f)) {
                BisqText.BaseRegular(method.displayName)
                if (method.currencies.isNotEmpty() && method.currencies != listOf("*")) {
                    BisqText.SmallLight(
                        method.currencies.joinToString(", "),
                        color = BisqTheme.colors.mid_grey20,
                    )
                }
            }

            // Chargeback risk badge (trailing)
            Surface(
                shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
                color = riskColor.copy(alpha = 0.15f),
            ) {
                BisqText.SmallRegular(
                    text =
                        when (method.chargebackRisk) {
                            SimulatedChargebackRisk.VERY_LOW -> "Very Low"
                            SimulatedChargebackRisk.LOW -> "Low"
                            SimulatedChargebackRisk.MODERATE -> "Moderate"
                        },
                    color = riskColor,
                    modifier =
                        Modifier.padding(
                            horizontal = BisqUIConstants.ScreenPaddingHalf,
                            vertical = BisqUIConstants.ScreenPaddingQuarter,
                        ),
                )
            }
        }
    }
}

/**
 * A filter chip for risk-level filtering in Step 1.
 *
 * Selected state uses a filled background (color at 20% alpha) + colored text.
 * Unselected state uses dark_grey40 background + mid_grey20 text.
 * Color parameter defaults to white for the "All" chip.
 */
@Composable
private fun RiskFilterChip(
    label: String,
    isSelected: Boolean,
    color: Color = BisqTheme.colors.white,
    onClick: () -> Unit,
) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(BisqUIConstants.BorderRadius),
        color = if (isSelected) color.copy(alpha = 0.15f) else BisqTheme.colors.dark_grey40,
    ) {
        BisqText.SmallRegular(
            label,
            color = if (isSelected) color else BisqTheme.colors.mid_grey20,
            modifier =
                Modifier.padding(
                    horizontal = BisqUIConstants.ScreenPadding,
                    vertical = BisqUIConstants.ScreenPaddingHalf,
                ),
        )
    }
}

// -------------------------------------------------------------------------------------
// Step 2: Account Data Form
// -------------------------------------------------------------------------------------

/**
 * Step 2 content — method-specific account data entry form.
 *
 * The form is parameterized by [formFields] — a list of [SimulatedFormField] descriptors.
 * Each field descriptor drives one BisqTextFieldV0 input with appropriate label,
 * placeholder, and multi-line configuration.
 *
 * This approach allows the same composable to render both CUSTOM (2 fields) and
 * structured methods like SEPA (3 fields: holder name, IBAN, BIC) without branching.
 * In production, the field descriptors are generated from the selected payment method's
 * form schema (adapting Bisq2's PaymentAccountFormFactory pattern).
 *
 * Above the form, a summary row shows the selected method's icon and name. This gives
 * the user a clear "you are creating a SEPA account" confirmation at step 2 entry,
 * reducing confusion if they navigated back and changed method.
 *
 * @param selectedMethodDisplayName Display name of the selected payment method
 * @param selectedMethodId ID of the selected payment method (for icon)
 * @param formFields Ordered list of form fields to render
 * @param fieldValues Current value for each field (keyed by fieldId)
 * @param onFieldChange Called with (fieldId, newValue) when a field changes
 */
@Composable
fun CreateFiatAccount_Step2_AccountData(
    selectedMethodDisplayName: String,
    selectedMethodId: String,
    formFields: List<SimulatedFormField>,
    fieldValues: Map<String, String>,
    onFieldChange: (String, String) -> Unit,
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
    ) {
        BisqText.H3Light("Account details")
        BisqText.SmallLight(
            "Enter the details for your $selectedMethodDisplayName account. " +
                "These will be shared with your trading partner when you confirm a trade.",
            color = BisqTheme.colors.mid_grey20,
        )

        BisqGap.VHalf()

        // Selected method recap — confirmation the user is on the right method
        BisqCard(
            backgroundColor = BisqTheme.colors.dark_grey50,
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
            ) {
                PaymentMethodIcon(
                    methodId = selectedMethodId,
                    isPaymentMethod = true,
                    size = BisqUIConstants.ScreenPadding2X,
                    contentDescription = selectedMethodDisplayName,
                )
                BisqText.BaseRegular(selectedMethodDisplayName)
            }
        }

        BisqGap.VHalf()

        // Dynamic form fields
        formFields.forEach { field ->
            BisqTextFieldV0(
                value = fieldValues[field.fieldId] ?: "",
                onValueChange = { onFieldChange(field.fieldId, it) },
                label = field.label,
                placeholder = field.placeholder,
                minLines = if (field.isMultiLine) 4 else 1,
            )
            BisqGap.VHalf()
        }

        // Privacy note — always shown, regardless of method
        // Users should understand account data is stored locally only
        BisqText.SmallLight(
            "Your account details are stored locally on your device and encrypted at rest. " +
                "They are only shared with your trading partner after you initiate a trade.",
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

// -------------------------------------------------------------------------------------
// Step 3: Review & Confirm
// -------------------------------------------------------------------------------------

/**
 * Step 3 content — summary of all entered account data before saving.
 *
 * Presents a read-only BisqCard with all field values. The user can tap "Back" to
 * return to Step 2 and correct data, or "Confirm" (via the wizard's Next button) to save.
 *
 * The summary also re-shows the chargeback risk badge as a final reminder.
 * This is the last opportunity to educate the user before the account is persisted.
 *
 * Desktop analogy: Bisq2's "Summary" step in the payment account creation wizard.
 *
 * @param selectedMethod The payment method chosen in Step 1
 * @param formFields Field descriptors (for labels)
 * @param fieldValues Entered values keyed by fieldId
 */
@Composable
fun CreateFiatAccount_Step3_Review(
    selectedMethod: SimulatedPaymentMethod,
    formFields: List<SimulatedFormField>,
    fieldValues: Map<String, String>,
) {
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
            // Method header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPadding),
                modifier = Modifier.fillMaxWidth(),
            ) {
                PaymentMethodIcon(
                    methodId = selectedMethod.methodId,
                    isPaymentMethod = true,
                    size = BisqUIConstants.ScreenPadding2X,
                    contentDescription = selectedMethod.displayName,
                )
                Column(modifier = Modifier.weight(1f)) {
                    BisqText.BaseRegular(selectedMethod.displayName)
                    if (selectedMethod.currencies.isNotEmpty() && selectedMethod.currencies != listOf("*")) {
                        BisqText.SmallLight(
                            selectedMethod.currencies.joinToString(", "),
                            color = BisqTheme.colors.mid_grey20,
                        )
                    }
                }
            }

            BisqGap.V1()

            // Field summary rows
            formFields.forEach { field ->
                ReviewFieldRow(
                    label = field.label,
                    value = fieldValues[field.fieldId] ?: "—",
                )
                BisqGap.VHalf()
            }

            BisqGap.VHalf()

            // Chargeback risk — final reminder
            ChargebackRiskBadge(risk = selectedMethod.chargebackRisk)
        }

        BisqGap.V1()

        BisqText.SmallLight(
            "Tapping Confirm will save this account to your device. " +
                "You can edit or delete it from the Payment Accounts screen at any time.",
            color = BisqTheme.colors.mid_grey30,
        )
    }
}

/**
 * A labeled key-value row in the review summary card.
 * Label in subdued grey (SmallLight), value in white (BaseRegular).
 */
@Composable
private fun ReviewFieldRow(
    label: String,
    value: String,
) {
    Column {
        BisqText.SmallLight(label, color = BisqTheme.colors.mid_grey20)
        BisqGap.VQuarter()
        BisqText.BaseRegular(value)
    }
}

// ChargebackRiskBadge is defined in PaymentAccountCard.kt and used here via internal visibility.

// -------------------------------------------------------------------------------------
// Full wizard composables (for @Preview)
// -------------------------------------------------------------------------------------

/**
 * Full wizard shell for Step 1 — used in previews and production entry.
 * State is intentionally local (remember) since this is a stateless POC.
 * Production implementation hoists state to a presenter.
 */
@Composable
private fun CreateFiatAccountWizard_Step1Preview(
    methods: List<SimulatedPaymentMethod> = allPaymentMethods,
    initialSearch: String = "",
    initialFilter: RiskFilter = RiskFilter.ALL,
    initialSelectedMethodId: String? = null,
) {
    var searchQuery by remember { mutableStateOf(initialSearch) }
    var activeFilter by remember { mutableStateOf(initialFilter) }
    var selectedMethodId by remember { mutableStateOf(initialSelectedMethodId) }

    // Clear selection when filter/search hides the selected method
    val visibleMethodIds =
        methods
            .filter { method ->
                (searchQuery.isBlank() || method.displayName.contains(searchQuery, ignoreCase = true)) &&
                    (
                        activeFilter == RiskFilter.ALL || method.chargebackRisk ==
                            when (activeFilter) {
                                RiskFilter.VERY_LOW -> SimulatedChargebackRisk.VERY_LOW
                                RiskFilter.LOW -> SimulatedChargebackRisk.LOW
                                RiskFilter.MODERATE -> SimulatedChargebackRisk.MODERATE
                                RiskFilter.ALL -> null
                            }
                    )
            }.map { it.methodId }
            .toSet()

    if (selectedMethodId != null && selectedMethodId !in visibleMethodIds) {
        selectedMethodId = null
    }

    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 1,
        stepsLength = 3,
        nextButtonText = "Next",
        nextDisabled = selectedMethodId == null,
        nextOnClick = { /* preview no-op */ },
        prevDisabled = true,
        prevOnClick = null,
        closeAction = true,
    ) {
        CreateFiatAccount_Step1_SelectMethod(
            methods = methods,
            searchQuery = searchQuery,
            activeRiskFilter = activeFilter,
            selectedMethodId = selectedMethodId,
            onSearchChange = { searchQuery = it },
            onRiskFilterChange = { activeFilter = it },
            onMethodSelect = { selectedMethodId = it },
        )
    }
}

/**
 * Full wizard shell for Step 2 — custom method form.
 */
@Composable
private fun CreateFiatAccountWizard_Step2CustomPreview() {
    var fieldValues by remember {
        mutableStateOf(mapOf("accountName" to "", "accountData" to ""))
    }

    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 2,
        stepsLength = 3,
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        CreateFiatAccount_Step2_AccountData(
            selectedMethodDisplayName = "Custom",
            selectedMethodId = "CUSTOM",
            formFields = customFormFields,
            fieldValues = fieldValues,
            onFieldChange = { id, value -> fieldValues = fieldValues + (id to value) },
        )
    }
}

/**
 * Full wizard shell for Step 2 — structured method form (SEPA example).
 */
@Composable
private fun CreateFiatAccountWizard_Step2StructuredPreview() {
    var fieldValues by remember {
        mutableStateOf(
            mapOf(
                "accountHolder" to "Satoshi Nakamoto",
                "iban" to "DE89 3704 0044 0532 0130 00",
                "bic" to "COBADEFFXXX",
            ),
        )
    }

    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 2,
        stepsLength = 3,
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        CreateFiatAccount_Step2_AccountData(
            selectedMethodDisplayName = "SEPA",
            selectedMethodId = "SEPA",
            formFields = sepaFormFields,
            fieldValues = fieldValues,
            onFieldChange = { id, value -> fieldValues = fieldValues + (id to value) },
        )
    }
}

/**
 * Full wizard shell for Step 3 — review and confirm.
 */
@Composable
private fun CreateFiatAccountWizard_Step3Preview() {
    MultiScreenWizardScaffold(
        title = "Add Fiat Account",
        stepIndex = 3,
        stepsLength = 3,
        nextButtonText = "Confirm",
        nextOnClick = { /* preview no-op */ },
        prevOnClick = { /* preview no-op */ },
        closeAction = true,
    ) {
        CreateFiatAccount_Step3_Review(
            selectedMethod =
                SimulatedPaymentMethod(
                    methodId = "SEPA",
                    displayName = "SEPA",
                    currencies = listOf("EUR"),
                    chargebackRisk = SimulatedChargebackRisk.VERY_LOW,
                ),
            formFields = sepaFormFields,
            fieldValues =
                mapOf(
                    "accountHolder" to "Satoshi Nakamoto",
                    "iban" to "DE89 3704 0044 0532 0130 00",
                    "bic" to "COBADEFFXXX",
                ),
        )
    }
}

// -------------------------------------------------------------------------------------
// @Preview functions
// -------------------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step1_DefaultPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step1Preview()
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step1_WithSearchPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step1Preview(
            initialSearch = "sepa",
            initialSelectedMethodId = "SEPA",
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step1_RiskFilterPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step1Preview(
            initialFilter = RiskFilter.VERY_LOW,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step2_CustomPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step2CustomPreview()
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step2_SepaStructuredPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step2StructuredPreview()
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun CreateFiatWizard_Step3_ReviewPreview() {
    BisqTheme.Preview {
        CreateFiatAccountWizard_Step3Preview()
    }
}
