package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import network.bisq.mobile.data.model.Settings

object SettingsSerializer : OkioSerializer<Settings> by jsonDataStoreSerializer(
    defaultValue = Settings(),
    typeName = "Settings",
)
