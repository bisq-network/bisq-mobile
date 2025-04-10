package network.bisq.mobile.domain.di

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.data.persistance.KeyValueStorage
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import org.koin.dsl.module

val testModule = module {
    single<Settings> { MapSettings() }

    single<PersistenceSource<*>> {
        KeyValueStorage(
            settings = get(),
            serializer = { Json.encodeToString(it) },
            deserializer = { Json.decodeFromString(it) }
        )
    }
}