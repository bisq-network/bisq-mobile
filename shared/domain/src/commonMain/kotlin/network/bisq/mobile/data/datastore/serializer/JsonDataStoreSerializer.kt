package network.bisq.mobile.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.serializer
import network.bisq.mobile.data.datastore.dataStoreJson
import okio.BufferedSink
import okio.BufferedSource

inline fun <reified T> jsonDataStoreSerializer(
    defaultValue: T,
    typeName: String,
    serializer: KSerializer<T> = serializer(),
): OkioSerializer<T> =
    object : OkioSerializer<T> {
        private val storedDefault: T = defaultValue

        override val defaultValue: T
            get() = storedDefault

        override suspend fun readFrom(source: BufferedSource): T {
            if (source.exhausted()) return storedDefault
            return try {
                dataStoreJson.decodeFromString(serializer, source.readUtf8())
            } catch (e: SerializationException) {
                throw CorruptionException("Cannot deserialize $typeName", e)
            } catch (e: IllegalArgumentException) {
                throw CorruptionException("Cannot read $typeName", e)
            }
        }

        override suspend fun writeTo(
            t: T,
            sink: BufferedSink,
        ) {
            sink.writeUtf8(dataStoreJson.encodeToString(serializer, t))
        }
    }
