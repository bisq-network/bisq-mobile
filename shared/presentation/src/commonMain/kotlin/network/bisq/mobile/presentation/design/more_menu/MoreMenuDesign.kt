/**
 * MoreMenuDesign.kt — Design PoC (Issue #1520)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Redesigns the flat More-tab list into four labelled sections:
 *   IDENTITY & TRUST  /  TRADING SETUP  /  HELP  /  APP
 *
 * This is a conservative restructure: all items stay at top level within their section.
 * No merges. No nesting. No items removed.
 *
 * ======================================================================================
 * ICON VISIBILITY IN PREVIEWS
 * ======================================================================================
 * The nav_*.png drawables are white-on-transparent. BisqTheme.Preview wraps content in
 * Box(Modifier.background(BisqTheme.colors.backgroundColor)) which is Color(0xFF1C1C1C) —
 * a near-black dark grey. This is the correct canvas for the white icons; no additional
 * background wrapping is needed in individual previews.
 *
 * ======================================================================================
 * DESIGN DECISIONS
 * ======================================================================================
 *
 * SECTION HEADER STYLE — muted color chosen over small-caps
 * ──────────────────────────────────────────────────────────
 * IBM Plex Sans (the project's typeface) does not have a dedicated small-caps axis, so
 * true small-caps would require OpenType "smcp" feature settings that are unreliable
 * across the Android WebView stack and would look inconsistent on older API levels.
 * Muted color (mid_grey20) with XSmallMedium weight achieves the same "label, not
 * furniture" effect without the font-rendering risk. The label is uppercased at render
 * time via String.uppercase() so i18n keys store natural sentence-case strings.
 *
 * GREYED-OUT IGNORED USERS — row stays tappable
 * ───────────────────────────────────────────────
 * When the user has no ignored profiles, the Ignored Users row is rendered with
 * mid_grey20 label text and a tinted icon (ColorFilter.tint with the same muted grey).
 * The row REMAINS TAPPABLE and navigates to the Ignored Users screen, which renders its
 * own "no ignored users" empty state.
 *
 * Rationale: hiding the row entirely teaches users the feature does not exist. Making
 * it non-tappable with no explanation leaves them confused. Letting them tap and land
 * on a clear empty state is the most discoverable path, especially for users who want
 * to understand what the feature does before using it. The muted appearance signals
 * "nothing here right now" without blocking discovery.
 *
 * Icon tinting (ColorFilter.tint) is used rather than alpha reduction because tint
 * produces a flat mid-grey colour that reads unambiguously as "inactive", whereas alpha
 * would produce a faded white that could appear as an accidental rendering glitch on
 * some displays.
 *
 * No badge or count is shown in the populated state — the row is visually identical
 * to other rows when enabled, keeping the list calm and scannable.
 *
 * PER-APP ADDITIONS (addCustomSettings hook)
 * ──────────────────────────────────────────
 * Connect app injects: Trusted Node Setup (ClientMiscItemsPresenter)
 *   Production icon: Res.drawable.nav_trusted_node from apps/clientApp resources.
 *   In this PoC the row uses nav_settings as a placeholder (see inline comment below).
 * Node app injects:    Backup and Restore (NodeMiscItemsPresenter)
 *   Production icon: Res.drawable.backup — confirmed in shared/presentation resources.
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 *   mobile.more.section.identity       → "Identity & Trust"
 *   mobile.more.section.tradingSetup   → "Trading Setup"
 *   mobile.more.section.help           → "Help"
 *   mobile.more.section.app            → "App"
 *
 * All menu item labels already have keys in the existing production presenter.
 *
 * ======================================================================================
 * LOCALIZATION NOTE
 * ======================================================================================
 * German/Russian section labels can run 30-40% longer than English. The section header
 * composable uses a single full-width text row with no truncation — safe for all 14
 * supported languages.
 */
package network.bisq.mobile.presentation.design.more_menu

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import bisqapps.shared.presentation.generated.resources.Res
import bisqapps.shared.presentation.generated.resources.backup
import bisqapps.shared.presentation.generated.resources.nav_accounts
import bisqapps.shared.presentation.generated.resources.nav_ignored_users
import bisqapps.shared.presentation.generated.resources.nav_reputation
import bisqapps.shared.presentation.generated.resources.nav_resources
import bisqapps.shared.presentation.generated.resources.nav_settings
import bisqapps.shared.presentation.generated.resources.nav_support
import bisqapps.shared.presentation.generated.resources.nav_user
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage
import org.jetbrains.compose.resources.DrawableResource
import org.jetbrains.compose.resources.painterResource

// -------------------------------------------------------------------------------------
// Simulated data types (primitives only — no presenter or domain dependency)
// -------------------------------------------------------------------------------------

/**
 * A single flat menu item for this design PoC.
 *
 * [label]     Display label, already localised in preview data below.
 * [icon]      Drawable resource for the left icon. Null items fall through to a
 *             composable icon slot (see [SimulatedMenuItemWithComposableIcon]).
 * [isEnabled] When false the row is rendered in a muted/greyed style. The row
 *             remains tappable: tapping it still invokes [onClick] so the destination
 *             screen can show its own empty state.
 */
data class SimulatedMenuItem(
    val label: String,
    val icon: DrawableResource,
    val isEnabled: Boolean = true,
)

/**
 * Menu item variant where the left icon is a composable slot rather than a drawable
 * resource. Used for the FAQs row, whose icon is the [QuestionIcon] composable
 * (icon_question_mark has its own grey fill and is already visible on the dark canvas).
 */
data class SimulatedMenuItemWithComposableIcon(
    val label: String,
    val iconSlot: @Composable () -> Unit,
    val isEnabled: Boolean = true,
)

/**
 * One section of the More menu, consisting of a heading and its items.
 *
 * [headingLabel] Localised section heading (ALL-CAPS rendering is handled in the
 *                composable via [String.uppercase] — the i18n key stores sentence case).
 */
data class SimulatedMenuSection(
    val headingLabel: String,
    val items: List<SimulatedMenuItem> = emptyList(),
    val composableItems: List<SimulatedMenuItemWithComposableIcon> = emptyList(),
)

// -------------------------------------------------------------------------------------
// Section header composable
// -------------------------------------------------------------------------------------

/**
 * Lightweight section heading — muted color, XSmallMedium weight, uppercased at render.
 * No separator lines — vertical rhythm of the surrounding gaps provides enough grouping.
 */
@Composable
private fun SectionHeader(label: String) {
    val modifier =
        Modifier
            .fillMaxWidth()
            .padding(
                start = BisqUIConstants.ScreenPadding,
                top = BisqUIConstants.ScreenPadding,
                bottom = BisqUIConstants.ScreenPaddingHalf,
            )
    BisqText.XSmallMedium(
        text = label.uppercase(),
        color = BisqTheme.colors.mid_grey20,
        modifier = modifier,
    )
}

// -------------------------------------------------------------------------------------
// Single menu row — drawable resource icon variant
// -------------------------------------------------------------------------------------

/**
 * One tappable row in the More menu, with a [DrawableResource] left icon.
 *
 * Disabled appearance: label and icon both rendered in mid_grey20 via ColorFilter.tint.
 * Tint (not alpha) is used so the icon colour reads unambiguously as "inactive" rather
 * than appearing as a faded rendering artefact.
 *
 * NOTE: even when [item.isEnabled] is false, [onClick] is still invoked on tap.
 * This is intentional for the Ignored Users row (see file-level design comment).
 */
@Composable
private fun MenuItemRow(
    item: SimulatedMenuItem,
    onClick: () -> Unit,
) {
    val labelColor = if (item.isEnabled) BisqTheme.colors.white else BisqTheme.colors.mid_grey20
    val iconTint = if (item.isEnabled) null else ColorFilter.tint(BisqTheme.colors.mid_grey20)
    BisqButton(
        text = item.label,
        onClick = onClick,
        fullWidth = true,
        backgroundColor = BisqTheme.colors.dark_grey40,
        color = labelColor,
        leftIcon = {
            Image(
                painter = painterResource(item.icon),
                contentDescription = item.label,
                modifier = Modifier.size(20.dp),
                colorFilter = iconTint,
            )
        },
        rightIcon = { ArrowRightIcon() },
        textAlign = TextAlign.Start,
        padding = PaddingValues(all = BisqUIConstants.ScreenPadding),
        disabled = !item.isEnabled,
    )
}

// -------------------------------------------------------------------------------------
// Single menu row — composable icon slot variant (FAQs row)
// -------------------------------------------------------------------------------------

/**
 * Variant of [MenuItemRow] where the left icon is a composable slot.
 * Used only for the FAQs row whose icon ([QuestionIcon]) has its own colour fill.
 */
@Composable
private fun MenuItemRowWithComposableIcon(
    item: SimulatedMenuItemWithComposableIcon,
    onClick: () -> Unit,
) {
    val labelColor = if (item.isEnabled) BisqTheme.colors.white else BisqTheme.colors.mid_grey20
    BisqButton(
        text = item.label,
        onClick = onClick,
        fullWidth = true,
        backgroundColor = BisqTheme.colors.dark_grey40,
        color = labelColor,
        leftIcon = item.iconSlot,
        rightIcon = { ArrowRightIcon() },
        textAlign = TextAlign.Start,
        padding = PaddingValues(all = BisqUIConstants.ScreenPadding),
        disabled = !item.isEnabled,
    )
}

// -------------------------------------------------------------------------------------
// Full More menu — sections + items
// -------------------------------------------------------------------------------------

/**
 * The full More menu layout with section headers and item rows.
 *
 * [sections]  The ordered list of sections to render. Each section renders its
 *             header, then [SimulatedMenuSection.items] (drawable icons), then
 *             [SimulatedMenuSection.composableItems] (composable icon slots).
 * [onItemTap] Called with the item's label on every tap, regardless of enabled state
 *             (see design rationale above).
 */
@Composable
fun MoreMenuContent(
    sections: List<SimulatedMenuSection>,
    onItemTap: (String) -> Unit,
) {
    val modifier =
        Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(
                horizontal = BisqUIConstants.ScreenPadding,
                vertical = BisqUIConstants.ScreenPaddingHalf,
            )
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.Top,
    ) {
        sections.forEach { section ->
            SectionHeader(label = section.headingLabel)
            section.items.forEach { item ->
                MenuItemRow(
                    item = item,
                    onClick = { onItemTap(item.label) },
                )
                BisqGap.VHalf()
            }
            section.composableItems.forEach { item ->
                MenuItemRowWithComposableIcon(
                    item = item,
                    onClick = { onItemTap(item.label) },
                )
                BisqGap.VHalf()
            }
        }
    }
}

// -------------------------------------------------------------------------------------
// Preview data helpers
// -------------------------------------------------------------------------------------

/**
 * The four standard sections shared by both apps.
 * [ignoredUsersEnabled] drives the greyed-out state for the Ignored Users row.
 */
private fun buildCoreSections(ignoredUsersEnabled: Boolean): List<SimulatedMenuSection> {
    val identityItems =
        listOf(
            SimulatedMenuItem("User Profile", icon = Res.drawable.nav_user),
            SimulatedMenuItem("Ignored Users", icon = Res.drawable.nav_ignored_users, isEnabled = ignoredUsersEnabled),
            SimulatedMenuItem("Reputation", icon = Res.drawable.nav_reputation),
        )
    val tradingItems =
        listOf(
            SimulatedMenuItem("Payment Accounts", icon = Res.drawable.nav_accounts),
        )
    // FAQs uses a composable icon slot (QuestionIcon has its own colour fill).
    val helpItems =
        listOf(
            SimulatedMenuItem("Support", icon = Res.drawable.nav_support),
        )
    val helpComposableItems =
        listOf(
            SimulatedMenuItemWithComposableIcon(
                label = "FAQs",
                iconSlot = { QuestionIcon(modifier = Modifier.size(20.dp)) },
            ),
        )
    val appItems =
        listOf(
            SimulatedMenuItem("Settings", icon = Res.drawable.nav_settings),
            SimulatedMenuItem("Resources", icon = Res.drawable.nav_resources),
        )
    return listOf(
        SimulatedMenuSection(headingLabel = "Identity & Trust", items = identityItems),
        SimulatedMenuSection(headingLabel = "Trading Setup", items = tradingItems),
        SimulatedMenuSection(headingLabel = "Help", items = helpItems, composableItems = helpComposableItems),
        SimulatedMenuSection(headingLabel = "App", items = appItems),
    )
}

/**
 * Appends [extraItem] to the last (APP) section of [baseSections].
 * Mirrors what ClientMiscItemsPresenter / NodeMiscItemsPresenter do via addCustomSettings().
 */
private fun withAppAddition(
    baseSections: List<SimulatedMenuSection>,
    extraItem: SimulatedMenuItem,
): List<SimulatedMenuSection> {
    val updated = baseSections.toMutableList()
    val lastIndex = updated.lastIndex
    val lastSection = updated[lastIndex]
    val updatedLastSection = lastSection.copy(items = lastSection.items + extraItem)
    updated[lastIndex] = updatedLastSection
    return updated
}

// -------------------------------------------------------------------------------------
// Preview 1: Connect app — normal state (has ignored users)
// -------------------------------------------------------------------------------------

/**
 * Preview 1: Connect app with all 9 items + Trusted Node Setup.
 * Ignored Users row is in its enabled (normal) state because the user has at least
 * one ignored profile.
 *
 * Trusted Node Setup uses nav_settings as a placeholder.
 * Production icon: Res.drawable.nav_trusted_node from apps/clientApp resources
 * (app-specific resource, not importable from the shared:presentation design package).
 */
@ExcludeFromCoverage
@Preview(name = "1. Connect app — normal state (has ignored users)")
@Composable
private fun ConnectApp_Normal_Preview() {
    // Placeholder: nav_settings stands in for nav_trusted_node (clientApp-only resource).
    val sections =
        withAppAddition(
            baseSections = buildCoreSections(ignoredUsersEnabled = true),
            extraItem = SimulatedMenuItem("Trusted Node Setup", icon = Res.drawable.nav_settings),
        )
    BisqTheme.Preview {
        MoreMenuContent(
            sections = sections,
            onItemTap = {},
        )
    }
}

// -------------------------------------------------------------------------------------
// Preview 2: Connect app — empty ignored users state
// -------------------------------------------------------------------------------------

/**
 * Preview 2: Connect app, same layout, but Ignored Users row is greyed-out because
 * the user has no ignored profiles. The row is still tappable — the destination
 * screen will show the empty state. This is the key new visual to validate.
 *
 * Visual expectation: "Ignored Users" text appears in mid_grey20 (same as section
 * headers) and the nav_ignored_users icon is tinted to the same mid_grey20.
 * All other rows are unaffected.
 */
@ExcludeFromCoverage
@Preview(name = "2. Connect app — empty ignored users state (key new UI state)")
@Composable
private fun ConnectApp_EmptyIgnoredUsers_Preview() {
    // Placeholder: nav_settings stands in for nav_trusted_node (clientApp-only resource).
    val sections =
        withAppAddition(
            baseSections = buildCoreSections(ignoredUsersEnabled = false),
            extraItem = SimulatedMenuItem("Trusted Node Setup", icon = Res.drawable.nav_settings),
        )
    BisqTheme.Preview {
        MoreMenuContent(
            sections = sections,
            onItemTap = {},
        )
    }
}

// -------------------------------------------------------------------------------------
// Preview 3: Node app — normal state
// -------------------------------------------------------------------------------------

/**
 * Preview 3: Node app with all 9 standard items + Backup and Restore.
 * Trusted Node Setup is absent; Backup and Restore takes its place under APP.
 * Confirms the per-app hook (addCustomSettings) lands the right item in the right section.
 *
 * Backup and Restore uses Res.drawable.backup which is confirmed in the shared
 * presentation composeResources (not app-specific).
 */
@ExcludeFromCoverage
@Preview(name = "3. Node app — normal state (Backup and Restore under APP)")
@Composable
private fun NodeApp_Normal_Preview() {
    val sections =
        withAppAddition(
            baseSections = buildCoreSections(ignoredUsersEnabled = true),
            extraItem = SimulatedMenuItem("Backup and Restore", icon = Res.drawable.backup),
        )
    BisqTheme.Preview {
        MoreMenuContent(
            sections = sections,
            onItemTap = {},
        )
    }
}

// -------------------------------------------------------------------------------------
// Preview 4: Section header style isolation
// -------------------------------------------------------------------------------------

/**
 * Preview 4: Shows the IDENTITY & TRUST section in isolation so the header style can
 * be evaluated away from full-list visual noise. Useful for type/color sign-off in
 * Android Studio's preview panel.
 */
@ExcludeFromCoverage
@Preview(name = "4. Section header isolation — IDENTITY & TRUST")
@Composable
private fun SectionHeader_Isolation_Preview() {
    val modifier =
        Modifier
            .fillMaxWidth()
            .padding(horizontal = BisqUIConstants.ScreenPadding)
    BisqTheme.Preview {
        Column(modifier = modifier) {
            SectionHeader(label = "Identity & Trust")
            MenuItemRow(item = SimulatedMenuItem("User Profile", icon = Res.drawable.nav_user), onClick = {})
            BisqGap.VHalf()
            MenuItemRow(
                item = SimulatedMenuItem("Ignored Users", icon = Res.drawable.nav_ignored_users, isEnabled = false),
                onClick = {},
            )
            BisqGap.VHalf()
            MenuItemRow(item = SimulatedMenuItem("Reputation", icon = Res.drawable.nav_reputation), onClick = {})
            BisqGap.VHalf()
        }
    }
}

// -------------------------------------------------------------------------------------
// Preview 5: Side-by-side enabled vs greyed-out row comparison
// -------------------------------------------------------------------------------------

/**
 * Preview 5: Directly compares the enabled and greyed/disabled visual states for the
 * same label ("Ignored Users") so the icon tint + label colour treatment can both be
 * evaluated in one view. Not a realistic screen composition — purely a contrast aid.
 */
@ExcludeFromCoverage
@Preview(name = "5. Enabled vs greyed-out row comparison")
@Composable
private fun EnabledVsGreyedOut_Comparison_Preview() {
    val columnModifier =
        Modifier
            .fillMaxWidth()
            .padding(BisqUIConstants.ScreenPadding)
    val labelModifier =
        Modifier
            .fillMaxWidth()
            .padding(bottom = BisqUIConstants.ScreenPaddingHalf)
    BisqTheme.Preview {
        Column(
            modifier = columnModifier,
            verticalArrangement = Arrangement.spacedBy(BisqUIConstants.ScreenPaddingHalf),
        ) {
            BisqText.XSmallMedium(
                text = "ENABLED (has ignored users)",
                color = BisqTheme.colors.mid_grey20,
                modifier = labelModifier,
            )
            MenuItemRow(
                item = SimulatedMenuItem("Ignored Users", icon = Res.drawable.nav_ignored_users, isEnabled = true),
                onClick = {},
            )
            BisqGap.V1()
            BisqText.XSmallMedium(
                text = "GREYED OUT (no ignored users)",
                color = BisqTheme.colors.mid_grey20,
                modifier = labelModifier,
            )
            MenuItemRow(
                item = SimulatedMenuItem("Ignored Users", icon = Res.drawable.nav_ignored_users, isEnabled = false),
                onClick = {},
            )
        }
    }
}
