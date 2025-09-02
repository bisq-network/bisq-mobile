package network.bisq.mobile.domain.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.SerializationException
import network.bisq.mobile.domain.data.datastore.dataStoreJson
import network.bisq.mobile.domain.data.model.User
import okio.BufferedSink
import okio.BufferedSource

object UserSerializer : OkioSerializer<User> {
    override val defaultValue: User
        get() = User()

    override suspend fun readFrom(source: BufferedSource): User {
        if (source.exhausted()) return defaultValue
        return try {
            dataStoreJson.decodeFromString(
                User.serializer(),
                source.readUtf8()
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot deserialize User", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Cannot read User", e)
        }
    }

    override suspend fun writeTo(t: User, sink: BufferedSink) {
        val payload = dataStoreJson.encodeToString(User.serializer(), t)
        sink.writeUtf8(payload)
    }
}