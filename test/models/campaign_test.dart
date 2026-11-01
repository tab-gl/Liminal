import 'package:flutter_test/flutter_test.dart';
import 'package:sproutly/shared/models/campaign.dart';
import 'package:sproutly/shared/models/enums.dart';

Campaign _campaign({int budget = 100000, int spend = 25000, int impressions = 5000, int clicks = 120}) {
  return Campaign.fromDoc('c1', <String, dynamic>{
    'ownerId': 'dev-1',
    'applicationId': 'app-1',
    'applicationName': 'Sprout Notes',
    'name': 'Launch push',
    'objective': 'awareness',
    'budget': budget,
    'spend': spend,
    'impressions': impressions,
    'clicks': clicks,
    'currency': 'INR',
    'status': 'active',
    'placements': <String>['sponsoredCard'],
    'targeting': <String, dynamic>{'categoryIds': <String>['productivity']},
    'creative': <String, dynamic>{'headline': 'Notes that grow'},
    'startDate': DateTime.utc(2026, 1, 1),
    'endDate': DateTime.utc(2026, 2, 1),
    'createdAt': DateTime.utc(2026, 1, 1),
    'updatedAt': DateTime.utc(2026, 1, 1),
  });
}

void main() {
  test('derives remaining budget, spend ratio and CTR', () {
    final Campaign campaign = _campaign();
    expect(campaign.remainingPaise, 75000);
    expect(campaign.spendRatio, closeTo(0.25, 1e-9));
    expect(campaign.ctr, closeTo(0.024, 1e-9));
    expect(campaign.cpcPaise, closeTo(25000 / 120, 1e-6));
    expect(campaign.isLive, isTrue);
  });

  test('never reports negative remaining budget', () {
    final Campaign campaign = _campaign(budget: 10000, spend: 40000);
    expect(campaign.remainingPaise, 0);
    expect(campaign.spendRatio, 1.0);
  });

  test('new campaigns are submitted for approval with zero delivery', () {
    final Campaign campaign = _campaign();
    final Map<String, dynamic> map = campaign.toCreateMap();
    expect(map['status'], 'pending_approval');
    expect(map['impressions'], 0);
    expect(map['clicks'], 0);
    expect(map['spend'], 0);
  });

  test('unknown status falls back to draft rather than throwing', () {
    final Campaign campaign = Campaign.fromDoc('c2', <String, dynamic>{'status': 'something_new'});
    expect(campaign.status, CampaignStatus.draft);
  });

  test('a new campaign is created unfunded; the request is stored separately', () {
    final Campaign campaign = _campaign(budget: 250000, spend: 0, impressions: 0, clicks: 0);
    final Map<String, dynamic> map = campaign.toCreateMap();
    expect(map['budget'], 0, reason: 'only a verified payment may credit funds');
    expect(map['requestedBudget'], 250000);
    expect(map['spend'], 0);
    expect(map['status'], 'pending_approval');
  });

  test('reads the funded balance and the requested amount independently', () {
    final Campaign campaign = Campaign.fromDoc('c2', <String, dynamic>{
      'ownerId': 'dev-1',
      'applicationId': 'app-1',
      'budget': 0,
      'requestedBudget': 100000,
      'status': 'pending_approval',
    });
    expect(campaign.budgetPaise, 0);
    expect(campaign.requestedBudgetPaise, 100000);
    expect(campaign.isFunded, isFalse);
    expect(campaign.remainingPaise, 0);
  });
}
