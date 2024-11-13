import SwiftUI
import domain
import presentation

@main
struct iosClient: App {
    init() {
        // TODO might need to get away the helper approach in favour of adding koin pods in
        DomainDIHelperKt.doInitKoin()
        PresentationDIHelperKt.doInitKoin()
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
        }
    }
}
