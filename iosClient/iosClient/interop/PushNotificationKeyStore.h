#import <Foundation/Foundation.h>

NS_ASSUME_NONNULL_BEGIN

/**
 * Manages the AES-256 symmetric key used for push notification encryption/decryption.
 * The key is stored in a shared Keychain access group so both the main app and the
 * Notification Service Extension (NSE) can access it.
 */
@interface PushNotificationKeyStore : NSObject

/**
 * Shared singleton instance
 */
@property (class, nonatomic, readonly) PushNotificationKeyStore *shared;

/**
 * Returns the Base64-encoded AES-256 symmetric key for push notification encryption.
 * Generates and stores a new key if one doesn't exist yet.
 *
 * @param error Output parameter for any error that occurs
 * @return Base64-encoded key string, or nil if an error occurred
 */
- (NSString * _Nullable)getOrCreateKeyBase64WithError:(NSError * _Nullable * _Nullable)error;

/**
 * Rotates the symmetric key: deletes the old key and generates a fresh one.
 * Called on each device re-registration to limit the exposure window.
 *
 * @param error Output parameter for any error that occurs
 * @return Base64-encoded new key string, or nil if an error occurred
 */
- (NSString * _Nullable)rotateKeyBase64WithError:(NSError * _Nullable * _Nullable)error;

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
