import UserNotifications
import CryptoKit
import Foundation
import Security
import os.log

/// Notification Service Extension that decrypts push notification content before display.
/// This avoids the double-notification problem where iOS first shows a generic alert,
/// then the app wakes up and posts a second decrypted notification.
///
/// Privacy: The lock-screen banner shows only a category-based summary (e.g. "Trade update"),
/// never counterparty names, amounts, or offer details. Full decrypted content is stored in
/// the shared app group container for the main app to display after unlock.
///
/// Requirements:
/// - The relay server must set `mutable-content: 1` in the APNs payload
/// - The trusted node must encrypt with AES-256-GCM using the device's symmetric key
/// - The symmetric key must be stored in shared Keychain (via PushNotificationKeyStore)
class NotificationService: UNNotificationServiceExtension {
    private static let NONCE_SIZE = 12
    private static let TAG_SIZE = 16
    private static let APP_GROUP = "group.network.bisq.mobile"
    private static let NSE_BREADCRUMB_KEY = "nse_last_invocation"
    private static let KEYCHAIN_SERVICE = "network.bisq.mobile"
    private static let KEYCHAIN_ACCOUNT = "push_notification_symmetric_key"
    // Resolved at build time from Info.plist via $(AppIdentifierPrefix).
    // Falls back to nil (default keychain group) if the plist key is missing,
    // though that would fail for NSE since it has a different default group.
    private static let KEYCHAIN_ACCESS_GROUP: String? = Bundle.main.object(forInfoDictionaryKey: "KeychainAccessGroup") as? String

    private let log = OSLog(subsystem: "network.bisq.mobile.BisqNotificationService", category: "NSE")

    private var contentHandler: ((UNNotificationContent) -> Void)?
    private var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(
        _ request: UNNotificationRequest,
        withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void
    ) {
        os_log("NSE didReceive invoked", log: log, type: .info)
        writeBreadcrumb(stage: "didReceive_start")

        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        guard let bestAttemptContent = bestAttemptContent else {
            os_log("NSE: no mutable content available", log: log, type: .error)
            writeBreadcrumb(stage: "no_mutable_content")
            contentHandler(request.content)
            return
        }

        guard let encryptedBase64 = request.content.userInfo["encrypted"] as? String,
              let encryptedData = Data(base64Encoded: encryptedBase64) else {
            os_log("NSE: no 'encrypted' field in userInfo or Base64 decode failed", log: log, type: .error)
            writeBreadcrumb(stage: "no_encrypted_field")
            contentHandler(bestAttemptContent)
            return
        }

        os_log("NSE: encrypted payload found (%{public}d bytes)", log: log, type: .info, encryptedData.count)

        guard let keyData = retrieveSymmetricKey() else {
            os_log("NSE: keychain retrieval failed — showing fallback", log: log, type: .error)
            writeBreadcrumb(stage: "keychain_retrieval_failed")
            bestAttemptContent.title = "Bisq"
            bestAttemptContent.body = "New notification"
            contentHandler(bestAttemptContent)
            return
        }

        os_log("NSE: symmetric key retrieved (%{public}d bytes)", log: log, type: .info, keyData.count)

        do {
            let decryptedData = try decryptAESGCM(data: encryptedData, keyData: keyData)
            let payload = try JSONDecoder().decode(NotificationPayload.self, from: decryptedData)

            // Privacy: show only a category-based summary on the lock screen.
            let summary = NotificationCategory.from(title: payload.title)
            bestAttemptContent.title = "Bisq"
            bestAttemptContent.body = summary.displayText

            // Pass opaque identifiers only — no human-readable trade details in userInfo
            bestAttemptContent.userInfo = bestAttemptContent.userInfo.merging([
                "nse_decrypted": true,
                "notification_id": payload.id,
                "notification_category": summary.rawValue,
            ]) { _, new in new }

            os_log("NSE: decryption success, category=%{public}@", log: log, type: .info, summary.rawValue)
            writeBreadcrumb(stage: "decrypt_success:\(summary.rawValue)")
        } catch {
            os_log("NSE: decryption failed: %{public}@", log: log, type: .error, error.localizedDescription)
            writeBreadcrumb(stage: "decrypt_failed:\(error.localizedDescription)")
            bestAttemptContent.title = "Bisq"
            bestAttemptContent.body = "New notification"
        }

        contentHandler(bestAttemptContent)
    }

    override func serviceExtensionTimeWillExpire() {
        os_log("NSE: serviceExtensionTimeWillExpire — delivering best attempt", log: log, type: .error)
        writeBreadcrumb(stage: "time_expired")
        if let contentHandler = contentHandler, let bestAttemptContent = bestAttemptContent {
            contentHandler(bestAttemptContent)
        }
    }

    // MARK: - Privacy-safe categories

    private enum NotificationCategory: String {
        case tradeUpdate = "trade_update"
        case chatMessage = "chat_message"
        case offerUpdate = "offer_update"
        case general = "general"

        var displayText: String {
            switch self {
            case .tradeUpdate: return "Trade update"
            case .chatMessage: return "New message"
            case .offerUpdate: return "Offer update"
            case .general: return "New notification"
            }
        }

        static func from(title: String) -> NotificationCategory {
            let lower = title.lowercased()
            if lower.contains("trade") || lower.contains("payment") || lower.contains("btc") {
                return .tradeUpdate
            }
            if lower.contains("message") || lower.contains("chat") {
                return .chatMessage
            }
            if lower.contains("offer") {
                return .offerUpdate
            }
            return .general
        }
    }

    // MARK: - Diagnostic breadcrumbs

    /// Writes a breadcrumb to the shared app group UserDefaults so the main app
    /// (or a developer inspecting the device) can verify the NSE was invoked.
    private func writeBreadcrumb(stage: String) {
        guard let defaults = UserDefaults(suiteName: NotificationService.APP_GROUP) else { return }
        let entry: [String: String] = [
            "stage": stage,
            "timestamp": ISO8601DateFormatter().string(from: Date()),
        ]
        var breadcrumbs = defaults.array(forKey: NotificationService.NSE_BREADCRUMB_KEY) as? [[String: String]] ?? []
        breadcrumbs.append(entry)
        // Keep bounded — only retain the last 20 breadcrumbs
        if breadcrumbs.count > 20 {
            breadcrumbs = Array(breadcrumbs.suffix(20))
        }
        defaults.set(breadcrumbs, forKey: NotificationService.NSE_BREADCRUMB_KEY)
    }

    // MARK: - Decryption

    private func decryptAESGCM(data: Data, keyData: Data) throws -> Data {
        guard data.count >= NotificationService.NONCE_SIZE + NotificationService.TAG_SIZE else {
            throw NSError(domain: "NSE", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Encrypted data too short"])
        }

        let nonceData = data.prefix(NotificationService.NONCE_SIZE)
        let remaining = data.dropFirst(NotificationService.NONCE_SIZE)
        let ciphertext = remaining.dropLast(NotificationService.TAG_SIZE)
        let tag = remaining.suffix(NotificationService.TAG_SIZE)

        let symmetricKey = SymmetricKey(data: keyData)
        let nonce = try AES.GCM.Nonce(data: nonceData)
        let sealedBox = try AES.GCM.SealedBox(nonce: nonce, ciphertext: ciphertext, tag: tag)
        return try AES.GCM.open(sealedBox, using: symmetricKey)
    }

    // MARK: - Keychain

    private func retrieveSymmetricKey() -> Data? {
        // Note: kSecAttrAccessible is intentionally NOT included in the search query.
        // It is a storage attribute, not a search filter. Including it causes
        // SecItemCopyMatching to silently return errSecItemNotFound if there is
        // any mismatch with how the item was originally stored.
        // kSecAttrAccessGroup MUST be specified because the NSE runs as a separate
        // process with a different bundle ID (and thus different default keychain group)
        // than the main app. Without it, the NSE searches its own group and finds nothing.
        var query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: NotificationService.KEYCHAIN_ACCOUNT,
            kSecAttrService as String: NotificationService.KEYCHAIN_SERVICE,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
        ]
        if let group = NotificationService.KEYCHAIN_ACCESS_GROUP {
            query[kSecAttrAccessGroup as String] = group
        }

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        if status == errSecSuccess, let data = result as? Data {
            return data
        }
        os_log("NSE: SecItemCopyMatching returned status %{public}d", log: log, type: .error, status)
        return nil
    }
}

// MARK: - Notification Payload

private struct NotificationPayload: Decodable {
    let id: String
    let title: String
    let message: String
}
