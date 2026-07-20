package network.bisq.mobile.node.common.di

import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
val testModule =
    module {
        // Exception handler setup - singleton to ensure consistent setup
        single<CoroutineExceptionHandlerSetup> { CoroutineExceptionHandlerSetup() }

        // Job managers - factory to ensure each component has its own instance
        factory<CoroutineJobsManager> {
            DefaultCoroutineJobsManager().apply {
                get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
            }
        }

        // Test dispatcher-based GlobalUiManager
        single { GlobalUiManager(UnconfinedTestDispatcher()) }

        // Default NavigationManager — lazy relaxed mock; presenter tests override it with their own
        // field mock (via additionalModules) when they need to verify navigation.
        single<NavigationManager> { mockk(relaxed = true) }
    }
