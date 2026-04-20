import UserNotifications
import CryptoKit
import Foundation
import Security

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
    private static let PENDING_NOTIFICATION_KEY = "pending_decrypted_notification"
    private static let KEYCHAIN_SERVICE = "network.bisq.mobile"
    private static let KEYCHAIN_ACCOUNT = "push_notification_symmetric_key"
    private static let KEYCHAIN_ACCESS_GROUP = "$(AppIdentifierPrefix)network.bisq.mobile"

    private var contentHandler: ((UNNotificationContent) -> Void)?
    private var bestAttemptContent: UNMutableNotificationContent?

    override func didReceive(
        _ request: UNNotificationRequest,
        withContentHandler contentHandler: @escaping (UNNotificationContent) -> Void
    ) {
        self.contentHandler = contentHandler
        bestAttemptContent = (request.content.mutableCopy() as? UNMutableNotificationContent)

        guard let bestAttemptContent = bestAttemptContent else {
            contentHandler(request.content)
            return
        }

        guard let encryptedBase64 = request.content.userInfo["encrypted"] as? String,
              let encryptedData = Data(base64Encoded: encryptedBase64) else {
            contentHandler(bestAttemptContent)
            return
        }

        guard let keyData = retrieveSymmetricKey() else {
            bestAttemptContent.title = "Bisq"
            bestAttemptContent.body = "New notification"
            contentHandler(bestAttemptContent)
            return
        }

        do {
            let decryptedData = try decryptAESGCM(data: encryptedData, keyData: keyData)
            let payload = try JSONDecoder().decode(NotificationPayload.self, from: decryptedData)

            // Privacy: show only a category-based summary on the lock screen.
            // Full content is stored for the main app to display after unlock.
            let summary = NotificationCategory.from(title: payload.title)
            bestAttemptContent.title = "Bisq"
            bestAttemptContent.body = summary.displayText

            // Store full decrypted content for in-app display
            storePendingNotification(payload)

            // Pass opaque identifiers only — no human-readable trade details in userInfo
            bestAttemptContent.userInfo = bestAttemptContent.userInfo.merging([
                "nse_decrypted": true,
                "notification_id": payload.id,
                "notification_category": summary.rawValue,
            ]) { _, new in new }
        } catch {
            bestAttemptContent.title = "Bisq"
            bestAttemptContent.body = "New notification"
        }

        contentHandler(bestAttemptContent)
    }

    override func serviceExtensionTimeWillExpire() {
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

    // MARK: - Pending notification storage

    private func storePendingNotification(_ payload: NotificationPayload) {
        // Store in shared UserDefaults (app group) so the main app can read after unlock
        guard let defaults = UserDefaults(suiteName: NotificationService.APP_GROUP) else { return }
        let entry: [String: String] = [
            "id": payload.id,
            "title": payload.title,
            "message": payload.message,
            "timestamp": ISO8601DateFormatter().string(from: Date()),
        ]
        var pending = defaults.array(forKey: NotificationService.PENDING_NOTIFICATION_KEY) as? [[String: String]] ?? []
        pending.append(entry)
        // Keep bounded — drop oldest if over 50
        if pending.count > 50 {
            pending = Array(pending.suffix(50))
        }
        defaults.set(pending, forKey: NotificationService.PENDING_NOTIFICATION_KEY)
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
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: NotificationService.KEYCHAIN_ACCOUNT,
            kSecAttrService as String: NotificationService.KEYCHAIN_SERVICE,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly,
        ]

        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)

        if status == errSecSuccess, let data = result as? Data {
            return data
        }
        return nil
    }
}

// MARK: - Notification Payload

private struct NotificationPayload: Decodable {
    let id: String
    let title: String
    let message: String
}
