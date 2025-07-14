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
            .ignoresSafeArea(.all) // Compose has own keyboard handler
            .environmentObject(notificationServiceWrapper)
    }
}
