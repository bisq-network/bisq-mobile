package network.bisq.mobile.test.presentation.di

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.analytics.NoOpAnalyticsService
import network.bisq.mobile.domain.coroutines.DispatcherProvider
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.presentation.common.ui.base.GlobalUiManager
import network.bisq.mobile.presentation.common.ui.navigation.manager.NavigationManager
import network.bisq.mobile.test.coroutines.StandardTestDispatcherProvider
import network.bisq.mobile.test.coroutines.TestCoroutineJobsManager
import org.koin.core.module.Module
import org.koin.dsl.module

@OptIn(ExperimentalCoroutinesApi::class)
fun presentationTestModule(
    testDispatcher: CoroutineDispatcher = StandardTestDispatcher(),
    navigationManager: NavigationManager = NoopNavigationManager(),
    globalUiManager: GlobalUiManager = GlobalUiManager(testDispatcher),
    dispatcherProvider: DispatcherProvider = StandardTestDispatcherProvider(testDispatcher),
): Module =
    module {
        single { CoroutineExceptionHandlerSetup() }
        factory<CoroutineJobsManager> { TestCoroutineJobsManager(testDispatcher) }
        single<NavigationManager> { navigationManager }
        single { globalUiManager }
        single<DispatcherProvider> { dispatcherProvider }
        // BasePresenter resolves AnalyticsService as a non-null dependency. Tests that assert on
        // analytics load their own mock after this module, which overrides the no-op.
        single<AnalyticsService> { NoOpAnalyticsService }
    }
