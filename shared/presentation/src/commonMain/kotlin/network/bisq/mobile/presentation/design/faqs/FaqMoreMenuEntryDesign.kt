/**
 * FaqMoreMenuEntryDesign.kt — Design PoC (Issue #592)
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Shows where the new "FAQs" entry sits in the "More" tab menu hierarchy.
 *
 * ======================================================================================
 * PLACEMENT RATIONALE
 * ======================================================================================
 * The More tab is a simple flat list of navigation leaves (see MiscItemsScreen.kt).
 * No "About" or "Help" section grouping exists today — items share one visual level.
 *
 * The FAQ entry is proposed to sit between "Support" and "Payment Accounts":
 *
 *   Support          ← existing: community chat links, bug reporting
 *   FAQs             ← NEW: this feature
 *   Payment Accounts ← existing: account management
 *   Reputation       ← existing
 *   User Profile     ← existing
 *   Settings         ← existing
 *   Resources        ← existing: version, legal, web links
 *
 * Rationale: FAQ belongs near Support because both serve users who need help. Placing
 * it directly below Support creates a natural "help cluster" at the top of the list
 * without requiring a section header or structural changes to MiscItemsPresenter.
 *
 * If a "Help" section header is ever added, FAQs and Support would move into it together.
 *
 * ======================================================================================
 * PRODUCTION WIRING (for the dev picking this up)
 * ======================================================================================
 * 1. Add `data object Faqs : NavRoute` to NavRoute.kt
 * 2. Register the screen in the navigation graph (alongside ResourcesScreen, SupportScreen)
 * 3. Add a new nav icon drawable (e.g. `nav_faqs`) — or reuse `icon_question_mark`
 * 4. In MiscItemsPresenter.buildMenu(), insert:
 *      MenuItem.Leaf(
 *          label = "mobile.more.faqs".i18n(),
 *          icon  = Res.drawable.nav_faqs,
 *          route = NavRoute.Faqs,
 *      )
 *    immediately after the Support leaf.
 * 5. Add i18n key `mobile.more.faqs = "FAQs"` to mobile_en.properties
 * 6. Create FaqsPresenter / FaqsUiState / FaqsUiAction and move this design's
 *    content into the production package.
 * 7. Pass appType from the DI layer (ClientAppType enum or similar) so Q2/Q3 answers
 *    are selected at runtime.
 *
 * ======================================================================================
 * ICON NOTE
 * ======================================================================================
 * The simulated icons below use Material Icons (vector) as preview-only stand-ins.
 * The production nav_*.png drawables are white-on-transparent PNGs that render correctly
 * on the app's dark background at runtime. In Android Studio Compose preview the PNG
 * icons can appear invisible depending on the preview canvas colour, so Material Icons
 * are used here to keep the preview readable.
 *
 * Each Material Icon is chosen to match the semantic meaning of the real production
 * drawable it represents. The FAQs entry continues to use QuestionIcon (Res.drawable)
 * because that drawable has a visible grey fill and renders correctly in preview.
 */

package network.bisq.mobile.presentation.design.faqs

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.ArrowRightIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.icons.QuestionIcon
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.components.layout.BisqStaticLayout
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ============================================================================================
// Simulated menu items — mirrors MiscItemsPresenter.MenuItem.Leaf without importing it
// ============================================================================================

private data class SimulatedMenuItem(
    val label: String,
    val iconSlot: @Composable () -> Unit,
)

/**
 * Preview-only list simulating the More tab menu with the FAQs entry inserted.
 *
 * Icons: Material Icon vectors are used as preview-only stand-ins for the production
 * nav_*.png drawables (which are white-on-transparent PNGs). The chosen Material Icons
 * are semantically matched to each menu item.
 *
 * FAQs is the only entry that uses the real Bisq drawable (QuestionIcon /
 * icon_question_mark) because that PNG has its own grey fill and is visible in preview.
 */
private val faqsMenuItems: List<SimulatedMenuItem>
    get() =
        listOf(
            SimulatedMenuItem(
                label = "Support",
                // preview stand-in for nav_support.png
                iconSlot = {
                    Icon(
                        imageVector = Icons.Filled.SupportAgent,
                        contentDescription = "Support",
                        modifier = Modifier.size(20.dp),
                        tint = BisqTheme.colors.light_grey10,
                    )
                },
            ),
            // NEW entry — uses icon_question_mark which has a visible grey fill in preview.
            // Production will use a dedicated nav_faqs drawable (or reuse icon_question_mark).
            SimulatedMenuItem(
                label = "FAQs",
                iconSlot = {
                    QuestionIcon(modifier = Modifier.size(20.dp))
                },
            ),
            SimulatedMenuItem(
                label = "Payment Accounts",
                // preview stand-in for nav_accounts.png
                iconSlot = {
                    Icon(
                        imageVector = Icons.Filled.CreditCard,
                        contentDescription = "Payment Accounts",
                        modifier = Modifier.size(20.dp),
                        tint = BisqTheme.colors.light_grey10,
                    )
                },
            ),
            SimulatedMenuItem(
                label = "Reputation",
                // preview stand-in for nav_reputation.png
                iconSlot = {
                    Icon(
                        imageVector = Icons.Filled.Star,
                        contentDescription = "Reputation",
                        modifier = Modifier.size(20.dp),
                        tint = BisqTheme.colors.light_grey10,
                    )
                },
            ),
            SimulatedMenuItem(
                label = "User Profile",
                // preview stand-in for nav_user.png
                iconSlot = {
                    Icon(
                        imageVector = Icons.Filled.AccountCircle,
                        contentDescription = "User Profile",
                        modifier = Modifier.size(20.dp),
                        tint = BisqTheme.colors.light_grey10,
                    )
                },
            ),
            SimulatedMenuItem(
                label = "Settings",
                // preview stand-in for nav_settings.png
                iconSlot = {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp),
                        tint = BisqTheme.colors.light_grey10,
                    )
                },
            ),
            SimulatedMenuItem(
                label = "Resources",
                // preview stand-in for nav_resources.png
                iconSlot = {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = "Resources",
                        modifier = Modifier.size(20.dp),
                        tint = BisqTheme.colors.light_grey10,
                    )
                },
            ),
        )

// ============================================================================================
// Composables
// ============================================================================================

@Composable
private fun MoreMenuContent(
    items: List<SimulatedMenuItem>,
    highlightLabel: String = "",
    onItemClick: (String) -> Unit = {},
) {
    BisqStaticLayout(
        contentPadding = PaddingValues(all = BisqUIConstants.Zero),
        verticalArrangement = Arrangement.Top,
    ) {
        items.forEach { item ->
            val isHighlighted = item.label == highlightLabel
            BisqButton(
                text = item.label,
                onClick = { onItemClick(item.label) },
                fullWidth = true,
                backgroundColor =
                    if (isHighlighted) {
                        BisqTheme.colors.primary2
                    } else {
                        BisqTheme.colors.dark_grey40
                    },
                leftIcon = item.iconSlot,
                rightIcon = { ArrowRightIcon() },
                textAlign = TextAlign.Start,
                padding = PaddingValues(all = BisqUIConstants.ScreenPadding),
            )
            BisqGap.VHalf()
        }
    }
}

// ============================================================================================
// Previews
// ============================================================================================

/**
 * The full More tab menu with the FAQs entry highlighted (green tint background) so
 * reviewers can immediately spot the new item in context.
 */
@ExcludeFromCoverage
@Preview(name = "More Menu — FAQs entry highlighted")
@Composable
private fun MoreMenu_FaqsHighlighted_Preview() {
    BisqTheme.Preview {
        MoreMenuContent(
            items = faqsMenuItems,
            highlightLabel = "FAQs",
        )
    }
}

/**
 * The full More tab menu with no highlighting — matches the normal in-production appearance.
 */
@ExcludeFromCoverage
@Preview(name = "More Menu — Normal appearance")
@Composable
private fun MoreMenu_Normal_Preview() {
    BisqTheme.Preview {
        MoreMenuContent(
            items = faqsMenuItems,
            highlightLabel = "",
        )
    }
}
