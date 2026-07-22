package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import network.bisq.mobile.data.model.TradeReadStateMap

object TradeReadStateMapSerializer : OkioSerializer<TradeReadStateMap> by jsonDataStoreSerializer(
    defaultValue = TradeReadStateMap(),
    typeName = "TradeReadStateMap",
)
