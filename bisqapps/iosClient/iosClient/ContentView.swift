import UIKit
import SwiftUI

import presentation
import domain

struct ComposeView: UIViewControllerRepresentable {

    // TODO DI injection is not fully resolved for iOS. we need to fix this with Koin or worst case ask the
    // view for the presenter its using
    // it can also work because this is just the main presenter that binds to lifecycle, that we allow
    // this hardcoded dependnecy here.
    private let presenter: MainPresenter

    init() {
        let repository = DomainGreetingRepository<DomainGreeting>()
        self.presenter = MainPresenter(greetingRepository: repository)
    }

    func makeUIViewController(context: Context) -> UIViewController {
        return LifecycleAwareComposeViewController(presenter: presenter)
    }

    func updateUIViewController(_ uiViewController: UIViewController, context: Context) {}
}

struct ContentView: View {
    var body: some View {
        ComposeView()
                .ignoresSafeArea(.keyboard) // Compose has own keyboard handler
    }
}
