import '../../../shared/models/application.dart';
import '../../../shared/models/campaign.dart';
import '../../../shared/models/report.dart';
import '../../../shared/models/user_profile.dart';

/// Everything an administrator can do. Every method maps to a callable Cloud
/// Function that re-checks the `admin` custom claim - the UI gate is cosmetic.
abstract class AdminRepository {
  Future<List<AppListing>> pendingApplications();

  Future<void> moderateApplication({
    required String applicationId,
    required bool approve,
    String? reason,
  });

  Future<List<Campaign>> pendingCampaigns();

  Future<void> moderateCampaign({
    required String campaignId,
    required bool approve,
    String? reason,
  });

  Future<List<ContentReport>> openReports();

  Future<void> resolveReport({
    required String reportId,
    required bool uphold,
    String? note,
  });

  Future<List<UserProfile>> users({String? query});

  Future<void> setRole({required String uid, required String role});

  Future<Map<String, dynamic>> platformStats();
}
