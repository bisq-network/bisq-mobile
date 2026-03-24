import SwiftUI
import UIKit
import UserNotifications
import ClientApp

class AppDelegate: NSObject, UIApplicationDelegate {

    // handles deep links
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
        return true
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Provide Kotlin with a callback to trigger remote notification registration.
        // This replaces the old polling timer approach — Kotlin invokes this directly
        // when it needs a device token, and APNs delivers the result back via the
        // didRegisterForRemoteNotificationsWithDeviceToken / didFailToRegister callbacks.
        IosPushNotificationTokenProvider.Companion.shared.setRegistrationTrigger {
            DispatchQueue.main.async {
                UIApplication.shared.registerForRemoteNotifications()
            }
        }
        return true
    }

    // MARK: - Push Notification Registration

    func application(_ application: UIApplication, didRegisterForRemoteNotificationsWithDeviceToken deviceToken: Data) {
        // Convert device token to hex string
        let tokenString = deviceToken.map { String(format: "%02.2hhx", $0) }.joined()
        print("APNs device token received: \(tokenString.prefix(20))...")

        // Forward to Kotlin code
        IosPushNotificationTokenProvider.Companion.shared.onTokenReceived(token: tokenString)
    }

    func application(_ application: UIApplication, didFailToRegisterForRemoteNotificationsWithError error: Error) {
        print("Failed to register for remote notifications: \(error.localizedDescription)")

        // Forward error to Kotlin code
        let nsError = error as NSError
        let kotlinError = KotlinThrowable(message: nsError.localizedDescription)
        IosPushNotificationTokenProvider.Companion.shared.onTokenRegistrationFailed(error: kotlinError)
    }
}

@main
struct iosClient: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

    init() {
        // Initialize Koin dependency injection
        DependenciesProviderHelper().doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
