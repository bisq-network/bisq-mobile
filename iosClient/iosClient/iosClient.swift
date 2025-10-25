import SwiftUI
import presentation
import UIKit
import UserNotifications

class AppDelegate: NSObject, UIApplicationDelegate {
    
    // handles deep links
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
        return true
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
