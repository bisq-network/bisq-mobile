package network.bisq.mobile.domain.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import network.bisq.mobile.domain.data.model.Settings
import okio.BufferedSink
import okio.BufferedSource

object SettingsSerializer: OkioSerializer<Settings> {
    override val defaultValue: Settings
        get() = Settings()

    override suspend fun readFrom(source: BufferedSource): Settings {
        return if (source.exhausted()) defaultValue
        else decodeFromString(
            Settings.serializer(),
            source.readUtf8()
        )
    }

    override suspend fun writeTo(t: Settings, sink: BufferedSink) {
        val json = encodeToString(Settings.serializer(), t)
        sink.writeUtf8(json)
    }
}