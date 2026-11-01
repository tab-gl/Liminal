import 'package:flutter/foundation.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:google_mobile_ads/google_mobile_ads.dart';
import 'package:material_ui/material_ui.dart';

import '../providers/ads_provider.dart';
import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';

/// A banner placement.
///
/// Behaviour that matters:
///  * the space collapses completely when there is no ad, so a failed load
///    never leaves a hole in the layout;
///  * the ad is disposed with the widget - a leaked `BannerAd` keeps a platform
///    view alive and is a real memory bug;
///  * a "Sponsored" caption sits above the unit, because Sproutly labels every
///    paid placement, including its own ad inventory;
///  * it reloads once on failure with a short backoff rather than hammering
///    the network.
class SproutlyBannerAd extends ConsumerStatefulWidget {
  const SproutlyBannerAd({
    super.key,
    this.placement = 'discover',
    this.margin = const EdgeInsets.symmetric(horizontal: 20, vertical: 8),
  });

  /// Where the unit is rendered. Used for reporting and for the label copy.
  final String placement;
  final EdgeInsets margin;

  @override
  ConsumerState<SproutlyBannerAd> createState() => _SproutlyBannerAdState();
}

class _SproutlyBannerAdState extends ConsumerState<SproutlyBannerAd> {
  BannerAd? _ad;
  bool _loading = false;
  bool _gaveUp = false;
  int _attempts = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _load());
  }

  @override
  void dispose() {
    _ad?.dispose();
    _ad = null;
    super.dispose();
  }

  Future<void> _load() async {
    if (_loading || _gaveUp || _ad != null) return;
    final ads = ref.read(adsServiceProvider);
    if (!ads.isReady) {
      // Bootstrap initialises AdMob; if it is not ready yet, try once more
      // shortly instead of showing nothing forever.
      if (_attempts < 5) {
        _attempts++;
        await Future<void>.delayed(const Duration(milliseconds: 600));
        if (mounted) await _load();
      }
      return;
    }

    _loading = true;
    final BannerAd banner = BannerAd(
      adUnitId: ads.bannerUnitId,
      size: AdSize.banner,
      request: ads.buildRequest(),
      listener: BannerAdListener(
        onAdLoaded: (Ad ad) {
          if (!mounted) {
            ad.dispose();
            return;
          }
          setState(() {
            _ad = ad as BannerAd;
            _loading = false;
          });
        },
        onAdFailedToLoad: (Ad ad, LoadAdError error) {
          ad.dispose();
          if (!mounted) return;
          if (kDebugMode) {
            debugPrint('Sproutly: banner failed on ${widget.placement} (${error.code}: ${error.message})');
          }
          setState(() => _loading = false);
          if (_attempts < 1) {
            _attempts++;
            Future<void>.delayed(const Duration(seconds: 30), () {
              if (mounted) _load();
            });
          } else {
            setState(() => _gaveUp = true);
          }
        },
      ),
    );
    await banner.load();
    if (!mounted) {
      banner.dispose();
      return;
    }
    _loading = false;
  }

  @override
  Widget build(BuildContext context) {
    final BannerAd? ad = _ad;
    if (_gaveUp || ad == null) {
      // While loading, keep a thin skeleton so the layout does not jump when
      // the ad arrives; once it has given up, collapse entirely.
      if (_loading && !_gaveUp) {
        return Padding(
          padding: widget.margin,
          child: Container(
            height: ad == null ? 50 : ad.size.height.toDouble(),
            decoration: BoxDecoration(
              color: context.colors.surfaceContainerHigh.withValues(alpha: 0.5),
              borderRadius: SproutlyShapes.radiusMd,
            ),
          ),
        );
      }
      return const SizedBox.shrink();
    }

    return Padding(
      padding: widget.margin,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            children: <Widget>[
              Icon(Icons.campaign_outlined, size: 11, color: context.colors.onSurfaceVariant),
              const SizedBox(width: 4),
              Text('Ad', style: context.text.labelSmall!.copyWith(fontSize: 9, letterSpacing: 0.6)),
            ],
          ),
          const SizedBox(height: 4),
          ClipRRect(
            borderRadius: SproutlyShapes.radiusMd,
            child: SizedBox(
              width: ad.size.width.toDouble(),
              height: ad.size.height.toDouble(),
              child: AdWidget(ad: ad),
            ),
          ),
        ],
      ),
    );
  }
}
