import Foundation
import CryptoKit
import Security

@objc(LocalEncryptionBridge)
public class LocalEncryptionBridge: NSObject {
    private static let KEY_SIZE = 32 // 256 bits
    private static let NONCE_SIZE = 12 // GCM standard nonce size
    private static let TAG_SIZE = 16 // GCM authentication tag size
    private static let SERVICE_NAME = "network.bisq.mobile"
    
    @objc public static let shared = LocalEncryptionBridge()
    
    // In-memory key storage for testing when keychain is not available
    private var memoryKeyStore: [String: Data] = [:]
    private let memoryKeyStoreLock = NSLock()
    private var useMemoryStore = false
    
    private override init() {
        super.init()
        // Check if keychain is available
        self.useMemoryStore = !isKeychainAvailable()
    }
    
    private func isKeychainAvailable() -> Bool {
        // Try a test query to see if keychain is available
        let testQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: "test",
            kSecReturnData as String: false
        ]
        
        let status = SecItemCopyMatching(testQuery as CFDictionary, nil)
        // If we get errSecItemNotFound or errSecSuccess, keychain is working
        // If we get errSecNotAvailable, keychain is not accessible
        return status != errSecNotAvailable
    }
    
    // MARK: - Key Management
    private func generateSymmetricKeyData() throws -> Data {
        var keyData = Data(count: LocalEncryptionBridge.KEY_SIZE)
        let result = keyData.withUnsafeMutableBytes { bytes in
            guard let baseAddress = bytes.baseAddress else {
                return errSecParam
            }
            return SecRandomCopyBytes(kSecRandomDefault, LocalEncryptionBridge.KEY_SIZE, baseAddress)
        }
        
        guard result == errSecSuccess else {
            throw NSError(domain: "LocalEncryption", code: Int(result),
                        userInfo: [NSLocalizedDescriptionKey: "Failed to generate random bytes"])
        }
        
        return keyData
    }

    private func storeKeyInKeychain(keyData: Data, keyAlias: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrLabel as String: keyAlias,
            kSecAttrAccount as String: keyAlias,
            kSecAttrService as String: LocalEncryptionBridge.SERVICE_NAME,
            kSecValueData as String: keyData,
            kSecAttrAccessible as String: kSecAttrAccessibleAfterFirstUnlockThisDeviceOnly
        ]
        
        let status = SecItemAdd(query as CFDictionary, nil)
        guard status == errSecSuccess || status == errSecDuplicateItem else {
            throw NSError(domain: "LocalEncryption", code: Int(status),
                        userInfo: [NSLocalizedDescriptionKey: "Failed to store key: \(status)"])
        }
    }

    private func retrieveKeyFromKeychain(keyAlias: String) throws -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: keyAlias,
            kSecAttrService as String: LocalEncryptionBridge.SERVICE_NAME,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        
        var result: CFTypeRef?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        
        if status == errSecItemNotFound {
            return nil
        }
        
        guard status == errSecSuccess else {
            throw NSError(domain: "LocalEncryption", code: Int(status),
                        userInfo: [NSLocalizedDescriptionKey: "Failed to retrieve key: \(status)"])
        }
        
        return result as? Data
    }

    private func getOrCreateKey(keyAlias: String) throws -> Data {
        if useMemoryStore {
            // Lock the entire check-and-set operation for memory store
            memoryKeyStoreLock.lock()
            defer { memoryKeyStoreLock.unlock() }
            
            // Check if key exists
            if let existingKey = memoryKeyStore[keyAlias] {
                return existingKey
            }
            
            // Generate and store new key
            let keyData = try generateSymmetricKeyData()
            memoryKeyStore[keyAlias] = keyData
            return keyData
        } else {
            // For keychain, check if key exists
            if let existingKey = try retrieveKeyFromKeychain(keyAlias: keyAlias) {
                return existingKey
            }
            
            // Generate and store new key
            let keyData = try generateSymmetricKeyData()
            try storeKeyInKeychain(keyData: keyData, keyAlias: keyAlias)
            
            // Retrieve to confirm storage (handles race condition where another thread stored first)
            guard let key = try retrieveKeyFromKeychain(keyAlias: keyAlias) else {
                throw NSError(domain: "LocalEncryption", code: -1,
                            userInfo: [NSLocalizedDescriptionKey: "Failed to retrieve generated key"])
            }
            
            return key
        }
    }
    
    // MARK: - Encryption
    
    @objc public func encrypt(data: Data, keyAlias: String, completion: @escaping (Data?, Error?) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let result = try self.encryptSync(data: data, keyAlias: keyAlias)
                DispatchQueue.main.async {
                    completion(result, nil)
                }
            } catch {
                DispatchQueue.main.async {
                    completion(nil, error)
                }
            }
        }
    }
    
    private func encryptSync(data: Data, keyAlias: String) throws -> Data {
        let keyData = try getOrCreateKey(keyAlias: keyAlias)
        
        // Generate random nonce
        var nonceData = Data(count: LocalEncryptionBridge.NONCE_SIZE)
        let nonceResult = nonceData.withUnsafeMutableBytes { bytes in
            SecRandomCopyBytes(kSecRandomDefault, LocalEncryptionBridge.NONCE_SIZE, bytes.baseAddress!)
        }
        
        guard nonceResult == errSecSuccess else {
            throw NSError(domain: "LocalEncryption", code: Int(nonceResult),
                         userInfo: [NSLocalizedDescriptionKey: "Failed to generate nonce"])
        }
        
        // Create symmetric key and encrypt
        let symmetricKey = SymmetricKey(data: keyData)
        let nonce = try AES.GCM.Nonce(data: nonceData)
        let sealedBox = try AES.GCM.seal(data, using: symmetricKey, nonce: nonce)
        
        // Combine nonce + ciphertext + tag
        var result = Data()
        result.append(nonceData)
        result.append(sealedBox.ciphertext)
        result.append(sealedBox.tag)
        
        return result
    }
    
    // MARK: - Decryption
    
    @objc public func decrypt(data: Data, keyAlias: String, completion: @escaping (Data?, Error?) -> Void) {
        DispatchQueue.global(qos: .userInitiated).async {
            do {
                let result = try self.decryptSync(data: data, keyAlias: keyAlias)
                DispatchQueue.main.async {
                    completion(result, nil)
                }
            } catch {
                DispatchQueue.main.async {
                    completion(nil, error)
                }
            }
        }
    }
    
    private func decryptSync(data: Data, keyAlias: String) throws -> Data {
        let keyData = try getOrCreateKey(keyAlias: keyAlias)
        
        guard data.count >= LocalEncryptionBridge.NONCE_SIZE + LocalEncryptionBridge.TAG_SIZE else {
            throw NSError(domain: "LocalEncryption", code: -1,
                         userInfo: [NSLocalizedDescriptionKey: "Invalid encrypted data: too short"])
        }
        
        // Extract nonce, ciphertext, and tag
        let nonceData = data.prefix(LocalEncryptionBridge.NONCE_SIZE)
        let remaining = data.dropFirst(LocalEncryptionBridge.NONCE_SIZE)
        let tagSize = LocalEncryptionBridge.TAG_SIZE
        let ciphertext = remaining.dropLast(tagSize)
        let tag = remaining.suffix(tagSize)
        
        // Create symmetric key and decrypt
        let symmetricKey = SymmetricKey(data: keyData)
        let nonce = try AES.GCM.Nonce(data: nonceData)
        let sealedBox = try AES.GCM.SealedBox(nonce: nonce, ciphertext: ciphertext, tag: tag)
        let decryptedData = try AES.GCM.open(sealedBox, using: symmetricKey)
        
        return decryptedData
    }
    
    // MARK: - Synchronous Methods (for testing)
    
    @objc public func encryptSyncWith(data: Data, keyAlias: String, error: NSErrorPointer) -> Data? {
        do {
            return try encryptSync(data: data, keyAlias: keyAlias)
        } catch let encryptError as NSError {
            error?.pointee = encryptError
            return nil
        }
    }
    
    @objc public func decryptSyncWith(data: Data, keyAlias: String, error: NSErrorPointer) -> Data? {
        do {
            return try decryptSync(data: data, keyAlias: keyAlias)
        } catch let decryptError as NSError {
            error?.pointee = decryptError
            return nil
        }
    }
}
