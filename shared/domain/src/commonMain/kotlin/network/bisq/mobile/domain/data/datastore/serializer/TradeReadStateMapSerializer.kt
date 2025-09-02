package network.bisq.mobile.domain.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import network.bisq.mobile.domain.data.model.TradeReadStateMap
import okio.BufferedSink
import okio.BufferedSource

object TradeReadStateMapSerializer: OkioSerializer<TradeReadStateMap> {
    override val defaultValue: TradeReadStateMap
        get() = TradeReadStateMap()

    override suspend fun readFrom(source: BufferedSource): TradeReadStateMap {
        return if (source.exhausted()) defaultValue
        else decodeFromString(
            TradeReadStateMap.serializer(),
            source.readUtf8()
        )
    }

    override suspend fun writeTo(t: TradeReadStateMap, sink: BufferedSink) {
        val json = encodeToString(TradeReadStateMap.serializer(), t)
        sink.writeUtf8(json)
    }
}