import 'package:flutter_riverpod/flutter_riverpod.dart';

import '../services/ads_service.dart';

/// The AdMob service. Overridden in bootstrap with the initialised instance so
/// no widget triggers platform-channel work during a build.
final Provider<AdsService> adsServiceProvider =
    Provider<AdsService>((Ref ref) => AdsService());
