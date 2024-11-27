package network.bisq.mobile.domain.di

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import network.bisq.mobile.domain.data.persistance.KeyValueStorage
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import org.koin.dsl.module

val testModule = module {
    single<Settings> { MapSettings() }

    single<PersistenceSource<*>> {
        KeyValueStorage(
            settings = get(),
            serializer = { kotlinx.serialization.json.Json.encodeToString(it) },
            deserializer = { kotlinx.serialization.json.Json.decodeFromString(it) }
        )
    }
}