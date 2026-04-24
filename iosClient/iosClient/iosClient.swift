import SwiftUI
import UIKit
import UserNotifications
import ClientApp

class AppDelegate: NSObject, UIApplicationDelegate, UNUserNotificationCenterDelegate {

    // handles deep links
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
        return true
    }

    // MARK: - UNUserNotificationCenterDelegate

    // Handle notification tap — must be on the delegate set in didFinishLaunchingWithOptions,
    // otherwise iOS drops the response when the app launches from a terminated state.
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        didReceive response: UNNotificationResponse,
        withCompletionHandler completionHandler: @escaping () -> Void
    ) {
        let userInfo = response.notification.request.content.userInfo
        let actionId = response.actionIdentifier == UNNotificationDefaultActionIdentifier ? "default" : response.actionIdentifier

        if actionId == "default" || actionId == "route" {
            if let uri = userInfo[actionId] as? String {
                ExternalUriHandler.shared.onNewUri(uri: uri)
            }
        }
        completionHandler()
    }

    // Handle notification presentation while app is in foreground
    func userNotificationCenter(
        _ center: UNUserNotificationCenter,
        willPresent notification: UNNotification,
        withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void
    ) {
        let userInfo = notification.request.content.userInfo
        // Suppress all remote push notifications when the app is in foreground.
        // iOS skips the NSE for foreground apps, delivering the raw APNs payload
        // ("Bisq Connect Notification") which is unhelpful. The app's WebSocket
        // handles all trade/chat events directly when active.
        // Local notifications (from OpenTradesNotificationService) don't have "aps"
        // in userInfo, so they pass through unless skipForeground is set.
        let isRemotePush = userInfo["aps"] != nil
        if userInfo["skipForeground"] != nil || isRemotePush {
            completionHandler([])
        } else {
            completionHandler([.alert, .sound, .badge])
        }
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Set the notification center delegate early — before the system delivers any
        // pending notification responses. If the delegate is set too late (e.g., after
        // Compose initializes), tapping a notification while the app is terminated
        // causes the response to be silently dropped by iOS.
        UNUserNotificationCenter.current().delegate = self

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
