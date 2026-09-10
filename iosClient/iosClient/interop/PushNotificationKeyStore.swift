import Foundation
import Security

@objc(PushNotificationKeyStore)
public class PushNotificationKeyStore: NSObject {
    private static let KEY_SIZE = 32 // AES-256
    private static let SERVICE_NAME = "network.bisq.mobile"
    private static let KEY_ACCOUNT = "push_notification_symmetric_key"
    // The key generation the last rotation displaced. The NSE tries it after the current key:
    // a push encrypted just before a rotation (or queued by APNs while the app was closed)
    // is otherwise undecryptable the moment the app re-registers. One generation of skew is
    // the deliberate window; mirrors the Android SharedPrefsKeyStore's previous slot.
    static let KEY_ACCOUNT_PREVIOUS = "push_notification_symmetric_key_previous"

    // Keychain access group shared between main app and NSE.
    // Must be explicitly specified in queries so that both the main app process and
    // the NSE process (which have different bundle IDs and different default keychain
    // groups) can access the same keychain item.
    // Resolved at build time from Info.plist via $(AppIdentifierPrefix), so it works
    // for any developer team without hardcoding the team ID.
    private static let ACCESS_GROUP: String? = {
        let group = Bundle.main.object(forInfoDictionaryKey: "KeychainAccessGroup") as? String
        if group == nil {
            #if DEBUG
            assertionFailure("KeychainAccessGroup missing from Info.plist — keychain sharing between app and NSE will fail")
            #endif
            NSLog("[PushNotificationKeyStore] WARNING: KeychainAccessGroup missing from Info.plist")
        }
        return group
    }()

    @objc public static let shared = PushNotificationKeyStore()

    private override init() {
        super.init()
    }

    /// Returns the Base64-encoded AES-256 symmetric key for push notification encryption.
    /// Generates and stores a new key if one doesn't exist yet.
    @objc public func getOrCreateKeyBase64WithError(_ error: NSErrorPointer) -> String? {
        do {
            let keyData = try getOrCreateKey()
            return keyData.base64EncodedString()
        } catch let keyError as NSError {
            error?.pointee = keyError
            return nil
        }
    }

    /// Rotates the symmetric key: the displaced key moves to the previous slot (see
    /// KEY_ACCOUNT_PREVIOUS) before a fresh one is generated and stored. Called on each
    /// device re-registration to limit the exposure window if a key is ever compromised.
    /// Returns the new key as Base64.
    @objc public func rotateKeyBase64WithError(_ error: NSErrorPointer) -> String? {
        do {
            if let displaced = PushNotificationKeyStore.retrieveKeyData() {
                deleteKey(account: PushNotificationKeyStore.KEY_ACCOUNT_PREVIOUS)
                try storeKey(displaced, account: PushNotificationKeyStore.KEY_ACCOUNT_PREVIOUS)
            }
            deleteKey(account: PushNotificationKeyStore.KEY_ACCOUNT)
            let keyData = try generateKey()
            try storeKey(keyData, account: PushNotificationKeyStore.KEY_ACCOUNT)
            return keyData.base64EncodedString()
        } catch let keyError as NSError {
            error?.pointee = keyError
            return nil
        }
    }

    // MARK: - Internal (also used by NSE via direct Keychain read)

    static func retrieveKeyData(account: String = KEY_ACCOUNT) -> Data? {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: account,
            kSecAttrService as String: SERVICE_NAME,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        if let group = ACCESS_GROUP {
            query[kSecAttrAccessGroup as String] = group
        }

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        if status == errSecSuccess, let data = result as? Data {
            return data
        }
        return nil
    }

    // MARK: - Private

    private func getOrCreateKey() throws -> Data {
        if let existing = PushNotificationKeyStore.retrieveKeyData() {
            return existing
        }

        let keyData = try generateKey()
        try storeKey(keyData)

        // Re-read to handle race condition
        if let stored = PushNotificationKeyStore.retrieveKeyData() {
            return stored
        }
        return keyData
    }

    private func generateKey() throws -> Data {
        var keyData = Data(count: PushNotificationKeyStore.KEY_SIZE)
        let result = keyData.withUnsafeMutableBytes { bytes in
            guard let baseAddress = bytes.baseAddress else {
                return errSecParam
            }
            return SecRandomCopyBytes(kSecRandomDefault, PushNotificationKeyStore.KEY_SIZE, baseAddress)
        }

        guard result == errSecSuccess else {
            throw NSError(domain: "PushNotificationKeyStore", code: Int(result),
                         userInfo: [NSLocalizedDescriptionKey: "Failed to generate random key"])
        }
        return keyData
    }

    private func storeKey(_ keyData: Data, account: String = PushNotificationKeyStore.KEY_ACCOUNT) throws {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: account,
            kSecAttrService as String: PushNotificationKeyStore.SERVICE_NAME,
            kSecValueData as String: keyData,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]
        if let group = PushNotificationKeyStore.ACCESS_GROUP {
            query[kSecAttrAccessGroup as String] = group
        }

        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess || status == errSecDuplicateItem else {
            throw NSError(domain: "PushNotificationKeyStore", code: Int(status),
                         userInfo: [NSLocalizedDescriptionKey: "Failed to store key: \(status)"])
        }
    }

    private func deleteKey(account: String = PushNotificationKeyStore.KEY_ACCOUNT) {
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: account,
            kSecAttrService as String: PushNotificationKeyStore.SERVICE_NAME,
        ]
        if let group = PushNotificationKeyStore.ACCESS_GROUP {
            query[kSecAttrAccessGroup as String] = group
        }
        SecItemDelete(query as CFDictionary)
    }
}
