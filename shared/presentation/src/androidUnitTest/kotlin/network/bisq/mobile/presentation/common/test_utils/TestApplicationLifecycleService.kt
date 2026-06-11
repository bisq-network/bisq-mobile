package network.bisq.mobile.presentation.common.test_utils

import io.mockk.mockk
import network.bisq.mobile.data.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.data.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.data.service.network.KmpTorService
import network.bisq.mobile.domain.analytics.AnalyticsBootstrapConfig
import network.bisq.mobile.domain.analytics.AnalyticsService
import network.bisq.mobile.domain.analytics.AnalyticsSocksPortProvider
import network.bisq.mobile.domain.analytics.BufferedAnalyticsService
import network.bisq.mobile.domain.analytics.NoOpAnalyticsService
import network.bisq.mobile.domain.repository.SettingsRepository

/**
 * Lightweight reusable ApplicationLifecycleService for unit tests.
 * Uses relaxed MockK defaults for its dependencies and no-op lifecycle methods.
 * Analytics defaults to NoOp + a blank-DSN config — the base bootstrap call
 * becomes a guaranteed no-op so this fixture is safe for every test.
 */
class TestApplicationLifecycleService(
    applicationBootstrapFacade: ApplicationBootstrapFacade = mockk(relaxed = true),
    kmpTorService: KmpTorService = mockk(relaxed = true),
    analyticsService: AnalyticsService = NoOpAnalyticsService,
    analyticsBootstrapConfig: AnalyticsBootstrapConfig =
        AnalyticsBootstrapConfig(dsn = "", environment = "test", release = "test", isDebug = false),
    bufferedAnalyticsService: BufferedAnalyticsService? = null,
    analyticsSocksPortProvider: AnalyticsSocksPortProvider? = null,
    settingsRepository: SettingsRepository? = null,
) : ApplicationLifecycleService(
        applicationBootstrapFacade,
        kmpTorService,
        analyticsService,
        analyticsBootstrapConfig,
        bufferedAnalyticsService,
        analyticsSocksPortProvider,
        settingsRepository,
    ) {
    override suspend fun activateServiceFacades() {}

    override suspend fun deactivateServiceFacades() {}
}
