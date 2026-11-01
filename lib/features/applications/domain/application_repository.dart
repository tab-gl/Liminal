import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../../../shared/models/report.dart';

abstract class ApplicationRepository {
  Stream<AppListing?> watch(String applicationId);

  Future<AppListing?> fetch(String applicationId);

  /// Appends a product event. View events are de-duplicated per device per day
  /// by the aggregation function, not by the client.
  Future<void> recordEvent({
    required String applicationId,
    required AppEventType type,
  });

  Future<void> reportContent({
    required String targetId,
    required String target,
    required String reason,
    String? details,
  });

  Future<List<ContentReport>> myReports();
}
