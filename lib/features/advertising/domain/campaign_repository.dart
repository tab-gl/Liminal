import '../../../shared/models/campaign.dart';

abstract class CampaignRepository {
  Future<List<Campaign>> mine();

  Stream<Campaign?> watch(String campaignId);

  /// Creates a campaign in `pending_approval` with zero delivery. Returns the id.
  Future<String> create(Campaign campaign);

  Future<void> pause(String campaignId);

  Future<void> resume(String campaignId);

  /// Reports an impression or a click to the server. The function applies
  /// pacing, budget and fraud checks and returns the updated counters.
  Future<Map<String, dynamic>> track({
    required String campaignId,
    required String kind,
  });
}
