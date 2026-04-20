package network.bisq.mobile.data.crypto

/**
 * Returns the Base64-encoded AES-256 symmetric key for push notification encryption.
 * On iOS, the key is stored in a shared Keychain accessible by the Notification Service Extension.
 * On Android, returns null (Android does not need symmetric key for push notification decryption).
 */
expect fun getOrCreatePushNotificationKeyBase64(): String?
