import 'package:cloud_firestore/cloud_firestore.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sproutly/shared/models/application.dart';
import 'package:sproutly/shared/models/enums.dart';

Map<String, dynamic> _doc() => <String, dynamic>{
      'ownerId': 'dev-1',
      'ownerName': 'Asha Rao',
      'name': 'Sprout Notes',
      'tagline': 'Notes that grow with you',
      'description': 'A' * 80,
      'categoryId': 'productivity',
      'tags': <String>['notes', 'offline'],
      'iconUrl': 'https://cdn.sproutly.app/icon.png',
      'screenshots': <String>['https://cdn.sproutly.app/1.png'],
      'platforms': <String>['android', 'ios'],
      'pricingModel': 'freemium',
      'status': 'approved',
      'rating': 4.5,
      'reviewCount': 12,
      'viewCount': 900,
      'clickCount': 90,
      'sponsored': true,
      'featured': false,
      'search': <String, dynamic>{
        'title': 'sprout notes',
        'keywords': <String>['notes', 'offline'],
      },
      'createdAt': Timestamp.fromDate(DateTime.utc(2026, 1, 1)),
      'updatedAt': Timestamp.fromDate(DateTime.utc(2026, 1, 2)),
    };

void main() {
  group('AppListing.fromDoc', () {
    test('maps every display field', () {
      final AppListing listing = AppListing.fromDoc('app-1', _doc());

      expect(listing.id, 'app-1');
      expect(listing.ownerName, 'Asha Rao');
      expect(listing.status, ApplicationStatus.approved);
      expect(listing.isLive, isTrue);
      expect(listing.platforms, containsAll(<AppPlatform>[AppPlatform.android, AppPlatform.ios]));
      expect(listing.pricingModel, PricingModel.freemium);
      expect(listing.sponsored, isTrue);
      expect(listing.rating, 4.5);
      expect(listing.createdAt, DateTime.utc(2026, 1, 1));
    });

    test('tolerates a sparse document without throwing', () {
      final AppListing listing = AppListing.fromDoc('app-2', <String, dynamic>{});
      expect(listing.name, 'Untitled app');
      expect(listing.categoryId, 'other');
      expect(listing.status, ApplicationStatus.draft);
      expect(listing.platforms, isEmpty);
      expect(listing.rating, 0);
    });

    test('computes click-through rate from counters', () {
      final AppListing listing = AppListing.fromDoc('app-1', _doc());
      expect(listing.clickThroughRate, closeTo(0.1, 1e-9));
    });
  });

  group('AppListing.toCreateMap', () {
    test('seeds every server-owned counter at zero', () {
      final AppListing listing = AppListing.fromDoc('', _doc());
      final Map<String, dynamic> map = listing.toCreateMap(ownerName: 'Asha Rao');

      expect(map['rating'], 0);
      expect(map['reviewCount'], 0);
      expect(map['viewCount'], 0);
      expect(map['clickCount'], 0);
      expect(map['bookmarkCount'], 0);
      expect(map['sponsored'], isFalse);
      expect(map['featured'], isFalse);
      expect(map['status'], 'approved', reason: 'copied from the draft model');
    });

    test('builds a lowercase search index', () {
      final AppListing listing = AppListing.fromDoc('', _doc());
      final Map<String, dynamic> map = listing.toCreateMap(ownerName: 'Asha Rao');
      final Map<String, dynamic> search = map['search'] as Map<String, dynamic>;
      expect(search['title'], 'sprout notes');
      expect(search['keywords'], contains('notes'));
    });

    test('content updates never carry counters or moderation state', () {
      final AppListing listing = AppListing.fromDoc('app-1', _doc());
      final Map<String, dynamic> update = listing.toContentUpdate();
      expect(update.containsKey('viewCount'), isFalse);
      expect(update.containsKey('status'), isFalse);
      expect(update.containsKey('sponsored'), isFalse);
      expect(update['updatedAt'], isNotNull);
    });
  });
}
