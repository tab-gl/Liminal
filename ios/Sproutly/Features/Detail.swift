import SwiftUI

struct DetailView: View {
    let product: Product

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 18) {
                hero

                Text(product.title)
                    .font(.system(size: 26, weight: .bold))
                    .foregroundStyle(Theme.espresso)

                HStack(spacing: 8) {
                    if product.official { Pill(text: "OFFICIAL", filled: true) }
                    Pill(text: product.isFree ? "Free" : "₹\(product.price / 100)")
                    Text("\(product.installsLabel) installs")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }

                Text(product.description)
                    .font(.body)
                    .foregroundStyle(Theme.espresso.opacity(0.8))

                installCard

                Text("About this app")
                    .font(.headline)
                Text(product.storeName)
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
            .padding(20)
        }
        .background(Theme.cream)
        .navigationBarTitleDisplayMode(.inline)
    }

    private var hero: some View {
        let url = product.images.first.flatMap { URL(string: $0.url) }
        return AsyncImage(url: url) { phase in
            if case .success(let image) = phase {
                image.resizable().aspectRatio(contentMode: .fill)
            } else {
                ZStack {
                    Theme.lime.opacity(0.5)
                    Text(String(product.title.prefix(1)))
                        .font(.system(size: 60, weight: .bold))
                        .foregroundStyle(Theme.olive.opacity(0.5))
                }
            }
        }
        .frame(height: 220)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 30, style: .continuous))
    }

    private var installCard: some View {
        Card {
            VStack(alignment: .leading, spacing: 10) {
                Text(product.apkUrl == nil ? "No build attached yet" : "Android build")
                    .font(.headline)

                // iOS cannot install an app from outside the App Store, so this
                // opens the listing rather than pretending otherwise.
                if let raw = product.apkUrl, let url = URL(string: raw) {
                    Link(destination: url) {
                        Text("Open the build")
                            .font(.subheadline.weight(.semibold))
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 12)
                            .background(Theme.olive)
                            .foregroundStyle(Theme.lime)
                            .clipShape(Capsule())
                    }
                }

                Text("iOS installs only from the App Store. Sproutly shows the listing and links to the build.")
                    .font(.caption)
                    .foregroundStyle(.secondary)
            }
        }
    }
}

struct SettingsView: View {
    @EnvironmentObject var catalogue: Catalogue

    var body: some View {
        NavigationStack {
            List {
                Section("About") {
                    LabeledContent("Version", value: Bundle.main
                        .object(forInfoDictionaryKey: "CFBundleShortVersionString") as? String ?? "—")
                    LabeledContent("Apps in the store", value: "\(catalogue.products.count)")
                }
                Section("Legal") {
                    NavigationLink("Privacy policy") { LegalView(terms: false) }
                    NavigationLink("Terms of service") { LegalView(terms: true) }
                }
            }
            .navigationTitle("You")
        }
    }
}

struct LegalView: View {
    let terms: Bool

    var body: some View {
        ScrollView {
            VStack(alignment: .leading, spacing: 12) {
                Text(terms ? "Terms of service" : "Privacy policy")
                    .font(.system(size: 26, weight: .bold))
                Text(bodyText)
                    .font(.body)
                    .foregroundStyle(Theme.espresso.opacity(0.8))
            }
            .padding(20)
        }
        .background(Theme.cream)
    }

    private var bodyText: String {
        terms
            ? "Publish only what you have the right to publish. Builds are scanned when they are uploaded. A listing can be removed if it breaks these terms."
            : "We store your account email, the apps you install and the reviews you write. Builds are hosted on Cloudflare R2; images on Cloudinary."
    }
}
