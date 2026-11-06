import SwiftUI

@main
struct SproutlyApp: App {
    @StateObject private var catalogue = Catalogue()

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(catalogue)
                .tint(Theme.olive)
                .preferredColorScheme(.light)
        }
    }
}

struct RootView: View {
    @EnvironmentObject var catalogue: Catalogue

    var body: some View {
        TabView {
            StorefrontView()
                .tabItem { Label("Apps", systemImage: "square.grid.2x2") }
            OffersView()
                .tabItem { Label("Offers", systemImage: "gift") }
            SearchView()
                .tabItem { Label("Search", systemImage: "magnifyingglass") }
            SettingsView()
                .tabItem { Label("You", systemImage: "person") }
        }
        .task { await catalogue.load() }
    }
}
