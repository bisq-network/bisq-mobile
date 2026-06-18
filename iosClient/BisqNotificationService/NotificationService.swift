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
            // Prefer the explicit `payload.category` from the trusted node — only
            // fall back to the title-keyword heuristic for older bisq2 versions
            // that don't yet populate `category`. Mirrors the Android-side fix in
            // bisq-network/bisq-mobile#1450.
            let summary = NotificationCategory.from(payload: payload)
            bestAttemptContent.title = "Bisq"
            bestAttemptContent.body = summary.displayText

            // Pass opaque identifiers only — no human-readable trade details in userInfo.
            // notification_trade_id is forwarded ONLY when the trusted node
            // emitted it (bisq-network/bisq-mobile#1395), letting the main app's
            // notification tap handler deep-link straight to the specific trade.
            // Older trusted nodes omit the field; the main app falls back to the
            // category-only routing in that case.
            var userInfo: [AnyHashable: Any] = [
                "nse_decrypted": true,
                "notification_id": payload.id,
                "notification_category": summary.rawValue,
            ]
            if let tradeId = payload.tradeId, !tradeId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                userInfo["notification_trade_id"] = tradeId
            }
            // Synthesize a `default`-action deep-link URI so the existing main-app
            // AppDelegate.userNotificationCenter(_:didReceive:) handler — already
            // wired for local notifications — routes a tap on a relayed push
            // through the same `ExternalUriHandler.onNewUri(...)` codepath. The
            // URI scheme matches NavRoute.toUriString() on the Kotlin side
            // (bisq://OpenTrade/<id> | bisq://TabMyTrades?initialTab=0), keeping
            // Android and iOS routing identical. Mirrors the Android
            // deepLinkRouteFor(category, tradeId) logic.
            if let deepLink = summary.deepLinkUri(tradeId: payload.tradeId) {
                userInfo["default"] = deepLink
            }
            bestAttemptContent.userInfo = bestAttemptContent.userInfo.merging(userInfo) { _, new in new }

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

        /// Deep-link URI to use for a notification tap. Matches the Android
        /// `deepLinkRouteFor(category, tradeId)` mapping exactly:
        /// - Trade-scoped categories with a `tradeId` → `bisq://OpenTrade/<id>`.
        /// - Trade-scoped categories without `tradeId` (older trusted nodes that
        ///   predate bisq-network/bisq-mobile#1395) → trade list fallback.
        /// - Offer/general → nil (no deep link; tap just opens the app).
        ///
        /// The URI is consumed by `AppDelegate.userNotificationCenter(_:didReceive:)`
        /// via `userInfo["default"]`, then routed through `ExternalUriHandler`.
        func deepLinkUri(tradeId: String?) -> String? {
            switch self {
            case .tradeUpdate, .chatMessage:
                if let tradeId = tradeId, !tradeId.trimmingCharacters(in: .whitespacesAndNewlines).isEmpty {
                    return "bisq://OpenTrade/\(tradeId)"
                }
                // Trade list. Matches NavRoute.TabMyTrades.toUriString() with
                // initialTab=0 (TAB_OPEN). Hard-coded mirror of the Kotlin
                // route because the NSE can't link the Kotlin shared module
                // (its binary footprint would explode the NSE memory limit).
                return "bisq://TabMyTrades?initialTab=0"
            case .offerUpdate, .general:
                return nil
            }
        }

        /// Resolves the category from the decrypted payload, preferring the
        /// explicit `category` id over the brittle title-keyword heuristic.
        ///
        /// - When `payload.category` is present and recognized, use it. This is
        ///   the stable wire signal from the trusted node.
        /// - When `payload.category` is present but unknown to this client
        ///   (e.g. a newer bisq2 introduced a new id like `dispute_alert`),
        ///   return `.general` rather than running the title heuristic — the
        ///   node already told us it's a specific category, so a generic banner
        ///   is more honest than guessing.
        /// - When `payload.category` is absent (older bisq2 that predates
        ///   bisq-network/bisq-mobile#1450), fall back to title-keyword
        ///   scanning. This matches the Android-side `fromPayload` contract.
        static func from(payload: NotificationPayload) -> NotificationCategory {
            if let categoryId = payload.category {
                return NotificationCategory(rawValue: categoryId) ?? .general
            }
            return from(title: payload.title)
        }

        static func from(title: String) -> NotificationCategory {
            let lower = title.lowercased()
            // Chat keyword check is intentionally ordered BEFORE the trade/payment/btc
            // check: trade-private chat titles built by bisq2 (e.g.
            // "Alice (Bisq Easy → Open Trades → Bob)") contain "trade" but no chat
            // keyword, so they'll still mislabel as trade-update on older nodes —
            // the explicit `category` path above is the real fix. For titles that
            // contain BOTH (e.g. "Trade chat update"), chat wins. Mirrors the
            // Android ordering tested by `fromTitle prefers chat over trade keywords`.
            if lower.contains("message") || lower.contains("chat") {
                return .chatMessage
            }
            if lower.contains("trade") || lower.contains("payment") || lower.contains("btc") {
                return .tradeUpdate
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
    /// Stable category id emitted by the trusted node (bisq2 #1450). Mirrors
    /// `Notification.Category#getId` on the bisq2 side and
    /// `BisqFirebaseMessagingService.NotificationCategory#id` on Android. Optional
    /// for backward compatibility with trusted nodes that don't yet populate it.
    let category: String?
    /// Bisq2 trade id surfaced by the trusted node (bisq-network/bisq-mobile#1395).
    /// When present, the main app deep-links a notification tap straight to the
    /// trade screen instead of the open-trade list. Optional for backward
    /// compatibility with older trusted nodes that don't emit it.
    let tradeId: String?
}
