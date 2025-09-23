package network.bisq.mobile.domain.di

import androidx.datastore.core.DataStore
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import network.bisq.mobile.domain.data.datastore.createDataStore
import network.bisq.mobile.domain.data.datastore.serializer.SettingsSerializer
import network.bisq.mobile.domain.data.datastore.serializer.TradeReadStateMapSerializer
import network.bisq.mobile.domain.data.datastore.serializer.UserSerializer
import network.bisq.mobile.domain.data.model.Settings
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import network.bisq.mobile.domain.data.model.User
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.SettingsRepositoryImpl
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.data.repository.TradeReadStateRepositoryImpl
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.data.repository.UserRepositoryImpl
import network.bisq.mobile.domain.getStorageDir
import network.bisq.mobile.domain.utils.CoroutineExceptionHandlerSetup
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import org.koin.core.qualifier.named
import org.koin.dsl.module

val domainModule = module {

    single<DataStore<Settings>>(named("Settings")) {
        createDataStore(
            "Settings",
            getStorageDir(),
            SettingsSerializer,
            ReplaceFileCorruptionHandler { Settings() },
        )
    }

    single<DataStore<User>>(named("User")) {
        createDataStore(
            "User",
            getStorageDir(),
            UserSerializer,
            ReplaceFileCorruptionHandler { User() },
        )
    }

    single<DataStore<TradeReadStateMap>>(named("TradeReadStateMap")) {
        createDataStore(
            "TradeReadStateMap",
            getStorageDir(),
            TradeReadStateMapSerializer,
            ReplaceFileCorruptionHandler { TradeReadStateMap() },
        )
    }

    // Repositories
    single<SettingsRepository> { SettingsRepositoryImpl(get(named("Settings"))) }
    single<UserRepository> { UserRepositoryImpl(get(named("User"))) }
    single<TradeReadStateRepository> { TradeReadStateRepositoryImpl(get(named("TradeReadStateMap"))) }

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
