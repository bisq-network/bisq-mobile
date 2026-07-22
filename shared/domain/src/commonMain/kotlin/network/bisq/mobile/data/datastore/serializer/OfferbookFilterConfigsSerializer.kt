package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import network.bisq.mobile.data.model.offerbook.OfferbookFilterConfigs

object OfferbookFilterConfigsSerializer : OkioSerializer<OfferbookFilterConfigs> by jsonDataStoreSerializer(
    defaultValue = OfferbookFilterConfigs(),
    typeName = "OfferbookFilterConfigs",
)
