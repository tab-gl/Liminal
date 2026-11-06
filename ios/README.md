# Sproutly for iOS

A SwiftUI client for the same Firestore project. It reads the catalogue over the
Firestore REST API, so there is no Firebase SDK and no CocoaPods.

## What it does

- Storefront, Offers, Search and You tabs, in the same lime/olive palette.
- Listing detail with artwork, price, installs, description and the build link.
- Reads the same `products` collection, one equality filter, sorted in the app —
  the same shape as the Android build, so neither needs a composite index.

## What it cannot do

**Install apps.** iOS only installs from the App Store. A listing shows its build
and links to it; there is no equivalent of `REQUEST_INSTALL_PACKAGES` and no
Shizuku. This is a platform rule, not an omission.

## Build

```bash
brew install xcodegen
cd ios && xcodegen generate && open Sproutly.xcodeproj
```

## IPA

`.github/workflows/ios.yml` builds on a macOS runner, packages `Payload/` into
`Sproutly-unsigned.ipa` and uploads it as an artifact.

The IPA is **unsigned**: it installs through AltStore, Sideloadly or a paid Apple
Developer certificate, not by tapping it. Signing needs your team id and
certificates as secrets, which are not in this repo.
