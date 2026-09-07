package network.bisq.mobile.presentation.common.ui.navigation.manager

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.TabNavRoute

/**
 * Manages navigation state and actions for the Bisq mobile app.
 *
 * This interface provides centralized navigation control, including:
 * - Root and tab-level navigation controller management
 * - Current tab navigation state observation via [currentTab]
 * - Navigation actions for moving between screens and tabs
 *
 * Implementations must be thread-safe for concurrent access to [currentTab].
 */
interface NavigationManager {
    // Navigation State
    val currentTab: StateFlow<TabNavRoute?>

    // Controller Management - NavGraphs call these when composed/disposed
    fun setRootNavController(navController: NavHostController?)

    fun setTabNavController(navController: NavHostController?)

    // Navigation State Queries
    fun isAtMainScreen(): Boolean

    fun isAtHomeTab(): Boolean

    fun showBackButton(): Boolean

    // Navigation Actions
    fun navigate(
        destination: NavRoute,
        customSetup: (NavOptionsBuilder) -> Unit = {},
        onCompleted: (() -> Unit)? = null,
    )

    fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean = true,
        shouldLaunchSingleTop: Boolean = true,
        shouldRestoreState: Boolean = true,
    )

    fun navigateBackTo(
        destination: NavRoute,
        shouldInclusive: Boolean = false,
        shouldSaveState: Boolean = false,
    )

    fun navigateFromUri(uri: String)

    fun navigateBack(
        onCompleted: (() -> Unit)? = null,
    )

    /**
     * True when the back-stack entry directly beneath the current one is exactly [destination] —
     * same route type AND same arguments. Lets a presenter turn a forward navigation that would
     * recreate the screen the user just came from into a plain back navigation, which is what
     * bounds the PrivateChat ⇄ PeerProfile tap cycle instead of stacking a copy per round trip.
     */
    fun isPreviousRoute(destination: NavRoute): Boolean
}
