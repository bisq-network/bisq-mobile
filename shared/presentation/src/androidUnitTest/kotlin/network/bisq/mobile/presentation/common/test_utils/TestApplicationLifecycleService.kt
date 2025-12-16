package network.bisq.mobile.presentation.common.test_utils

import io.mockk.mockk
import network.bisq.mobile.domain.service.bootstrap.ApplicationBootstrapFacade
import network.bisq.mobile.domain.service.bootstrap.ApplicationLifecycleService
import network.bisq.mobile.domain.service.network.KmpTorService

/**
 * Lightweight reusable ApplicationLifecycleService for unit tests.
 * Uses relaxed MockK defaults for its dependencies and no-op lifecycle methods.
 */
class TestApplicationLifecycleService(
    applicationBootstrapFacade: ApplicationBootstrapFacade = mockk(relaxed = true),
    kmpTorService: KmpTorService = mockk(relaxed = true),
) : ApplicationLifecycleService(applicationBootstrapFacade, kmpTorService) {
    override suspend fun activateServiceFacades() {}
    override suspend fun deactivateServiceFacades() {}
}

