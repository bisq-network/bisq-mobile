import SwiftUI
import domain

@main
struct iosClient: App {
    init() {
        HelperDIKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}