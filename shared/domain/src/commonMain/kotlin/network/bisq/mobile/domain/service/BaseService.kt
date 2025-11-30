package network.bisq.mobile.domain.service

import kotlinx.coroutines.CoroutineScope
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.Logging
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * Base class for Bisq Android Services.
 *
 * Provide useful coroutines methods
 */
abstract class BaseService : KoinComponent, Logging {
    // we use KoinCompoent inject to avoid having to pass the manager as parameter on every single service
    protected val jobsManager: CoroutineJobsManager by inject()
    
    // Provide access to the service scope from the jobsManager
    protected val serviceScope: CoroutineScope
        get() = jobsManager.getScope()
}
