package network.bisq.mobile.domain.di

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.data.persistance.KeyValueStorage
import network.bisq.mobile.domain.data.repository.SettingsRepository
import network.bisq.mobile.domain.data.repository.TradeRepository
import network.bisq.mobile.domain.data.repository.TradeReadStateRepository
import network.bisq.mobile.domain.data.repository.UserRepository
import network.bisq.mobile.domain.getPlatformSettings
import network.bisq.mobile.domain.service.notifications.OpenTradesNotificationService
import network.bisq.mobile.domain.utils.CoroutineJobsManager
import network.bisq.mobile.domain.utils.DefaultCoroutineJobsManager
import org.koin.dsl.module

val domainModule = module {
    // Data
    single<Settings> { getPlatformSettings() }

    // Provide PersistenceSource
    single<KeyValueStorage<*>> {
        KeyValueStorage(
            settings = get(),
            serializer = { Json.encodeToString(it) },
            deserializer = { Json.decodeFromString(it) }
        )
    }

    // Repositories
    single<SettingsRepository> { SettingsRepository(get()) }
    single<UserRepository> { UserRepository(get()) }
    single<TradeRepository> { TradeRepository(get()) }
    single<TradeReadStateRepository> { TradeReadStateRepository(get()) }

    // Services
    single<OpenTradesNotificationService> { OpenTradesNotificationService(get(), get()) }

    factory<CoroutineJobsManager> { DefaultCoroutineJobsManager() }
}
