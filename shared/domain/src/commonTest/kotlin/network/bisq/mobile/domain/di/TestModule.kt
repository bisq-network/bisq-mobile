package network.bisq.mobile.domain.di

import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import network.bisq.mobile.domain.data.model.BaseModel
import network.bisq.mobile.domain.data.model.Trade
import network.bisq.mobile.domain.data.persistance.KeyValueStorage
import network.bisq.mobile.domain.data.persistance.PersistenceSource
import org.koin.dsl.module
import org.koin.core.qualifier.named

val testModule = module {
    // Use MapSettings for testing
    single<Settings> { MapSettings() }

    // Generic persistence source for BaseModel
    single<PersistenceSource<BaseModel>> {
        KeyValueStorage(
            settings = get(),
            serializer = { Json.encodeToString(it) },
            deserializer = { Json.decodeFromString(it) }
        )
    }

    // IMPORTANT: use named() qualifiers to retrieve the right one on testing (not necessary in app code)
    // Specific persistence source for Trade
    single<PersistenceSource<Trade>>(qualifier = named("tradeStorage")) {
        KeyValueStorage(
            settings = get(),
            serializer = { Json.encodeToString(it) },
            deserializer = { Json.decodeFromString<Trade>(it) }
        )
    }
    
    // Specific persistence source for Settings
    single<PersistenceSource<network.bisq.mobile.domain.data.model.Settings>>(qualifier = named("settingsStorage")) {
        KeyValueStorage(
            settings = get(),
            serializer = { Json.encodeToString(it) },
            deserializer = { Json.decodeFromString<network.bisq.mobile.domain.data.model.Settings>(it) }
        )
    }
}