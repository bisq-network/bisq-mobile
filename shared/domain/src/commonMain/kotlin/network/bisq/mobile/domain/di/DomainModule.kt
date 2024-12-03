package network.bisq.mobile.domain.di

import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.data.model.Greeting
import network.bisq.mobile.domain.data.persistance.KeyValueStorage
import network.bisq.mobile.domain.data.repository.*
import network.bisq.mobile.domain.getPlatformSettings
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
    single<GreetingRepository<Greeting>> { GreetingRepository() }
    single<BisqStatsRepository> { BisqStatsRepository() }
    single<BtcPriceRepository> { BtcPriceRepository() }
    single<MyTradesRepository> { MyTradesRepository() }
    single<SettingsRepository> { SettingsRepository(get()) }
    single<UserRepository> { UserRepository(get()) }
}
