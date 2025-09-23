import SwiftUI
import presentation
import shared

class AppDelegate: NSObject, UIApplicationDelegate {
    
    // handles deep links
    func application(_ app: UIApplication, open url: URL, options: [UIApplication.OpenURLOptionsKey : Any] = [:]) -> Bool {
        ExternalUriHandler.shared.onNewUri(uri: url.absoluteString)
        return true
    }

    // setup delegate to handle user interactions
    func application(_ application: UIApplication, didFinishLaunchingWithOptions launchOptions: [UIApplication.LaunchOptionsKey: Any]?) -> Bool {
        UNUserNotificationCenter.current().delegate = self
        return true
    }

    // handles notification actions
    func userNotificationCenter(_ center: UNUserNotificationCenter, didReceive response: UNNotificationResponse, withCompletionHandler completionHandler: @escaping () -> Void) {
        if response.actionIdentifier.starts(with: "route_") {
            if let userInfo = response.notification.request.content.userInfo as? [String: Any],
               let uri = userInfo[response.actionIdentifier] as? String,
               let url = URL(string: uri) {
                UIApplication.shared.open(url)
            }
        }
        completionHandler()
    }
}

class NotificationServiceWrapper: ObservableObject {
    @Published var notificationServiceController: DomainNotificationServiceController

    init() {
        self.notificationServiceController = get()
        self.notificationServiceController.registerBackgroundTask()
    }
}

@main
struct iosClient: App {
    @UIApplicationDelegateAdaptor(AppDelegate.self) var delegate

//    @Environment(\.scenePhase) var scenePhase
    @StateObject var notificationServiceWrapper = NotificationServiceWrapper()

    init() {
        DependenciesProviderHelper().doInitKoin()
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
