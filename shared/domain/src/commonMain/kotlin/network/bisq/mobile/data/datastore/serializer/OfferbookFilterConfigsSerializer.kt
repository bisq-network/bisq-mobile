package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.SerializationException
import network.bisq.mobile.data.datastore.dataStoreJson
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfigs
import okio.BufferedSink
import okio.BufferedSource

object OfferbookFilterConfigsSerializer : OkioSerializer<OfferbookFilterConfigs> {
    override val defaultValue: OfferbookFilterConfigs
        get() = OfferbookFilterConfigs()

    override suspend fun readFrom(source: BufferedSource): OfferbookFilterConfigs {
        if (source.exhausted()) return defaultValue
        return try {
            dataStoreJson.decodeFromString(
                OfferbookFilterConfigs.serializer(),
                source.readUtf8(),
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot deserialize OfferbookFilterConfigs", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Cannot read OfferbookFilterConfigs", e)
        }
    }

    override suspend fun writeTo(
        t: OfferbookFilterConfigs,
        sink: BufferedSink,
    ) {
        val payload = dataStoreJson.encodeToString(OfferbookFilterConfigs.serializer(), t)
        sink.writeUtf8(payload)
    }
}
