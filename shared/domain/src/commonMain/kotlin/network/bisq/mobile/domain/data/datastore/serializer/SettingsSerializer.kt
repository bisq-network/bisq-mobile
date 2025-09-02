package network.bisq.mobile.domain.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.SerializationException
import network.bisq.mobile.domain.data.datastore.dataStoreJson
import network.bisq.mobile.domain.data.model.Settings
import okio.BufferedSink
import okio.BufferedSource

object SettingsSerializer : OkioSerializer<Settings> {
    override val defaultValue: Settings
        get() = Settings()

    override suspend fun readFrom(source: BufferedSource): Settings {
        if (source.exhausted()) return defaultValue
        return try {
            dataStoreJson.decodeFromString(
                Settings.serializer(),
                source.readUtf8()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot deserialize Settings", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Cannot read Settings", e)
        }
    }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        val payload = dataStoreJson.encodeToString(Settings.serializer(), t)
        sink.writeUtf8(payload)
    }
}