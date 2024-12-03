import SwiftUI
import domain
import presentation

@main
struct iosClient: App {

//    private let notificationServiceController: NotificationServiceController?
    private let notificationHandler = NotificationHandler()

    init() {
        DependenciesProviderHelper().doInitKoin()
//        TODO not working
//        notificationServiceController = get()
//        if (notificationServiceController != nil) {
//            notificationHandler.setNotificationHandlerImpl(notificationServiceController!)
//        }
//        // Request notification permissions and set the delegate
//        configureNotifications()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

    private func configureNotifications() {
        let center = UNUserNotificationCenter.current()
        center.delegate = notificationHandler  // Custom delegate

        center.requestAuthorization(options: [.alert, .sound, .badge]) { granted, error in
            if let error = error {
                print("Error requesting notifications permission: \(error)")
            }
            if granted {
                DispatchQueue.main.async {
                    UIApplication.shared.registerForRemoteNotifications()
                }
            }
        }
        UNUserNotificationCenter.current().getNotificationSettings { settings in
            print("Notification settings: \(settings)")
            if settings.authorizationStatus == .authorized {
                print("Notifications are authorized.")
            } else {
                print("Notifications are not authorized.")
            }
        }

        // simulation
        let content = UNMutableNotificationContent()
        content.title = "Test Notification"
        content.body = "This is a test notification."
        content.sound = .default

        let request = UNNotificationRequest(identifier: UUID().uuidString, content: content, trigger: nil)
        UNUserNotificationCenter.current().add(request) { error in
            if let error = error {
                print("Failed to add notification: \(error)")
            } else {
                print("Notification added successfully")
            }
        }
    }
}

// Custom notification handler that conforms to `UNUserNotificationCenterDelegate`
class NotificationHandler: NSObject, UNUserNotificationCenterDelegate {
    private var impl: NotificationServiceController?

    func setNotificationHandlerImpl(_ controller: NotificationServiceController) {
        self.impl = controller
    }

    func userNotificationCenter(_ center: UNUserNotificationCenter, willPresent notification: UNNotification, withCompletionHandler completionHandler: @escaping (UNNotificationPresentationOptions) -> Void) {
        // Show the notification while in foreground
        print("Foreground notification received")
        completionHandler([.banner, .sound, .badge])
        impl?.pushNotification(title: "pepe", message: "parada")
    }
}