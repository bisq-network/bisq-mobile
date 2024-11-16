import SwiftUI
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
