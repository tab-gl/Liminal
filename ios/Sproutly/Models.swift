import Foundation

/// A listing, as the Android app models it.
struct Product: Identifiable, Decodable {
    let id: String
    let title: String
    let description: String
    let storeName: String
    let price: Int
    let compareAtPrice: Int?
    let versionName: String
    let rating: Double
    let ratingCount: Int
    let unitsSold: Int
    let official: Bool
    let images: [ProductImage]
    let apkUrl: String?
    let offerKind: String?
    let promoHeadline: String?
    let promoBody: String?
    let promoImageUrl: String?
    let offerEndsAt: Double?

    var isFree: Bool { price == 0 }
    var isOnOffer: Bool { (compareAtPrice ?? 0) > price }

    /// Installs are rounded the way the store shows them: 5+, 1k+.
    var installsLabel: String {
        switch unitsSold {
        case 0: return "New"
        case ..<10: return "5+"
        case ..<50: return "40+"
        case ..<100: return "50+"
        case ..<1_000: return "100+"
        case ..<10_000: return "1k+"
        case ..<100_000: return "10k+"
        case ..<1_000_000: return "100k+"
        default: return "1M+"
        }
    }
}

struct ProductImage: Decodable {
    let url: String
}

/// Firestore REST returns every value as a tagged object; this flattens one.
struct FirestoreValue: Decodable {
    let stringValue: String?
    let integerValue: String?
    let doubleValue: Double?
    let booleanValue: Bool?
    let arrayValue: ArrayValue?
    let timestampValue: String?
    let nullValue: String?

    struct ArrayValue: Decodable {
        let values: [FirestoreValue]?
    }
}

struct FirestoreDocument: Decodable {
    let name: String
    let fields: [String: FirestoreValue]
}

struct FirestoreResponse: Decodable {
    let documents: [FirestoreDocument]?
}

extension FirestoreDocument {
    var id: String { name.components(separatedBy: "/").last ?? name }

    func string(_ key: String) -> String? { fields[key]?.stringValue }
    func int(_ key: String) -> Int? { fields[key]?.integerValue.flatMap(Int.init) }
    func double(_ key: String) -> Double? {
        fields[key]?.doubleValue ?? fields[key]?.integerValue.flatMap(Double.init)
    }
    func bool(_ key: String) -> Bool? { fields[key]?.booleanValue }
    func millis(_ key: String) -> Double? {
        guard let raw = fields[key]?.timestampValue else { return nil }
        return ISO8601DateFormatter().date(from: raw)?.timeIntervalSince1970
    }
    func images(_ key: String) -> [ProductImage] {
        let values = fields[key]?.arrayValue?.values ?? []
        return values.compactMap { $0.stringValue }.map(ProductImage.init(url:))
    }
}
