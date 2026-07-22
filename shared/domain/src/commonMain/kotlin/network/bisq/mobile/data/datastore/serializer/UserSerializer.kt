package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import network.bisq.mobile.data.model.User

object UserSerializer : OkioSerializer<User> by jsonDataStoreSerializer(
    defaultValue = User(),
    typeName = "User",
)
