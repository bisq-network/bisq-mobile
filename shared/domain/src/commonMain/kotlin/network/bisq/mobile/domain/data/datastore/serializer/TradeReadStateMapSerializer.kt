package network.bisq.mobile.domain.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.SerializationException
import network.bisq.mobile.domain.data.datastore.dataStoreJson
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import okio.BufferedSink
import okio.BufferedSource

object TradeReadStateMapSerializer : OkioSerializer<TradeReadStateMap> {
    override val defaultValue: TradeReadStateMap
        get() = TradeReadStateMap()

    override suspend fun readFrom(source: BufferedSource): TradeReadStateMap {
        if (source.exhausted()) return defaultValue
        return try {
            dataStoreJson.decodeFromString(
                TradeReadStateMap.serializer(),
                source.readUtf8()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot deserialize TradeReadStateMap", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Cannot read TradeReadStateMap", e)
        }
    }

    override suspend fun writeTo(t: TradeReadStateMap, sink: BufferedSink) {
        val payload = dataStoreJson.encodeToString(TradeReadStateMap.serializer(), t)
        sink.writeUtf8(payload)
    }
}