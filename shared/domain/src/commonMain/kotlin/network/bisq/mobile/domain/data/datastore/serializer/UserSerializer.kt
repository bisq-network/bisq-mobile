package network.bisq.mobile.domain.data.datastore.serializer

import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.json.Json.Default.decodeFromString
import kotlinx.serialization.json.Json.Default.encodeToString
import network.bisq.mobile.domain.data.model.User
import okio.BufferedSink
import okio.BufferedSource

object UserSerializer: OkioSerializer<User> {
    override val defaultValue: User
        get() = User()

    override suspend fun readFrom(source: BufferedSource): User {
        return if (source.exhausted()) defaultValue
        else decodeFromString(
            User.serializer(),
            source.readUtf8()
        )
    }

    override suspend fun writeTo(t: User, sink: BufferedSink) {
        val json = encodeToString(User.serializer(), t)
        sink.writeUtf8(json)
    }
}