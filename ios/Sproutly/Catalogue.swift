import Foundation

/// Reads the same Firestore project over its REST API.
///
/// The Android app uses the Firebase SDK. iOS has one too, but it arrives through
/// CocoaPods or Swift Package Manager and drags in a lot of binary for four
/// queries — the REST API is the same data with none of that.
@MainActor
final class Catalogue: ObservableObject {
    @Published var products: [Product] = []
    @Published var isLoading = true
    @Published var error: String?

    private let project = "tab-gl"
    private let apiKey = Bundle.main.object(forInfoDictionaryKey: "FIREBASE_API_KEY") as? String ?? ""

    private var base: URL? {
        URL(string: "https://firestore.googleapis.com/v1/projects/\(project)/databases/(default)/documents")
    }

    func load() async {
        isLoading = true
        error = nil
        defer { isLoading = false }

        guard let base else { return }
        // One equality filter, sorted here: the same shape the Android build uses
        // so neither needs a composite index.
        var components = URLComponents(
            url: base.appendingPathComponent("products"),
            resolvingAgainstBaseURL: false,
        )
        components?.queryItems = [
            URLQueryItem(name: "key", value: apiKey),
            URLQueryItem(name: "pageSize", value: "60"),
        ]
        guard let url = components?.url else { return }

        do {
            let (data, _) = try await URLSession.shared.data(from: url)
            let decoded = try JSONDecoder().decode(FirestoreResponse.self, from: data)
            let docs = decoded.documents ?? []
            products = docs
                .map(Self.product)
                .filter { $0 != nil }
                .compactMap { $0 }
                .sorted { $0.unitsSold > $1.unitsSold }
        } catch {
            self.error = error.localizedDescription
        }
    }

    private static func product(_ doc: FirestoreDocument) -> Product? {
        guard doc.string("status") == "ACTIVE" else { return nil }
        return Product(
            id: doc.id,
            title: doc.string("title") ?? "Untitled",
            description: doc.string("description") ?? "",
            storeName: doc.string("storeName") ?? "Developer",
            price: doc.int("price") ?? 0,
            compareAtPrice: doc.int("compareAtPrice"),
            versionName: doc.string("versionName") ?? "",
            rating: doc.double("rating") ?? 0,
            ratingCount: doc.int("ratingCount") ?? 0,
            unitsSold: doc.int("unitsSold") ?? 0,
            official: doc.bool("official") ?? false,
            images: doc.images("images"),
            apkUrl: doc.string("apkUrl"),
            offerKind: doc.string("offerKind"),
            promoHeadline: doc.string("promoHeadline"),
            promoBody: doc.string("promoBody"),
            promoImageUrl: doc.string("promoImageUrl"),
            offerEndsAt: doc.millis("offerEndsAt"),
        )
    }
}
