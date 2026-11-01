import 'dart:async';

import 'package:app_links/app_links.dart';

/// App Links / Universal Links.
///
/// Hosted at sproutly.app, every link resolves to a route the GoRouter tree
/// already knows: /app/<id>, /developer/<uid>, /category/<id>, /campaign/<id>.
/// Cold-start and warm links both land on [uriStream].
class DeepLinkService {
  DeepLinkService({AppLinks? appLinks}) : _links = appLinks ?? AppLinks();

  final AppLinks _links;
  final StreamController<String> _routes = StreamController<String>.broadcast();
  StreamSubscription<Uri>? _sub;

  Stream<String> get routes => _routes.stream;

  Future<void> init() async {
    final Uri? initial = await _links.getInitialLink();
    if (initial != null) _emit(initial);
    _sub = _links.uriLinkStream.listen(_emit, onError: (_) {});
  }

  void _emit(Uri uri) {
    final String route = routeFor(uri);
    if (route.isNotEmpty) _routes.add(route);
  }

  /// Maps an incoming link to an in-app route. Unknown paths are dropped rather
  /// than opening a dead screen.
  static String routeFor(Uri uri) {
    if (uri.host != 'sproutly.app' && uri.host != 'www.sproutly.app') return '';
    final List<String> segments = uri.pathSegments;
    if (segments.isEmpty) return '/discover';
    switch (segments.first) {
      case 'app':
        return segments.length > 1 ? '/app/${segments[1]}' : '/discover';
      case 'developer':
        return segments.length > 1 ? '/developer/${segments[1]}' : '/discover';
      case 'category':
        return segments.length > 1 ? '/category/${segments[1]}' : '/categories';
      case 'campaign':
        return segments.length > 1 ? '/campaign/${segments[1]}' : '/advertising';
      default:
        return '';
    }
  }

  Future<void> dispose() async {
    await _sub?.cancel();
    await _routes.close();
  }
}
