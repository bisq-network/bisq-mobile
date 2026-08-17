import UserNotifications
import CryptoKit
import Foundation
import Security
import os.log

/// Notification Service Extension that decrypts push notification content before display.
/// This avoids the double-notification problem where iOS first shows a generic alert,
/// then the app wakes up and posts a second decrypted notification.
///
/// Privacy: the banner names the counterparty for a chat message but never quotes it, and never
/// shows amounts or offer details. That is the same line the main app's locally raised
/// notifications draw, which `PushNotification` deliberately matches. Everything else stays a
/// category summary (e.g. "Trade update"). Full decrypted content is stored in the shared app
/// group container for the main app to display after unlock.
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

            // Interpret the payload once; the banner, the tap destination and the userInfo below
            // are all read off the resulting case. Privacy: the banner names the counterparty but
            // never quotes `payload.message`, which is the chat message body.
            let notification = PushNotification.from(payload: payload)
            let summary = notification.category
            bestAttemptContent.title = notification.banner.title
            bestAttemptContent.body = notification.banner.body

            // Pass opaque identifiers only — no human-readable trade details in userInfo.
            // `nse_decrypted` is what `NotificationControllerImpl.ios` filters pre-rendered
            // notifications by; the other two are diagnostic.
            var userInfo: [AnyHashable: Any] = [
                "nse_decrypted": true,
                "notification_id": payload.id,
                "notification_category": summary.rawValue,
            ]
            // Synthesize a `default`-action deep-link URI so the existing main-app
            // AppDelegate.userNotificationCenter(_:didReceive:) handler — already
            // wired for local notifications — routes a tap on a relayed push
            // through the same `ExternalUriHandler.onNewUri(...)` codepath.
            if let deepLink = notification.deepLinkUri {
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

    // MARK: - What a push actually is

    /// The banner shown to the user. Composed here, never taken from the wire.
    private struct Banner {
        let title: String
        let body: String
    }

    /// What a decrypted push actually *is*.
    ///
    /// The wire payload is a bag of optionals — category plus an optional trade id, channel id and
    /// peer name — but only a handful of their combinations are meaningful. `from(payload:)`
    /// collapses that bag into one of the cases below, once, and everything downstream reads what
    /// it needs off the case.
    ///
    /// Mirrors the Kotlin `BisqFirebaseMessagingService.PushNotification` one-to-one, including the
    /// precedence rules, so both platforms banner and route a given payload identically. Kept as a
    /// hand-written mirror because the NSE cannot link the Kotlin shared module — its binary
    /// footprint would blow the NSE memory limit.
    private enum PushNotification {
        /// A message inside a trade's chat: identified, titled and routed by that trade.
        case tradeChatMessage(id: String, tradeId: String, peerUserName: String?)
        /// A direct message outside any trade.
        case privateChatMessage(id: String, channelId: String, peerUserName: String?)
        /// A trade state transition.
        case tradeUpdate(id: String, tradeId: String)
        /// We know the category and nothing else: no routable id, or a category that carries none.
        case categoryOnly(id: String, category: NotificationCategory)

        /// The single point where the wire payload is interpreted. Blanks are normalised to nil
        /// here, so every case below holds values that are actually usable.
        static func from(payload: NotificationPayload) -> PushNotification {
            let category = NotificationCategory.from(payload: payload)
            let tradeId = payload.tradeId?.nonBlank
            let channelId = payload.channelId?.nonBlank
            let peerUserName = payload.peerUserName?.nonBlank

            switch category {
            case .chatMessage:
                // Trade id wins: a message in a trade's chat belongs to the trade, and a producer
                // that sends both means the same conversation either way.
                if let tradeId = tradeId {
                    return .tradeChatMessage(id: payload.id, tradeId: tradeId, peerUserName: peerUserName)
                }
                if let channelId = channelId {
                    return .privateChatMessage(id: payload.id, channelId: channelId, peerUserName: peerUserName)
                }
                return .categoryOnly(id: payload.id, category: category)

            case .tradeUpdate:
                // A trade update's channel id, if a producer ever sent one, is dropped here: a state
                // transition must never land the user in a private conversation.
                guard let tradeId = tradeId else {
                    return .categoryOnly(id: payload.id, category: category)
                }
                return .tradeUpdate(id: payload.id, tradeId: tradeId)

            case .offerUpdate, .general:
                return .categoryOnly(id: payload.id, category: category)
            }
        }

        var category: NotificationCategory {
            switch self {
            case .tradeChatMessage, .privateChatMessage: return .chatMessage
            case .tradeUpdate: return .tradeUpdate
            case .categoryOnly(_, let category): return category
            }
        }

        /// Names the counterparty, never quotes the message.
        ///
        /// KNOWN LIMITATION — these strings are English literals. The extension cannot reach the
        /// generated Kotlin bundles, so it cannot resolve `mobile.properties` keys the way the main
        /// app does. That means the relayed banner is English regardless of device language, while
        /// the locally raised one (`PrivateChatNotificationService`,
        /// `OpenTradesNotificationService`) is localised — the very split `peerUserName` was added
        /// to the wire to avoid, just moved from the node's locale to this file's.
        ///
        /// Not a regression: the category summaries above have always been English literals here.
        /// Do NOT reach for `NSLocalizedString` to fix it. The app resolves its language by looking
        /// `NSLocale.currentLocale.languageCode` — a bare code, "pt" not "pt-BR" — up in a map keyed
        /// `"pt-BR"`, `"af-ZA"`, `"pcm-NG"`, so those three already fall back to English app-wide.
        /// Apple's resolution would match them, and the user would read a Portuguese banner over an
        /// English app. Localising this file properly means mirroring the app's rule, which is worth
        /// doing together with fixing that rule, on both paths at once.
        ///
        /// Falling back to the category banner when there is no peer name is what a trusted node
        /// predating `peerUserName` produces.
        var banner: Banner {
            let categoryBanner = Banner(title: "Bisq", body: category.displayText)
            switch self {
            case .tradeChatMessage(_, let tradeId, let peerUserName):
                guard let peerUserName = peerUserName else { return categoryBanner }
                // bisq2 shortens with substring(0, 8) — see Trade.getShortId().
                return Banner(title: "Trade [\(tradeId.prefix(8))]",
                              body: "You have a new message from \(peerUserName)")

            case .privateChatMessage(_, _, let peerUserName):
                guard let peerUserName = peerUserName else { return categoryBanner }
                return Banner(title: "New message",
                              body: "You received a new message from \(peerUserName)")

            case .tradeUpdate, .categoryOnly:
                return categoryBanner
            }
        }

        /// Where a tap goes, or nil to just open the app.
        ///
        /// Consumed by `AppDelegate.userNotificationCenter(_:didReceive:)` via `userInfo["default"]`,
        /// then routed through `ExternalUriHandler`. The URIs are hand-written mirrors of
        /// `NavRoute.toUriString()` on the Kotlin side.
        var deepLinkUri: String? {
            switch self {
            case .tradeChatMessage(_, let tradeId, _):
                // The trade screen already contains the conversation.
                return "bisq://OpenTrade/\(tradeId)"

            case .privateChatMessage(_, let channelId, _):
                return "bisq://PrivateChat/\(channelId)"

            case .tradeUpdate(_, let tradeId):
                return "bisq://OpenTrade/\(tradeId)"

            case .categoryOnly(_, let category):
                switch category {
                // Somewhere relevant beats nowhere: both trade-scoped categories are about a trade
                // we cannot name, so the trade list is the closest honest destination.
                case .tradeUpdate, .chatMessage:
                    return "bisq://TabMyTrades?initialTab=0"
                case .offerUpdate, .general:
                    return nil
                }
            }
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
    /// Bisq2 chat channel id surfaced by the trusted node. When present (and no
    /// `tradeId`), the main app deep-links a tap on a private message straight to
    /// that conversation. Optional for backward compatibility with older trusted
    /// nodes that predate the private-chat relay support.
    let channelId: String?
    /// Counterparty name surfaced by the trusted node, so the banner can name the sender instead
    /// of showing `title` — which bisq2 builds in the *node's* locale. Optional for backward
    /// compatibility with nodes that predate it; the banner then stays category-only.
    let peerUserName: String?
}

private extension String {
    /// The string, or nil when it holds nothing but whitespace. Lets `PushNotification.from`
    /// normalise the wire's optionals once, so no case downstream repeats a blank check.
    var nonBlank: String? {
        let trimmed = trimmingCharacters(in: .whitespacesAndNewlines)
        return trimmed.isEmpty ? nil : self
    }
}
