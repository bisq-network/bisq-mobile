import UIKit
import SwiftUI
import ClientApp

struct ComposeView: UIViewControllerRepresentable {

    private let presenter: MainPresenter = get()

    func makeUIViewController(context: Context) -> UIViewController {
        return LifecycleAwareComposeViewController(presenter: presenter)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
            .ignoresSafeArea(.all) // Compose has own keyboard handler
    }
}
