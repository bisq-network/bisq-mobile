import SwiftUI
import presentation

class NotificationServiceWrapper: ObservableObject {
    @Published var notificationServiceController: DomainNotificationServiceController

    init() {
        self.notificationServiceController = get()
    }
}

@main
struct iosClient: App {
    @StateObject var notificationServiceWrapper = NotificationServiceWrapper()

    init() {
        DependenciesProviderHelper().doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .environmentObject(notificationServiceWrapper)
        }
    }
}
