import SwiftUI
import UIKit
import UserNotifications
import ClientApp

class AppDelegate: NSObject, UIApplicationDelegate {

    private var registrationCheckTimer: Timer?

    // handles deep links
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
        return true
    }

    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        // Start a timer to check if Kotlin code wants to register for push notifications
        startRegistrationCheckTimer()
        return true
    }

    private func startRegistrationCheckTimer() {
        // Poll every 500ms to check if Kotlin code wants to trigger registration
        registrationCheckTimer = Timer.scheduledTimer(withTimeInterval: 0.5, repeats: true) { [weak self] _ in
            if IosPushNotificationTokenProvider.Companion.shared.shouldTriggerRegistration() {
                print("Triggering remote notification registration from Kotlin request...")
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
        }
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

class NotificationServiceWrapper: ObservableObject {
    @Published var foregroundServiceController: ForegroundServiceControllerImpl
    @Published var notificationControllerImpl: NotificationControllerImpl

    init() {
        // print() log calls are left since this will run only once on app init
        print("NotificationServiceWrapper init - attempting to resolve ForegroundServiceController")
        print("Koin instance: \(DependenciesProviderHelper.companion.koin)")

        // Try to get the implementation class directly instead of the protocol
        print("Attempting to resolve NotificationControllerImpl directly")
        self.notificationControllerImpl = get(NotificationControllerImpl.self)
        print("NotificationController resolved successfully")

        // Try to get the implementation class directly instead of the protocol
        print("Attempting to resolve ForegroundServiceControllerImpl directly")
        self.foregroundServiceController = get(ForegroundServiceControllerImpl.self)
        print("ForegroundServiceController resolved successfully")

        print("Setting up notification controller")
        self.notificationControllerImpl.setup()
        print("notification controller setup complete")

        print("Registering background task")
        self.foregroundServiceController.registerBackgroundTask()
        print("Background task registered")
    }
}

@main
struct iosClient: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

//    @Environment(\.scenePhase) var scenePhase
    @StateObject var notificationServiceWrapper: NotificationServiceWrapper = {
        // Initialize Koin before creating NotificationServiceWrapper
        DependenciesProviderHelper().doInitKoin()
        return NotificationServiceWrapper()
    }()

    init() {
        // Koin is already initialized in the @StateObject closure above
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(notificationServiceWrapper)
//                .onChange(of: scenePhase) { newPhase in
//                    if newPhase == .active {
//                        // ensure no zombie mode - TODO not working causes crash
//                        DependenciesProviderHelper().doInitKoin()
//                    }
//                }
        }
    }
}
