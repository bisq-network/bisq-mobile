package network.bisq.mobile.domain.di

import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import org.koin.dsl.module

val testModule = module {
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