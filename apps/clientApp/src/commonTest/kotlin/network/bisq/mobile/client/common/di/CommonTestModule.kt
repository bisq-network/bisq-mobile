package network.bisq.mobile.client.common.di

import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import org.koin.dsl.module

/**
 * Common test module for unit tests that need Koin DI.
 * Provides the minimal dependencies needed for ServiceFacade-based classes.
 */
val commonTestModule =
    module {
        // Exception handler setup - singleton to ensure consistent setup
        single<CoroutineExceptionHandlerSetup> { CoroutineExceptionHandlerSetup() }

        // Job managers - factory to ensure each component has its own instance
        factory<CoroutineJobsManager> {
            DefaultCoroutineJobsManager().apply {
                // Set up exception handler from the singleton setup
                get<CoroutineExceptionHandlerSetup>().setupExceptionHandler(this)
            }
        }
    }
