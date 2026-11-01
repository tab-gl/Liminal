import 'dart:async';

import 'package:firebase_analytics/firebase_analytics.dart';
import 'package:material_ui/material_ui.dart';
import 'package:posthog_flutter/posthog_flutter.dart';

import '../config/app_config.dart';

/// Product analytics.
///
/// Firebase Analytics is always on (it is part of the Firebase project).
/// PostHog is opt-in: it initialises only when a project key is compiled in,
/// and every call is a no-op otherwise. No personally identifying data is sent
/// to either provider - only ids and event names.
class AnalyticsService {
  AnalyticsService({FirebaseAnalytics? analytics, bool posthogEnabled = false})
      : _analytics = analytics ?? FirebaseAnalytics.instance,
        posthogEnabled = posthogEnabled && AppConfig.hasPosthog;

  final FirebaseAnalytics _analytics;
  final bool posthogEnabled;

  NavigatorObserver get observer => FirebaseAnalyticsObserver(analytics: _analytics);

  Future<void> init() async {
    await _analytics.setAnalyticsCollectionEnabled(true);
    if (!posthogEnabled) return;
    try {
      final PostHogConfig config = PostHogConfig(AppConfig.posthogApiKey)
        ..host = AppConfig.posthogHost
        ..captureApplicationLifecycleEvents = false
        ..debug = AppConfig.isDev;
      await Posthog().setup(config);
    } catch (_) {
      // Analytics must never break startup.
    }
  }

  Future<void> logEvent(String name, [Map<String, Object>? params]) async {
    await _analytics.logEvent(name: name, parameters: params);
    if (!posthogEnabled) return;
    try {
      await Posthog().capture(eventName: name, properties: params);
    } catch (_) {
      // ignored
    }
  }

  Future<void> screen(String name, {String? applicationId}) =>
      logEvent('screen_view', <String, Object>{
        'screen_name': name,
        if (applicationId != null) 'application_id': applicationId,
      });

  Future<void> setUser(String? uid) async {
    await _analytics.setUserId(id: uid);
    if (!posthogEnabled) return;
    try {
      if (uid == null) {
        await Posthog().reset();
      } else {
        await Posthog().identify(userId: uid);
      }
    } catch (_) {
      // ignored
    }
  }

  // Domain events, named once so dashboards stay stable.
  Future<void> appViewed(String applicationId, {required String source}) => logEvent(
        'app_viewed',
        <String, Object>{'application_id': applicationId, 'source': source},
      );

  Future<void> appClicked(String applicationId, {required String target}) => logEvent(
        'app_clicked',
        <String, Object>{'application_id': applicationId, 'target': target},
      );

  Future<void> bookmarked(String applicationId, {required bool on}) => logEvent(
        on ? 'app_bookmarked' : 'app_unbookmarked',
        <String, Object>{'application_id': applicationId},
      );

  Future<void> publishStarted() => logEvent('publish_started');

  Future<void> publishSubmitted(String applicationId) =>
      logEvent('publish_submitted', <String, Object>{'application_id': applicationId});

  Future<void> reviewSubmitted(String applicationId, int rating) => logEvent(
        'review_submitted',
        <String, Object>{'application_id': applicationId, 'rating': rating},
      );

  Future<void> campaignCreated(String campaignId, int budgetPaise) => logEvent(
        'campaign_created',
        <String, Object>{'campaign_id': campaignId, 'budget_paise': budgetPaise},
      );

  Future<void> searchPerformed(String query, int resultCount) => logEvent(
        'search_performed',
        <String, Object>{'query_length': query.length, 'results': resultCount},
      );
}

/// Exposed for widget tests that need an observer-free navigator.
class NoopAnalytics extends AnalyticsService {
  NoopAnalytics() : super(posthogEnabled: false);

  @override
  Future<void> init() async {}

  @override
  Future<void> logEvent(String name, [Map<String, Object>? params]) async {}

  @override
  Future<void> setUser(String? uid) async {}
}
