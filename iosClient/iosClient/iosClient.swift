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
        print("KMP: NotificationServiceWrapper init - attempting to resolve ForegroundServiceController")
        print("KMP: Koin instance: \(DependenciesProviderHelper.companion.koin)")

        // Try to get the implementation class directly instead of the protocol
        print("KMP: Attempting to resolve NotificationControllerImpl directly")
        self.notificationControllerImpl = get(NotificationControllerImpl.self)
        print("KMP: ForegroundServiceController resolved successfully")
        
        // Try to get the implementation class directly instead of the protocol
        print("KMP: Attempting to resolve ForegroundServiceControllerImpl directly")
        self.foregroundServiceController = get(ForegroundServiceControllerImpl.self)
        print("KMP: ForegroundServiceController resolved successfully")
        
        print("KMP: Setting up notification controller")
        self.notificationControllerImpl.setup()
        print("KMP: notification controller setup complete")
        
        print("KMP: Registering background task")
        self.foregroundServiceController.registerBackgroundTask()
        print("KMP: Background task registered")
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
