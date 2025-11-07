#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Bridge class for local encryption/decryption using AES-GCM.
 * This class provides encryption and decryption functionality with secure key storage in the keychain.
 */
@interface LocalEncryptionBridge : NSObject

/**
 * Shared singleton instance
 */
@property (class, nonatomic, readonly) LocalEncryptionBridge *shared;

/**
 * Encrypts data using AES-GCM with the specified key alias.
 * The key will be automatically generated and stored in the keychain if it doesn't exist.
 *
 * @param data The data to encrypt
 * @param keyAlias The alias/identifier for the encryption key in the keychain
 * @param completion Completion handler with encrypted data (nonce + ciphertext + tag) or error
 */
- (void)encryptWithData:(NSData *)data
               keyAlias:(NSString *)keyAlias
             completion:(void (^)(NSData * _Nullable, NSError * _Nullable))completion;

/**
 * Decrypts data using AES-GCM with the specified key alias.
 * The key must already exist in the keychain.
 *
 * @param data The encrypted data (nonce + ciphertext + tag)
 * @param keyAlias The alias/identifier for the decryption key in the keychain
 * @param completion Completion handler with decrypted data or error
 */
- (void)decryptWithData:(NSData *)data
               keyAlias:(NSString *)keyAlias
             completion:(void (^)(NSData * _Nullable, NSError * _Nullable))completion;

/**
 * Synchronously encrypts data using AES-GCM with the specified key alias.
 * For testing purposes only - prefer the async version for production use.
 *
 * @param data The data to encrypt
 * @param keyAlias The alias/identifier for the encryption key in the keychain
 * @param error Output parameter for any error that occurs
 * @return The encrypted data (nonce + ciphertext + tag) or nil if an error occurred
 */
- (NSData * _Nullable)encryptSyncWithData:(NSData *)data
                                 keyAlias:(NSString *)keyAlias
                                    error:(NSError * _Nullable * _Nullable)error;

/**
 * Synchronously decrypts data using AES-GCM with the specified key alias.
 * For testing purposes only - prefer the async version for production use.
 *
 * @param data The encrypted data (nonce + ciphertext + tag)
 * @param keyAlias The alias/identifier for the decryption key in the keychain
 * @param error Output parameter for any error that occurs
 * @return The decrypted data or nil if an error occurred
 */
- (NSData * _Nullable)decryptSyncWithData:(NSData *)data
                                 keyAlias:(NSString *)keyAlias
                                    error:(NSError * _Nullable * _Nullable)error;

/**
 * Private initializer - use shared instance instead
 */
- (instancetype)init NS_UNAVAILABLE;

/**
 * Private initializer - use shared instance instead
 */
+ (instancetype)new NS_UNAVAILABLE;

@end

NS_ASSUME_NONNULL_END
