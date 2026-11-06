import SwiftUI

/// The same palette as the Android build: lime, olive, espresso.
enum Theme {
    static let lime = Color(red: 0.859, green: 0.902, blue: 0.631)      // #DBE6A1
    static let olive = Color(red: 0.247, green: 0.298, blue: 0.024)     // #3F4C06
    static let espresso = Color(red: 0.235, green: 0.192, blue: 0.188)  // #3C3130
    static let cream = Color(red: 0.976, green: 0.969, blue: 0.937)
    static let surface = Color.white

    static let cardRadius: CGFloat = 26
    static let pillRadius: CGFloat = 999
}

struct Card<Content: View>: View {
    @ViewBuilder var content: Content

    var body: some View {
        content
            .padding(16)
            .frame(maxWidth: .infinity, alignment: .leading)
            .background(Theme.surface)
            .clipShape(RoundedRectangle(cornerRadius: Theme.cardRadius, style: .continuous))
    }
}

struct Pill: View {
    let text: String
    var filled = false

    var body: some View {
        Text(text)
            .font(.caption.weight(.semibold))
            .padding(.horizontal, 10)
            .padding(.vertical, 5)
            .background(filled ? Theme.olive : Theme.lime.opacity(0.6))
            .foregroundStyle(filled ? Theme.lime : Theme.olive)
            .clipShape(Capsule())
    }
}
