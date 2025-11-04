package network.bisq.mobile.domain.data.datastore.serializer

import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import io.ktor.utils.io.core.toByteArray
import kotlinx.serialization.SerializationException
import network.bisq.mobile.crypto.decrypt
import network.bisq.mobile.crypto.encrypt
import network.bisq.mobile.domain.data.datastore.dataStoreJson
import network.bisq.mobile.domain.data.model.SensitiveSettings
import okio.BufferedSink
import okio.BufferedSource

object SensitiveSettingsSerializer : OkioSerializer<SensitiveSettings> {
    override val defaultValue: SensitiveSettings
        get() = SensitiveSettings()

    override suspend fun readFrom(source: BufferedSource): SensitiveSettings {
        if (source.exhausted()) return defaultValue
        return try {
            val decrypted = decrypt(source.readByteArray()).decodeToString()
            dataStoreJson.decodeFromString(
                SensitiveSettings.serializer(),
                decrypted
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot deserialize SensitiveSettings", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Cannot read SensitiveSettings", e)
        } catch (e: IllegalStateException) {
            throw CorruptionException("Cannot decrypt SensitiveSettings", e)
        }
    }

    override suspend fun writeTo(t: SensitiveSettings, sink: BufferedSink) {
        val payload = dataStoreJson.encodeToString(SensitiveSettings.serializer(), t)
        val encryptedPayload = encrypt(payload.toByteArray())
        sink.write(encryptedPayload)
    }
}