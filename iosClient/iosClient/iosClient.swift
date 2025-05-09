import SwiftUI
import presentation

class NotificationServiceWrapper: ObservableObject {
    @Published var notificationServiceController: DomainNotificationServiceController

    init() {
        self.notificationServiceController = get()
        self.notificationServiceController.registerBackgroundTask()
    }
}

@main
struct iosClient: App {

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
