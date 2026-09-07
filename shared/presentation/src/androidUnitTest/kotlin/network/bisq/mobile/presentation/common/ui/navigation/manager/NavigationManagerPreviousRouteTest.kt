package network.bisq.mobile.presentation.common.ui.navigation.manager

import android.app.Application
import androidx.navigation.NavHostController
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.createGraph
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import network.bisq.mobile.presentation.common.ui.navigation.NavRoute
import network.bisq.mobile.presentation.common.ui.navigation.graph.addCommonAppRoutes
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * [NavigationManager.isPreviousRoute] against a REAL back stack (the production graph via
 * [addCommonAppRoutes], no mocked `hasRoute`/`toRoute`): the helper must match the entry beneath
 * the current one by route type AND arguments, because the PrivateChat ⇄ PeerProfile loop guard
 * turns a forward navigation into a back navigation only for the exact chat the user came from.
 * Robolectric because [NavHostController] takes a `Context` on Android; the graph's content
 * lambdas are never invoked, so no Koin and no composition.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class NavigationManagerPreviousRouteTest {
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

    private fun managerWith(navController: NavHostController): NavigationManagerImpl = NavigationManagerImpl(TestCoroutineJobsManager(testDispatcher)).also { it.setRootNavController(navController) }

    @Test
    fun `matches the previous entry by type and arguments`() =
        runTest(testDispatcher) {
            val navController = realNavController()
            navController.navigate(NavRoute.PrivateChat("discussion.a-b"))
            navController.navigate(NavRoute.PeerProfile("peer-1"))
            val manager = managerWith(navController)
            advanceUntilIdle()

            assertTrue(manager.isPreviousRoute(NavRoute.PrivateChat("discussion.a-b")))
        }

    @Test
    fun `a different argument on the same route type does not match`() =
        runTest(testDispatcher) {
            val navController = realNavController()
            navController.navigate(NavRoute.PrivateChat("discussion.a-b"))
            navController.navigate(NavRoute.PeerProfile("peer-1"))
            val manager = managerWith(navController)
            advanceUntilIdle()

            assertFalse(manager.isPreviousRoute(NavRoute.PrivateChat("discussion.a-OTHER")))
        }

    @Test
    fun `the current entry is not the previous one`() =
        runTest(testDispatcher) {
            val navController = realNavController()
            navController.navigate(NavRoute.PrivateChat("discussion.a-b"))
            navController.navigate(NavRoute.PeerProfile("peer-1"))
            val manager = managerWith(navController)
            advanceUntilIdle()

            assertFalse(manager.isPreviousRoute(NavRoute.PeerProfile("peer-1")))
        }

    @Test
    fun `no previous entry answers false`() =
        runTest(testDispatcher) {
            val navController = realNavController()
            val manager = managerWith(navController)
            advanceUntilIdle()

            assertFalse(manager.isPreviousRoute(NavRoute.PrivateChat("discussion.a-b")))
        }

    @Test
    fun `no controller answers false`() =
        runTest(testDispatcher) {
            val manager = NavigationManagerImpl(TestCoroutineJobsManager(testDispatcher))

            assertFalse(manager.isPreviousRoute(NavRoute.PrivateChat("discussion.a-b")))
        }
}
