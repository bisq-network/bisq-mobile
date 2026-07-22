package network.bisq.mobile.client.common.domain.sensitive_settings

import androidx.annotation.VisibleForTesting
import androidx.datastore.core.CorruptionException
import androidx.datastore.core.okio.OkioSerializer
import io.ktor.utils.io.core.toByteArray
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.SerializationException
import network.bisq.mobile.data.crypto.decrypt
import network.bisq.mobile.data.crypto.encrypt
import network.bisq.mobile.data.datastore.dataStoreJson
import okio.BufferedSink
import okio.BufferedSource

object SensitiveSettingsSerializer : OkioSerializer<SensitiveSettings> {
    override val defaultValue: SensitiveSettings
        get() = SensitiveSettings()

    /**
     * True when a [GeneralSecurityException] (e.g. [javax.crypto.AEADBadTagException]) was
     * caught during deserialization. This signals that the Android Keystore key was
     * invalidated (typically by an OS upgrade or factory-reset of biometrics) and the
     * encrypted pairing credentials could not be recovered.
     *
     * The splash screen reads this flag to show an explanatory dialog before landing the
     * user on the trusted-node setup screen to re-pair.
     */
    private val _keystoreInvalidated = MutableStateFlow(false)
    val keystoreInvalidated: StateFlow<Boolean> = _keystoreInvalidated.asStateFlow()

    override suspend fun readFrom(source: BufferedSource): SensitiveSettings {
        if (source.exhausted()) return defaultValue
        return try {
            val decrypted = decrypt(source.readByteArray()).decodeToString()
            dataStoreJson.decodeFromString(
                SensitiveSettings.serializer(),
                decrypted,
            )
        } catch (e: SerializationException) {
            throw CorruptionException("Cannot deserialize SensitiveSettings", e)
        } catch (e: IllegalArgumentException) {
            throw CorruptionException("Cannot read SensitiveSettings", e)
        } catch (e: IllegalStateException) {
            throw CorruptionException("Cannot decrypt SensitiveSettings", e)
        } catch (e: Exception) {
            if (e.isKeystoreInvalidation()) {
                _keystoreInvalidated.value = true
                throw CorruptionException(
                    "Keystore key invalidated — encrypted SensitiveSettings unrecoverable. " +
                        "User must re-pair with their trusted node.",
                    e,
                )
            }
            throw e
        }
    }

    /**
     * Walks the exception cause chain looking for known Android Keystore
     * invalidation errors by simple class name (we cannot reference the
     * Java types directly from commonMain).
     */
    private fun Throwable.isKeystoreInvalidation(): Boolean {
        val keystoreExceptions =
            setOf(
                "AEADBadTagException",
                "KeyPermanentlyInvalidatedException",
                "BadPaddingException",
            )
        var current: Throwable? = this
        while (current != null) {
            if (current::class.simpleName in keystoreExceptions) return true
            current = current.cause
        }
        return false
    }

    override suspend fun writeTo(
        t: SensitiveSettings,
        sink: BufferedSink,
    ) {
        val payload = dataStoreJson.encodeToString(SensitiveSettings.serializer(), t)
        val encryptedPayload = encrypt(payload.toByteArray())
        sink.write(encryptedPayload)
    }

    @VisibleForTesting
    internal fun resetKeystoreInvalidatedForTest() {
        _keystoreInvalidated.value = false
    }

    @VisibleForTesting
    internal fun setKeystoreInvalidatedForTest(invalidated: Boolean) {
        _keystoreInvalidated.value = invalidated
    }
}
