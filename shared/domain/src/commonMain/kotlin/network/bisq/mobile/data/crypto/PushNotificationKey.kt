package network.bisq.mobile.data.crypto

/**
 * Returns the Base64-encoded AES-256 symmetric key for push notification encryption.
 * On iOS, the key is stored in a shared Keychain accessible by the Notification Service Extension.
 * On Android, returns null (Android does not need symmetric key for push notification decryption).
 */
expect fun getOrCreatePushNotificationKeyBase64(): String?

/**
 * Deletes every stored generation of the push notification key (current and previous), so pushes
 * a trusted node keeps encrypting with them can no longer be decrypted on this device. The next
 * registration creates a fresh key. Returns false when the keys could not be deleted.
 */
expect fun deletePushNotificationKeys(): Boolean
