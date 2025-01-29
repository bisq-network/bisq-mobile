import UIKit
import SwiftUI

import presentation
import domain

struct ComposeView: UIViewControllerRepresentable {
    
    @EnvironmentObject var notificationServiceWrapper: NotificationServiceWrapper
    private let presenter: MainPresenter = get()

    func makeUIViewController(context: Context) -> UIViewController {
        return LifecycleAwareComposeViewController(presenter: presenter, notificationServiceWrapper: notificationServiceWrapper)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    @EnvironmentObject var notificationServiceWrapper: NotificationServiceWrapper

    var body: some View {
        ComposeView()
            .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
            .environmentObject(notificationServiceWrapper)
//            .onAppear {
//                notificationServiceWrapper.notificationServiceController.stopService()
//                NotificationCenter.default.addObserver(forName: UIApplication.willResignActiveNotification, object: nil, queue: .main) { _ in
//                    notificationServiceWrapper.notificationServiceController.startService()
//                }
//                NotificationCenter.default.addObserver(forName: UIApplication.didBecomeActiveNotification, object: nil, queue: .main) { _ in
//                    notificationServiceWrapper.notificationServiceController.stopService()
//                }
//            }.onDisappear {
//                NotificationCenter.default.removeObserver(self)
//            }
    }
}
