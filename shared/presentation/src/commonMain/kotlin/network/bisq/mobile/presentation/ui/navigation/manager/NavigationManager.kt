package network.bisq.mobile.presentation.ui.navigation.manager

import androidx.navigation.NavHostController
import androidx.navigation.NavOptionsBuilder
import kotlinx.coroutines.flow.StateFlow
import network.bisq.mobile.presentation.ui.navigation.NavRoute
import network.bisq.mobile.presentation.ui.navigation.TabNavRoute

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

    // Controller Management
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
        onCompleted: (() -> Unit)? = null
    )

    fun navigateToTab(
        destination: TabNavRoute,
        saveStateOnPopUp: Boolean = true,
        shouldLaunchSingleTop: Boolean = true,
        shouldRestoreState: Boolean = true
    )

    fun navigateBackTo(
        destination: NavRoute,
        shouldInclusive: Boolean = false,
        shouldSaveState: Boolean = false
    )

    fun navigateFromUri(uri: String)

    fun navigateBack(
        onCompleted: (() -> Unit)? = null
    )
}
