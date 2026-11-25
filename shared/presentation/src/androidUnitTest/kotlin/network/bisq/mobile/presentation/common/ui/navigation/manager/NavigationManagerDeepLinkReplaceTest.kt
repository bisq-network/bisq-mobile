package network.bisq.mobile.presentation.common.ui.navigation.manager

import android.app.Application
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.navigation.toRoute
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.graph.addCommonAppRoutes
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

/**
 * A deep link to the screen already on top, against a REAL back stack (the production graph via
 * [addCommonAppRoutes]). Opening it with other arguments must give a new back-stack entry: reusing the
 * entry keeps its composition and the presenters it remembered, so a trade screen switched from one
 * trade to another by a notification kept acting on the first one. The same arguments keep reusing the
 * entry, so a repeated link does not stack a duplicate. Robolectric because [NavHostController] takes a
 * `Context` on Android; the graph's content lambdas are never invoked, so no Koin and no composition.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NavigationManagerDeepLinkReplaceTest {
    private val testDispatcher = StandardTestDispatcher()

    private fun realNavController(): NavHostController {
        val context: Application = ApplicationProvider.getApplicationContext()
        val navController = NavHostController(context)
        navController.navigatorProvider.addNavigator(ComposeNavigator())
        navController.graph =
            navController.createGraph(startDestination = NavRoute.Support) {
                addCommonAppRoutes { true }
            }
        return navController
    }

    private fun TestScope.openDeepLink(
        navController: NavHostController,
        uri: String,
    ) {
        val manager = NavigationManagerImpl(TestCoroutineJobsManager(testDispatcher))
        manager.setRootNavController(navController)
        advanceUntilIdle()
        manager.navigateFromUri(uri)
        advanceUntilIdle()
    }

    private fun NavHostController.stackOf(): List<NavBackStackEntry> = currentBackStack.value.filter { it.destination.route != null && it.destination.parent != null }

    private fun NavBackStackEntry.openTradeId(): String? = if (destination.hasRoute<NavRoute.OpenTrade>()) toRoute<NavRoute.OpenTrade>().tradeId else null

    @Test
    fun `a link to the trade screen on top with another trade replaces its entry`() =
        runTest(testDispatcher) {
            val navController = realNavController()
            navController.navigate(NavRoute.OpenTrade("trade-A"))
            val entryA = navController.currentBackStackEntry!!

            openDeepLink(navController, NavRoute.OpenTrade("trade-B").toUriString())

            val top = navController.currentBackStackEntry!!
            assertEquals("trade-B", top.openTradeId())
            assertNotEquals(entryA.id, top.id, "trade B must get a new entry, not trade A's with swapped arguments")
            assertEquals(
                listOf(null, "trade-B"),
                navController.stackOf().map { it.openTradeId() },
                "trade A is replaced, so back still returns to where it was opened from",
            )
        }

    @Test
    fun `a link to the trade screen on top with the same trade reuses its entry`() =
        runTest(testDispatcher) {
            val navController = realNavController()
            navController.navigate(NavRoute.OpenTrade("trade-A"))
            val entryA = navController.currentBackStackEntry!!

            openDeepLink(navController, NavRoute.OpenTrade("trade-A").toUriString())

            assertEquals(entryA.id, navController.currentBackStackEntry!!.id)
            assertEquals(listOf(null, "trade-A"), navController.stackOf().map { it.openTradeId() })
        }

    @Test
    fun `a link to another screen opens on top of the current one`() =
        runTest(testDispatcher) {
            val navController = realNavController()
            navController.navigate(NavRoute.OpenTrade("trade-A"))

            openDeepLink(navController, NavRoute.TradeChat("trade-A").toUriString())

            assertEquals(
                listOf(NavRoute.Support::class, NavRoute.OpenTrade::class, NavRoute.TradeChat::class),
                navController.stackOf().map { entry ->
                    listOf(NavRoute.Support::class, NavRoute.OpenTrade::class, NavRoute.TradeChat::class).first { entry.destination.hasRoute(it) }
                },
            )
        }

    @Test
    fun `a link to the chat on top with another channel replaces its entry`() =
        runTest(testDispatcher) {
            val navController = realNavController()
            navController.navigate(NavRoute.PrivateChat("discussion.a-b"))
            val entryA = navController.currentBackStackEntry!!

            openDeepLink(navController, NavRoute.PrivateChat("discussion.a-c").toUriString())

            val top = navController.currentBackStackEntry!!
            assertEquals("discussion.a-c", top.toRoute<NavRoute.PrivateChat>().channelId)
            assertNotEquals(entryA.id, top.id)
            assertEquals(2, navController.stackOf().size)
        }
}
