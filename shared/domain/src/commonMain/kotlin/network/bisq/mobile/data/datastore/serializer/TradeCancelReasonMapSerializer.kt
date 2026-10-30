package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import network.bisq.mobile.data.model.TradeCancelReasonMap

object TradeCancelReasonMapSerializer : OkioSerializer<TradeCancelReasonMap> by jsonDataStoreSerializer(
    defaultValue = TradeCancelReasonMap(),
    typeName = "TradeCancelReasonMap",
)
