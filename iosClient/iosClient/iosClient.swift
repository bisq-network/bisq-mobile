import SwiftUI
import domain
import presentation

@main
struct iosClient: App {

    init() {
        DependenciesProviderHelper().doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }

}