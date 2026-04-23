#!/usr/bin/env swift
//
// Unit test for the NSE decryption logic.
// Verifies that AES-256-GCM encryption (matching bisq2 MobileNotificationEncryption.encryptWithSymmetricKey)
// can be correctly decrypted using the same logic as NotificationService.swift.
//
// Run: swift support/test_nse_decryption.swift
//

import Foundation
import CryptoKit

// MARK: - Decryption logic (mirrors NotificationService.swift)

let NONCE_SIZE = 12
let TAG_SIZE = 16

func decryptAESGCM(data: Data, keyData: Data) throws -> Data {
    guard data.count >= NONCE_SIZE + TAG_SIZE else {
        throw NSError(domain: "NSE", code: -1,
                     userInfo: [NSLocalizedDescriptionKey: "Encrypted data too short"])
    }

    let nonceData = data.prefix(NONCE_SIZE)
    let remaining = data.dropFirst(NONCE_SIZE)
    let ciphertext = remaining.dropLast(TAG_SIZE)
    let tag = remaining.suffix(TAG_SIZE)

    let symmetricKey = SymmetricKey(data: keyData)
    let nonce = try AES.GCM.Nonce(data: nonceData)
    let sealedBox = try AES.GCM.SealedBox(nonce: nonce, ciphertext: ciphertext, tag: tag)
    return try AES.GCM.open(sealedBox, using: symmetricKey)
}

// MARK: - Encryption logic (mirrors bisq2 MobileNotificationEncryption.encryptWithSymmetricKey)

func encryptAESGCM(plaintext: Data, keyData: Data) throws -> Data {
    let symmetricKey = SymmetricKey(data: keyData)
    let sealedBox = try AES.GCM.seal(plaintext, using: symmetricKey)

    // Output format: nonce (12) || ciphertext || tag (16)
    // This matches Java's AES/GCM/NoPadding output format used by bisq2
    var result = Data()
    result.append(contentsOf: sealedBox.nonce)
    result.append(sealedBox.ciphertext)
    result.append(sealedBox.tag)
    return result
}

// MARK: - Notification category logic (mirrors NotificationService.swift)

enum NotificationCategory: String {
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

struct NotificationPayload: Codable {
    let id: String
    let title: String
    let message: String
}

// MARK: - Tests

var passed = 0
var failed = 0

func assert(_ condition: Bool, _ message: String, file: String = #file, line: Int = #line) {
    if condition {
        passed += 1
        print("  PASS: \(message)")
    } else {
        failed += 1
        print("  FAIL: \(message) (line \(line))")
    }
}

print("==========================================")
print("NSE Decryption Unit Tests")
print("==========================================")
print("")

// Test 1: Round-trip encrypt/decrypt
print("Test 1: AES-256-GCM round-trip encrypt/decrypt")
do {
    let keyData = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
    let payload = NotificationPayload(id: "test-1", title: "Trade update", message: "Payment confirmed")
    let json = try JSONEncoder().encode(payload)

    let encrypted = try encryptAESGCM(plaintext: json, keyData: keyData)
    let decrypted = try decryptAESGCM(data: encrypted, keyData: keyData)
    let decoded = try JSONDecoder().decode(NotificationPayload.self, from: decrypted)

    assert(decoded.id == "test-1", "ID matches")
    assert(decoded.title == "Trade update", "Title matches")
    assert(decoded.message == "Payment confirmed", "Message matches")
} catch {
    failed += 1
    print("  FAIL: Round-trip threw error: \(error)")
}
print("")

// Test 2: Correct output format (nonce + ciphertext + tag)
print("Test 2: Output format is nonce(12) + ciphertext + tag(16)")
do {
    let keyData = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
    let plaintext = "Hello, world!".data(using: .utf8)!

    let encrypted = try encryptAESGCM(plaintext: plaintext, keyData: keyData)
    assert(encrypted.count == NONCE_SIZE + plaintext.count + TAG_SIZE,
           "Encrypted size = \(NONCE_SIZE) + \(plaintext.count) + \(TAG_SIZE) = \(encrypted.count)")
} catch {
    failed += 1
    print("  FAIL: Threw error: \(error)")
}
print("")

// Test 3: Wrong key fails decryption
print("Test 3: Wrong key fails decryption")
do {
    let correctKey = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
    let wrongKey = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
    let plaintext = "Secret data".data(using: .utf8)!

    let encrypted = try encryptAESGCM(plaintext: plaintext, keyData: correctKey)
    do {
        _ = try decryptAESGCM(data: encrypted, keyData: wrongKey)
        failed += 1
        print("  FAIL: Should have thrown with wrong key")
    } catch {
        passed += 1
        print("  PASS: Decryption correctly fails with wrong key")
    }
} catch {
    failed += 1
    print("  FAIL: Encryption threw error: \(error)")
}
print("")

// Test 4: Truncated data fails
print("Test 4: Truncated data fails")
do {
    let keyData = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
    let shortData = Data([1, 2, 3]) // Too short

    do {
        _ = try decryptAESGCM(data: shortData, keyData: keyData)
        failed += 1
        print("  FAIL: Should have thrown with short data")
    } catch {
        passed += 1
        print("  PASS: Decryption correctly rejects truncated data")
    }
}
print("")

// Test 5: Tampered ciphertext fails
print("Test 5: Tampered ciphertext fails")
do {
    let keyData = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
    let plaintext = "Authentic data".data(using: .utf8)!

    var encrypted = try encryptAESGCM(plaintext: plaintext, keyData: keyData)
    // Flip a byte in the ciphertext (after nonce, before tag)
    encrypted[NONCE_SIZE + 1] ^= 0xFF

    do {
        _ = try decryptAESGCM(data: encrypted, keyData: keyData)
        failed += 1
        print("  FAIL: Should have thrown with tampered data")
    } catch {
        passed += 1
        print("  PASS: Decryption correctly rejects tampered ciphertext")
    }
} catch {
    failed += 1
    print("  FAIL: Encryption threw error: \(error)")
}
print("")

// Test 6: Base64 round-trip (simulates relay payload)
print("Test 6: Base64 round-trip (simulates relay/APNs payload)")
do {
    let keyData = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
    let payload = NotificationPayload(id: "relay-test", title: "New message from peer", message: "Are you ready to trade?")
    let json = try JSONEncoder().encode(payload)

    let encrypted = try encryptAESGCM(plaintext: json, keyData: keyData)
    let base64 = encrypted.base64EncodedString()

    // Simulate NSE receiving Base64 from APNs userInfo
    guard let fromBase64 = Data(base64Encoded: base64) else {
        failed += 1
        print("  FAIL: Base64 decode failed")
        fatalError()
    }

    let decrypted = try decryptAESGCM(data: fromBase64, keyData: keyData)
    let decoded = try JSONDecoder().decode(NotificationPayload.self, from: decrypted)

    assert(decoded.id == "relay-test", "ID matches after Base64 round-trip")
    assert(decoded.title == "New message from peer", "Title matches after Base64 round-trip")
    assert(decoded.message == "Are you ready to trade?", "Message matches after Base64 round-trip")
} catch {
    failed += 1
    print("  FAIL: Base64 round-trip threw error: \(error)")
}
print("")

// Test 7: Notification category classification
print("Test 7: Notification category classification")
assert(NotificationCategory.from(title: "Trade update") == .tradeUpdate, "Trade classified as tradeUpdate")
assert(NotificationCategory.from(title: "Payment confirmed") == .tradeUpdate, "Payment classified as tradeUpdate")
assert(NotificationCategory.from(title: "BTC sent") == .tradeUpdate, "BTC classified as tradeUpdate")
assert(NotificationCategory.from(title: "New message from Alice") == .chatMessage, "Message classified as chatMessage")
assert(NotificationCategory.from(title: "Chat notification") == .chatMessage, "Chat classified as chatMessage")
assert(NotificationCategory.from(title: "Offer accepted") == .offerUpdate, "Offer classified as offerUpdate")
assert(NotificationCategory.from(title: "System alert") == .general, "Unknown classified as general")
print("")

// Test 8: Key from known Base64 (simulates real registration key)
print("Test 8: Known Base64 key encrypt/decrypt")
do {
    let keyBase64 = "MqybkhyRZoc3k8ZcXYxyfN6N49vUNaD5lw5zzIIgxbI="
    guard let keyData = Data(base64Encoded: keyBase64) else {
        failed += 1
        print("  FAIL: Could not decode Base64 key")
        fatalError()
    }
    assert(keyData.count == 32, "Key is 256 bits (32 bytes)")

    let payload = NotificationPayload(id: "key-test", title: "Offer taken", message: "Your offer was taken by Bob")
    let json = try JSONEncoder().encode(payload)
    let encrypted = try encryptAESGCM(plaintext: json, keyData: keyData)
    let decrypted = try decryptAESGCM(data: encrypted, keyData: keyData)
    let decoded = try JSONDecoder().decode(NotificationPayload.self, from: decrypted)

    assert(decoded.title == "Offer taken", "Decryption with known key works")
} catch {
    failed += 1
    print("  FAIL: Known key test threw error: \(error)")
}
print("")

// Test 9: Empty encrypted field is rejected gracefully
print("Test 9: Empty/missing encrypted payload handled gracefully")
do {
    let keyData = Data((0..<32).map { _ in UInt8.random(in: 0...255) })

    // Empty string should fail Base64 decode (returns empty Data)
    let emptyData = Data(base64Encoded: "")!
    do {
        _ = try decryptAESGCM(data: emptyData, keyData: keyData)
        failed += 1
        print("  FAIL: Should have thrown with empty data")
    } catch {
        passed += 1
        print("  PASS: Empty encrypted data correctly rejected")
    }

    // Valid Base64 but too short for nonce+tag (< 28 bytes)
    let shortPayload = Data([0, 1, 2, 3, 4, 5])
    do {
        _ = try decryptAESGCM(data: shortPayload, keyData: keyData)
        failed += 1
        print("  FAIL: Should have thrown with short payload")
    } catch {
        passed += 1
        print("  PASS: Short payload correctly rejected")
    }
}
print("")

// Test 10: Payload with extra JSON fields decodes (forward compatibility)
print("Test 10: Payload with extra fields decodes successfully")
do {
    let keyData = Data((0..<32).map { _ in UInt8.random(in: 0...255) })
    let jsonStr = "{\"id\":\"compat-1\",\"title\":\"Trade update\",\"message\":\"test\",\"extraField\":42}"
    let json = jsonStr.data(using: .utf8)!

    let encrypted = try encryptAESGCM(plaintext: json, keyData: keyData)
    let decrypted = try decryptAESGCM(data: encrypted, keyData: keyData)
    let decoded = try JSONDecoder().decode(NotificationPayload.self, from: decrypted)

    assert(decoded.id == "compat-1", "ID decoded despite extra fields")
    assert(decoded.title == "Trade update", "Title decoded despite extra fields")
} catch {
    failed += 1
    print("  FAIL: Forward compatibility test threw error: \(error)")
}
print("")

// Summary
print("==========================================")
print("Results: \(passed) passed, \(failed) failed")
print("==========================================")

if failed > 0 {
    exit(1)
}
