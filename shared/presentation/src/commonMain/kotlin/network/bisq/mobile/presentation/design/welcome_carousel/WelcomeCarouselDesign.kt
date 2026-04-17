package network.bisq.mobile.presentation.design.welcome_carousel

/**
 * # Welcome Carousel Design PoC
 *
 * STATUS: Design proof-of-concept. NOT wired to any presenter or production code.
 *
 * ======================================================================================
 * PURPOSE
 * ======================================================================================
 * Replaces the current stacked-dialog pattern on the Dashboard screen (notification
 * permission → battery optimization → [new] analytics opt-in) with a single carousel
 * overlay that bundles all pending opt-in items into a clean, sequential experience.
 *
 * ======================================================================================
 * INTERACTION MODEL — NON-SWIPEABLE, ACTION-DRIVEN
 * ======================================================================================
 * The carousel is NOT swipeable. Users cannot browse ahead or swipe between pages.
 * Progression is strictly action-driven: the user MUST take one of two actions on the
 * current page before the next page becomes visible:
 *
 *   1. Primary action (e.g., "Enable notifications") — opts in and advances
 *   2. "Don't ask again" — permanently dismisses this item and advances
 *
 * This ensures every opt-in item is explicitly addressed. There is no "skip" or
 * "skip all" option. The carousel remains on screen until every pending page has
 * been resolved through one of the two actions.
 *
 * Rationale: These are permission/privacy decisions that should be conscious choices,
 * not things the user accidentally swipes past. The sequential, forced-decision model
 * guarantees informed consent for each item.
 *
 * ======================================================================================
 * BEHAVIOR
 * ======================================================================================
 * - Shows on every Dashboard load UNTIL all items are resolved
 * - Only shows pages for items still in PENDING state
 * - Each page has two paths: primary action (opt in) or "Don't ask again" (permanent dismiss)
 * - After the user addresses a page, the carousel crossfades to the next pending page
 * - When no pending pages remain, the carousel dismisses and won't reappear
 * - No "skip all" — each item must be individually addressed
 *
 * ======================================================================================
 * PAGE INDICATOR DOTS
 * ======================================================================================
 * Dots are always visible when the total number of pages is > 1. They represent the
 * total pending pages at carousel open time. As the user resolves each page, the
 * active dot advances (filled → next). Dots do NOT disappear as pages are resolved —
 * they show progress through the set (like a wizard stepper). The total count is
 * fixed for the duration of this carousel session. Next time the carousel opens,
 * only remaining PENDING items will have dots.
 *
 * ======================================================================================
 * CAROUSEL PAGES
 * ======================================================================================
 *
 * Page 1: Push Notifications
 *   - Primary: "Enable notifications" → triggers native permission dialog
 *   - Dismiss: "Don't ask again" → state = DONT_ASK_AGAIN, notifications stay OFF
 *   - Condition: notificationPermissionState == NOT_GRANTED or DENIED
 *   - Resolution: GRANTED or DONT_ASK_AGAIN
 *
 * Page 2: Battery Optimization (Android only)
 *   - Primary: "Disable optimization" → opens platform battery settings
 *   - Dismiss: "Don't ask again" → state = DONT_ASK_AGAIN
 *   - Condition: notifications GRANTED AND batteryOptimizationState == NOT_IGNORED
 *   - Resolution: IGNORED or DONT_ASK_AGAIN
 *   - NOTE: This page only appears if notifications were granted (same as current logic)
 *
 * Page 3: Crash & Usage Reporting (Analytics)
 *   - Primary: "Enable reporting" → analyticsEnabled = true
 *   - Dismiss: "Don't ask again" → analyticsOptInState = DONT_ASK_AGAIN, analytics stays OFF
 *   - Condition: analyticsOptInState == PENDING
 *   - Resolution: ENABLED or DONT_ASK_AGAIN
 *   - Includes a "Learn more" tappable link → opens privacy details wiki/info
 *     (using WebLinkConfirmationDialog since it's an external URL)
 *
 * ======================================================================================
 * APP DIFFERENCES
 * ======================================================================================
 *
 * Client App (Android):  All 3 pages applicable
 * Client App (iOS):      All 3 pages applicable (battery page may differ in copy)
 * Node App (Android):    All 3 pages applicable
 *
 * The carousel component itself is shared. Per-app differences are in:
 *   - Which pages are included (presenter decides based on platform)
 *   - Platform-specific action handlers (native permission APIs differ)
 *   - Copy variations (iOS battery text differs from Android)
 *
 * ======================================================================================
 * VISUAL DESIGN
 * ======================================================================================
 * - Transparent scrim overlay (30% black) — Dashboard remains visible behind the card
 *   so the user retains context of where they are
 * - Dark card background (dark_grey30) with rounded corners, centered vertically
 * - Top: large icon (56dp) with tinted circular background
 * - Center: title + description text
 * - Analytics page: includes "Learn more" link below description
 * - Bottom: primary action button (full width) + "Don't ask again" text button
 * - Page indicator dots always visible when totalPages > 1
 * - Smooth crossfade transition between pages (no horizontal slide — not swipeable)
 *
 * ======================================================================================
 * PRESENTER CONTRACT
 * ======================================================================================
 * ```kotlin
 * data class CarouselPageState(
 *     val type: CarouselPageType,
 *     val isPending: Boolean,
 * )
 *
 * enum class CarouselPageType { NOTIFICATIONS, BATTERY, ANALYTICS }
 *
 * // Presenter exposes:
 * val pendingPages: StateFlow<List<CarouselPageType>>
 * val isCarouselVisible: StateFlow<Boolean>
 * fun onPageAction(type: CarouselPageType)      // user taps primary button
 * fun onPageDismiss(type: CarouselPageType)      // user taps "don't ask again"
 * ```
 *
 * ======================================================================================
 * I18N KEYS NEEDED
 * ======================================================================================
 * mobile.welcomeCarousel.notifications.title       = "Stay informed about your trades"
 * mobile.welcomeCarousel.notifications.description  = "Get notified when your trade partner
 *     sends a message or when your trade status changes."
 * mobile.welcomeCarousel.notifications.action       = "Enable notifications"
 *
 * mobile.welcomeCarousel.battery.title              = "Keep Bisq running"
 * mobile.welcomeCarousel.battery.description         = "Disable battery optimization so you
 *     receive notifications reliably, even when the app is in the background."
 * mobile.welcomeCarousel.battery.action              = "Open settings"
 *
 * mobile.welcomeCarousel.analytics.title             = "Help improve Bisq"
 * mobile.welcomeCarousel.analytics.description       = "Share anonymous crash reports and
 *     usage data through Tor to help developers fix bugs and improve the app.
 *     No personal information is collected."
 * mobile.welcomeCarousel.analytics.action            = "Enable reporting"
 * mobile.welcomeCarousel.analytics.learnMore         = "Learn more about what we collect"
 * mobile.welcomeCarousel.analytics.learnMoreUrl      = "https://bisq.wiki/..."
 *
 * mobile.welcomeCarousel.dontAskAgain               = "Don't ask again"
 */

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButton
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqButtonType
import network.bisq.mobile.presentation.common.ui.components.atoms.BisqText
import network.bisq.mobile.presentation.common.ui.components.atoms.layout.BisqGap
import network.bisq.mobile.presentation.common.ui.theme.BisqTheme
import network.bisq.mobile.presentation.common.ui.theme.BisqUIConstants
import network.bisq.mobile.presentation.common.ui.utils.ExcludeFromCoverage

// ---------------------------------------------------------------------------
// Data model for carousel pages
// ---------------------------------------------------------------------------

internal enum class CarouselPageType {
    NOTIFICATIONS,
    BATTERY,
    ANALYTICS,
}

internal data class CarouselPage(
    val type: CarouselPageType,
    val iconText: String,
    val iconBackgroundColor: Color,
    val title: String,
    val description: String,
    val actionText: String,
    val learnMoreText: String? = null,
)

// ---------------------------------------------------------------------------
// Carousel overlay component
// ---------------------------------------------------------------------------

/**
 * Transparent overlay containing the carousel card.
 * In production, this replaces the dialog visibility logic in DashboardScreen.kt.
 *
 * The overlay uses a light scrim (30% black) so the Dashboard remains visible
 * behind the card, keeping the user oriented in the app.
 *
 * The carousel is NOT swipeable. Progression is strictly action-driven:
 * the user must tap either the primary action or "Don't ask again" to advance.
 *
 * @param pages List of pending pages to show (only unresolved items)
 * @param totalPages Total number of pages in this session (for dot indicator)
 * @param onAction Called when user taps the primary action button for a page
 * @param onDismiss Called when user taps "Don't ask again" for a page
 * @param onLearnMore Called when user taps the "Learn more" link (analytics page)
 */
@Composable
internal fun WelcomeCarouselOverlay(
    pages: List<CarouselPage>,
    totalPages: Int = pages.size,
    onAction: (CarouselPageType) -> Unit = {},
    onDismiss: (CarouselPageType) -> Unit = {},
    onLearnMore: () -> Unit = {},
) {
    if (pages.isEmpty()) return

    var currentIndex by remember { mutableIntStateOf(0) }
    val safeIndex = currentIndex.coerceIn(0, pages.lastIndex)
    val currentPage = pages[safeIndex]

    // Light transparent scrim — Dashboard stays visible for context
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = BisqUIConstants.ScreenPadding2X)
                    .clip(RoundedCornerShape(BisqUIConstants.BorderRadius))
                    .background(BisqTheme.colors.dark_grey30)
                    .padding(BisqUIConstants.ScreenPadding2X),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Page indicator dots — always visible when multiple pages
            if (totalPages > 1) {
                PageIndicatorDots(
                    totalPages = totalPages,
                    currentPage = safeIndex,
                )
                BisqGap.V2()
            }

            // Icon
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(currentPage.iconBackgroundColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                BisqText.H3Regular(
                    text = currentPage.iconText,
                    color = currentPage.iconBackgroundColor,
                )
            }

            BisqGap.V2()

            // Title + description with crossfade
            AnimatedContent(
                targetState = safeIndex,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "carousel_page",
            ) { index ->
                val page = pages[index]
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    BisqText.H5Light(
                        text = page.title,
                        textAlign = TextAlign.Center,
                    )
                    BisqGap.V1()
                    BisqText.BaseLightGrey(
                        text = page.description,
                        textAlign = TextAlign.Center,
                    )
                    // "Learn more" link (analytics page only)
                    if (page.learnMoreText != null) {
                        BisqGap.VHalf()
                        BisqButton(
                            text = page.learnMoreText,
                            onClick = onLearnMore,
                            type = BisqButtonType.Clear,
                            color = BisqTheme.colors.primary,
                            padding = PaddingValues(0.dp),
                        )
                    }
                }
            }

            BisqGap.V3()

            // Primary action button
            BisqButton(
                text = currentPage.actionText,
                onClick = {
                    onAction(currentPage.type)
                    if (currentIndex < pages.lastIndex) {
                        currentIndex++
                    }
                },
                fullWidth = true,
            )

            BisqGap.V1()

            // "Don't ask again" secondary action
            BisqButton(
                text = "Don't ask again",
                onClick = {
                    onDismiss(currentPage.type)
                    if (currentIndex < pages.lastIndex) {
                        currentIndex++
                    }
                },
                type = BisqButtonType.Clear,
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Page indicator dots
// ---------------------------------------------------------------------------

/**
 * Horizontal row of dots showing progress through the carousel.
 * Dots represent total pages for this session. The active dot is highlighted
 * green (primary); resolved dots behind it stay grey. Dots do NOT disappear
 * as pages are resolved — they show a wizard-style progress indicator.
 */
@Composable
private fun PageIndicatorDots(
    totalPages: Int,
    currentPage: Int,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(totalPages) { index ->
            Box(
                modifier =
                    Modifier
                        .size(if (index == currentPage) 10.dp else 8.dp)
                        .clip(CircleShape)
                        .background(
                            when {
                                index == currentPage -> BisqTheme.colors.primary
                                index < currentPage -> BisqTheme.colors.primary.copy(alpha = 0.4f)
                                else -> BisqTheme.colors.mid_grey30
                            },
                        ),
            )
        }
    }
}

// ---------------------------------------------------------------------------
// Sample pages factory
// ---------------------------------------------------------------------------

@Composable
private fun allSamplePages(): List<CarouselPage> =
    listOf(
        CarouselPage(
            type = CarouselPageType.NOTIFICATIONS,
            iconText = "\uD83D\uDD14",
            iconBackgroundColor = BisqTheme.colors.primary,
            title = "Stay informed about your trades",
            description =
                "Get notified when your trade partner sends a message " +
                    "or when your trade status changes.",
            actionText = "Enable notifications",
        ),
        CarouselPage(
            type = CarouselPageType.BATTERY,
            iconText = "\uD83D\uDD0B",
            iconBackgroundColor = BisqTheme.colors.warning,
            title = "Keep Bisq running",
            description =
                "Disable battery optimization so you receive " +
                    "notifications reliably, even when the app is in the background.",
            actionText = "Open settings",
        ),
        analyticsSamplePage(),
    )

@Composable
private fun analyticsSamplePage(): CarouselPage =
    CarouselPage(
        type = CarouselPageType.ANALYTICS,
        iconText = "\uD83D\uDEE1\uFE0F",
        iconBackgroundColor = BisqTheme.colors.primary,
        title = "Help improve Bisq",
        description =
            "Share anonymous crash reports and usage data through Tor " +
                "to help developers fix bugs and improve the app. " +
                "No personal information is collected.",
        actionText = "Enable reporting",
        learnMoreText = "Learn more about what we collect",
    )

// ---------------------------------------------------------------------------
// Previews
// ---------------------------------------------------------------------------

@ExcludeFromCoverage
@Preview
@Composable
private fun WelcomeCarousel_AllThreePages_Page1_Preview() {
    BisqTheme.Preview {
        val pages = allSamplePages()
        WelcomeCarouselOverlay(
            pages = pages,
            totalPages = pages.size,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun WelcomeCarousel_AllThreePages_Page2_Preview() {
    // Simulates state after user resolved notifications → now on battery page
    BisqTheme.Preview {
        val allPages = allSamplePages()
        // Show only remaining pages but totalPages reflects original count
        WelcomeCarouselOverlay(
            pages = allPages.drop(1),
            totalPages = allPages.size,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun WelcomeCarousel_AllThreePages_Page3_Analytics_Preview() {
    // Simulates state after user resolved notifications + battery → now on analytics
    BisqTheme.Preview {
        val allPages = allSamplePages()
        WelcomeCarouselOverlay(
            pages = allPages.drop(2),
            totalPages = allPages.size,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun WelcomeCarousel_TwoPages_BatteryAndAnalytics_Preview() {
    // User already granted notifications in a previous session
    BisqTheme.Preview {
        val pages =
            listOf(
                CarouselPage(
                    type = CarouselPageType.BATTERY,
                    iconText = "\uD83D\uDD0B",
                    iconBackgroundColor = BisqTheme.colors.warning,
                    title = "Keep Bisq running",
                    description =
                        "Disable battery optimization so you receive " +
                            "notifications reliably, even when the app is in the background.",
                    actionText = "Open settings",
                ),
                analyticsSamplePage(),
            )
        WelcomeCarouselOverlay(
            pages = pages,
            totalPages = pages.size,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun WelcomeCarousel_AnalyticsOnly_Preview() {
    // User resolved everything except analytics
    BisqTheme.Preview {
        WelcomeCarouselOverlay(
            pages = listOf(analyticsSamplePage()),
            totalPages = 1,
        )
    }
}

@ExcludeFromCoverage
@Preview
@Composable
private fun PageIndicatorDots_AllStates_Preview() {
    BisqTheme.Preview {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            BisqText.SmallLightGrey("3 pages, on page 1:")
            PageIndicatorDots(totalPages = 3, currentPage = 0)
            BisqText.SmallLightGrey("3 pages, on page 2 (page 1 resolved):")
            PageIndicatorDots(totalPages = 3, currentPage = 1)
            BisqText.SmallLightGrey("3 pages, on page 3 (pages 1-2 resolved):")
            PageIndicatorDots(totalPages = 3, currentPage = 2)
            BisqText.SmallLightGrey("2 pages, on page 1:")
            PageIndicatorDots(totalPages = 2, currentPage = 0)
            BisqText.SmallLightGrey("2 pages, on page 2:")
            PageIndicatorDots(totalPages = 2, currentPage = 1)
        }
    }
}
