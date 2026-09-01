package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import network.bisq.mobile.data.model.TradeStallClockMap

object TradeStallClockMapSerializer : OkioSerializer<TradeStallClockMap> by jsonDataStoreSerializer(
    defaultValue = TradeStallClockMap(),
    typeName = "TradeStallClockMap",
)
