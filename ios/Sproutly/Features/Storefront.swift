import SwiftUI

struct StorefrontView: View {
    @EnvironmentObject var catalogue: Catalogue

    private let columns = [GridItem(.flexible(), spacing: 12), GridItem(.flexible(), spacing: 12)]

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(alignment: .leading, spacing: 16) {
                    header

                    if let error = catalogue.error {
                        Card {
                            Text("Could not load the store")
                                .font(.headline)
                            Text(error)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }

                    if catalogue.isLoading {
                        ForEach(0..<4, id: \.self) { _ in
                            RoundedRectangle(cornerRadius: Theme.cardRadius, style: .continuous)
                                .fill(Theme.lime.opacity(0.35))
                                .frame(height: 150)
                        }
                    } else if catalogue.products.isEmpty {
                        Card {
                            Text("Nothing published yet")
                                .font(.headline)
                            Text("New apps show up here.")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    } else {
                        LazyVGrid(columns: columns, spacing: 12) {
                            ForEach(catalogue.products) { product in
                                NavigationLink(value: product.id) {
                                    AppCard(product: product)
                                }
                                .buttonStyle(.plain)
                            }
                        }
                    }
                }
                .padding(20)
            }
            .background(Theme.cream)
            .navigationDestination(for: String.self) { id in
                if let product = catalogue.products.first(where: { $0.id == id }) {
                    DetailView(product: product)
                }
            }
            .refreshable { await catalogue.load() }
        }
    }

    private var header: some View {
        VStack(alignment: .leading, spacing: 4) {
            HStack(spacing: 10) {
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Theme.lime)
                    .frame(width: 34, height: 34)
                    .overlay(
                        Image(systemName: "leaf.fill")
                            .foregroundStyle(Theme.olive)
                    )
                Text("Sproutly")
                    .font(.system(size: 30, weight: .bold))
                    .foregroundStyle(Theme.espresso)
            }
            Text("An app store for independent developers.")
                .font(.subheadline)
                .foregroundStyle(Theme.espresso.opacity(0.6))
        }
        .padding(.bottom, 4)
    }
}

struct AppCard: View {
    let product: Product

    var body: some View {
        VStack(alignment: .leading, spacing: 8) {
            artwork
            Text(product.title)
                .font(.subheadline.weight(.semibold))
                .lineLimit(1)
                .foregroundStyle(Theme.espresso)
            HStack(spacing: 6) {
                if product.isOnOffer {
                    Pill(text: "OFFER", filled: true)
                } else if product.official {
                    Pill(text: "OFFICIAL", filled: true)
                }
                Text(product.isFree ? "Free" : "₹\(product.price / 100)")
                    .font(.caption.weight(.semibold))
                    .foregroundStyle(Theme.olive)
            }
        }
        .padding(10)
        .background(Theme.surface)
        .clipShape(RoundedRectangle(cornerRadius: Theme.cardRadius, style: .continuous))
    }

    private var artwork: some View {
        let url = product.images.first.flatMap { URL(string: $0.url) }
        return AsyncImage(url: url) { phase in
            switch phase {
            case .success(let image):
                image.resizable().aspectRatio(contentMode: .fill)
            default:
                ZStack {
                    Theme.lime.opacity(0.5)
                    Text(String(product.title.prefix(1)))
                        .font(.system(size: 34, weight: .bold))
                        .foregroundStyle(Theme.olive.opacity(0.5))
                }
            }
        }
        .frame(height: 120)
        .frame(maxWidth: .infinity)
        .clipShape(RoundedRectangle(cornerRadius: 18, style: .continuous))
    }
}

struct OffersView: View {
    @EnvironmentObject var catalogue: Catalogue

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 12) {
                    let offers = catalogue.products.filter { $0.isOnOffer || $0.offerKind != nil }
                    if offers.isEmpty {
                        Card {
                            Text("No offers right now")
                                .font(.headline)
                            Text("Developers post codes and discounts here.")
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    } else {
                        ForEach(offers) { product in
                            Card {
                                Text(product.promoHeadline ?? product.title)
                                    .font(.headline)
                                Text(product.promoBody ?? "On offer now.")
                                    .font(.footnote)
                                    .foregroundStyle(.secondary)
                            }
                        }
                    }
                }
                .padding(20)
            }
            .background(Theme.cream)
            .navigationTitle("Offers")
        }
    }
}

struct SearchView: View {
    @EnvironmentObject var catalogue: Catalogue
    @State private var query = ""

    var body: some View {
        NavigationStack {
            ScrollView {
                LazyVStack(spacing: 10) {
                    ForEach(catalogue.products.filter {
                        query.isEmpty || $0.title.localizedCaseInsensitiveContains(query)
                    }) { product in
                        Card {
                            Text(product.title).font(.headline)
                            Text(product.storeName)
                                .font(.footnote)
                                .foregroundStyle(.secondary)
                        }
                    }
                }
                .padding(20)
            }
            .background(Theme.cream)
            .searchable(text: $query, prompt: "Search apps")
            .navigationTitle("Search")
        }
    }
}
