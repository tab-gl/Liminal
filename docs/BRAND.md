# Brand

## Palette

| Token | Hex | Where it is used |
| --- | --- | --- |
| **Lime** | `#DBE6A1` | primary container, hero panels, brand mark background |
| **Olive** | `#3F4C06` | primary actions, text on lime, seller revenue card |
| **Espresso** | `#3C3130` | dark canvas, admin hero |
| Lime soft | `#EFF4D8` | chips, skeletons |
| Lime tint | `#F6F9E9` | light surfaces |
| Olive deep | `#2A3304` | text on lime, pressed states |
| Olive mid | `#5B6B14` | secondary accents |
| Moss | `#7C8C3F` | secondary role |
| Cream | `#FAFBF4` | light background |
| Ink | `#23201A` | light-theme text |
| Espresso deep | `#221D1C` | dark background |
| Espresso mid | `#2C2523` | dark surface |
| Clay | `#B4593A` | low-stock / attention |
| Berry | `#B3261E` | errors |

Dark theme swaps the roles: Lime becomes `primary`, Olive becomes
`primaryContainer`, and the background moves to Espresso.

## Logo

The monstera mark is cropped from the supplied artwork and used at every
density:

- `mipmap-*/ic_launcher.png` — lime background, olive mark, legacy icon
- `mipmap-*/ic_launcher_foreground.png` — transparent, 60% safe zone
- `mipmap-anydpi-v26/ic_launcher.xml` — adaptive icon with lime background and
  a `monochrome` layer for themed icons (Android 13+)
- `drawable-nodpi/ic_sproutly_mark.png` — 512px mark used in-app (`BrandMark`,
  login hero, settings card)
- `docs/brand-logo-1080.png` — 1080×1080 asset for store listings

Rules: never recolour the mark, never place it on anything but Lime or Cream,
keep clear space equal to one leaf width.

## Icons

Hugeicons (Free 4.4.0) — a single 24dp stroke family at 1.5 stroke width, so
icons stay optically consistent instead of mixing Material's filled and
outlined sets. Hugeicons has no Android artifact, so the icons the app uses are
compiled into vector drawables by `tools/hugeicons.py` and addressed through
`HugeIcons` in `core/designsystem`.

## Type

System sans (SansSerif) with tight display tracking — `Black` weight at −1.4sp
for display sizes — and quiet, generous body copy at 15sp/23sp. Revenue and
hero prices use a serif (`NumericDisplay`) to make numbers feel editorial.

## Shape and spacing

Radii: cards 22dp, hero panels 30dp, sheets 30dp top, controls fully pill.
Spacing scale: 4 / 8 / 12 / 16 / 24 / 32 / 48, exposed as `LocalSpacing` so no
screen hardcodes padding.

## Motion

Material 3 **Expressive** motion via `MotionScheme.expressive()` — spring-based
spatial and effects specs, and component shape morphing on press, selection and
expansion. Screen transitions stay restrained: fade + 12% horizontal slide,
220–260ms.

Loading uses the M3e `LoadingIndicator` (`SproutlyLoader`): a single shape that
morphs through the Material shape set on a spring. Shimmer is reserved for image
placeholders, where a morph would be noise.
