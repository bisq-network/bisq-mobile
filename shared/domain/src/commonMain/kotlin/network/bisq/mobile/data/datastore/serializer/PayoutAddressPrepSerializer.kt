package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import network.bisq.mobile.data.model.PayoutAddressPrep

object PayoutAddressPrepSerializer : OkioSerializer<PayoutAddressPrep> by jsonDataStoreSerializer(
    defaultValue = PayoutAddressPrep(),
    typeName = "PayoutAddressPrep",
)
