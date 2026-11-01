# Design system

## Brand

| Token | Light | Dark | Use |
| --- | --- | --- | --- |
| Lime | `#DBE6A1` | `#DBE6A1` | Highlights, primary containers, selected nav, featured surfaces |
| Olive | `#3F4C06` | `#DBE6A1` (as primary) | Primary buttons, emphasis, strong accents |
| Warm off-white | `#FAF9F2` | - | App background |
| Soft neutral | `#F0F0E8` | - | Section fills |
| Deep charcoal | `#202117` | - | Primary text |
| Muted text | `#77786B` | `#A9AB9A` | Secondary text (≥ 4.5:1 on its surface) |
| Subtle border | `#E5E6D9` | `#35362A` | Hairlines |
| Ink | - | `#16170F` | Dark background |
| Success / Warning / Error | `#2E6B34` / `#8A5A00` / `#B3261E` | lightened for dark | Status |

No blues, no purples, no accent colour invented outside this table. Contrast was
checked for body text on every surface it appears on, in both themes.

## Material 3 Expressive: the honest situation

Flutter's own documentation states that **M3 Expressive is not available on
Flutter** - the SDK ships Material 3, not the Expressive component set. The
Expressive implementation arrives through the community package
`material_3_expressive` (1.2.0 at the time of writing), which requires
Flutter ≥ 3.47 and builds on the decoupled `material_ui` package that Flutter
3.47 introduced.

Sproutly's approach:

- **Every screen is built from Sproutly's own expressive components**
  (`core/widgets/`), sharing one set of tokens. They are not stock Material
  widgets with a colour swapped: the press behaviour, shape variation and
  haptics are Sproutly's.
- **Three components come from M3E directly**, in exactly one file:
  `core/widgets/m3e/expressive.dart` -
  `M3EButtonGroup` (the connected period selector on the dashboard),
  `M3EFab` (the publish action) and `M3EToolbar.docked` (the action bar on a
  listing page).
- **One file, one upgrade path.** The signatures used there are documented in
  the package's API reference for 1.2.0. If you upgrade the package, open that
  file first - it is the only place that can break. If you drop the package, three
  widgets need a replacement and nothing else changes.

Verify on first run:

```bash
flutter pub add material_3_expressive
flutter analyze lib/core/widgets/m3e/expressive.dart
```

## Tokens

`core/theme/` holds them, and `context.tokens` / `context.colors` /
`context.text` are the accessors used everywhere.

- **Typeface**: **DM Sans**, shipped as one variable font asset
  (`assets/fonts/DMSans.ttf`, wght 100-1000, opsz 9-40, OFL). One file carries
  every weight because each style sets `FontVariation('wght', …)` and
  `FontVariation('opsz', …)` - optical size tracks the rendered size, so
  headlines use the display cut and body copy the text cut. No faux-bold.
- **Type scale** (`sproutly_typography.dart`): display 34-52, headline 22-30,
  title 14-20, body 12-16, label 11-14. Display and headline styles carry
  negative tracking; labels carry positive tracking. Numbers use the metric
  style with `w700` and -1.0 tracking.
- **No gradients.** Hierarchy comes from flat fills, hairline borders, shape
  variation and type weight. `SproutlyTokens.heroSurface` is a single colour, and
  the banner scrim on a listing page is a flat translucent overlay rather than a
  fade.
- **Shapes** (`sproutly_shapes.dart`): `xs 8`, `sm 12`, `md 16`, `lg 22`,
  `xl 28`, `xxl 36`, pill. Cards use `lg` with one corner tightened to `sm`,
  which is what makes a grid read as organic rather than as boxes. Sheets are
  rounded only at the top. Buttons are pills.
- **Motion** (`sproutly_motion.dart`): two families. *Spatial* (260-520 ms,
  `Cubic(0.05, 0.7, 0.1, 1)`) moves or resizes containers; *effects*
  (120-200 ms) fades and scales content. Press feedback is 90 ms with
  `Curves.easeOutBack`. Every custom animation checks
  `MediaQuery.disableAnimations` and collapses to zero duration when reduced
  motion is on.

## Components (`core/widgets/`)

| Widget | Expressive behaviour |
| --- | --- |
| `SproutlyButton` | Scales to 0.96 on press with a spring back, deepens its fill while held, fires a light haptic, swaps the label for a spinner without changing width, keeps a 48dp+ touch target |
| `SproutlyIconButton` | Same press physics on a circular surface; selected state uses the lime container |
| `SproutlyCard` | One large radius with one tightened corner, hairline border instead of a heavy shadow, 0.985 scale on press |
| `SproutlyNavBar` | Detached floating island: inset 18dp from the sides, blurred translucent fill (18 sigma) so the feed reads through it, five slots, a lime capsule that widens behind the active slot, raised olive capsule for Publish, unread badge on Activity, Hugeicons at 1.7 stroke thickening to 2.2 when selected |
| `SproutlyPill` / `PricingBadge` / `StatusChip` / `SponsoredBadge` | Dense capsules; status colours map to the semantic tokens |
| `SproutlyField` / `SproutlySearchField` | Label above the field, filled surface, lime focus ring, counter only when it earns its space |
| `SproutlySheet` | Rounded top, drag handle, title row with a close action, keyboard-aware padding |
| `SproutlyPage` | Safe area, centred content with a 720 max width, optional pull-to-refresh |
| `MetricTile` | Dashboard statistic with an emphasised variant on the lime container |
| `EmptyView` / `ErrorStateView` | Mandatory empty and error states; retry appears only when the error is retryable |
| `Skeleton`, `SkeletonLines`, `AppCardSkeleton`, `RailSkeleton`, `DetailSkeleton` | Shimmer placeholders shaped like the content they replace, so the first paint is a preview rather than a hole. Shimmer stops when reduced motion is on |
| `ShapeMorphLoader`, `ScreenLoader` | Loading indicator that morphs between an ellipse and a squircle: a superellipse sampled at 64 points with the exponent animated 2 → 5, plus a slow quarter turn. A genuine continuous morph, no extra package, one static shape under reduced motion |
| `ApplicationCard` | Three densities (list / compact / grid); always shows icon, name, pitch, category, developer, rating, pricing, and a Sponsored label when paid |

## Iconography

Icons come from **Hugeicons** (`hugeicons: ^4.4.0`, MIT) - 6,000+ stroke-rounded
SVG icons. They are used through `HugeIcon`, not Material's `IconData`, so a
destination stores the icon definition itself:

```dart
SproutlyDestination(label: 'Discover', icon: HugeIcons.strokeRoundedCompass01)
```

| Destination | Icon |
| --- | --- |
| Discover | `strokeRoundedCompass01` |
| Categories | `strokeRoundedDashboardSquare01` |
| Publish | `strokeRoundedAdd01` |
| Activity | `strokeRoundedBell` |
| Profile | `strokeRoundedUser` |

Two conventions: **stroke weight carries state** (1.7 unselected, 2.2 selected)
so selection survives high-contrast mode and colour-blindness, and icons are
never the only signal - every icon in the navigation sits above its label.

Material icons are still used for small inline affordances (chevrons, checks,
status glyphs) where a full SVG is overkill. Hugeicons attribution is included in
the repository README, as the licence asks.

## Theming

`SproutlyTheme.light()` / `.dark()` set only the colour scheme, type scale and
brand tokens. Component styling lives in the widgets themselves rather than in
Material sub-themes, which is why the interface does not look like a Flutter
template: nothing is inherited from a default `ButtonTheme`.

`SproutlyTokens` (a `ThemeExtension`) carries what `ColorScheme` has no slot for:
the lime/olive pair, success/warning containers, card shadow, sponsored
foreground/background and the hero gradient. It interpolates correctly, so theme
switching is smooth.

## Accessibility

- Every interactive element has a semantic label; icon-only buttons use tooltips
  as their accessible name.
- Touch targets are at least 44-48 dp.
- Text scaling is clamped to 0.9-1.6x, and layouts use `Wrap`/`Flexible` rather
  than fixed widths so larger type reflows instead of clipping.
- Status is never conveyed by colour alone: `StatusChip` carries a text label,
  and sponsored placements carry the word "Sponsored".
- Reduced motion is respected by every custom animation.
