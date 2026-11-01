import 'enums.dart';
import 'firestore_codec.dart';

/// A user-submitted moderation report.
class ContentReport {
  const ContentReport({
    required this.id,
    required this.reporterId,
    required this.target,
    required this.targetId,
    required this.reason,
    required this.createdAt,
    this.targetLabel = '',
    this.details,
    this.status = ReportStatus.open,
    this.resolutionNote,
  });

  final String id;
  final String reporterId;
  final String target;
  final String targetId;
  final String targetLabel;
  final String reason;
  final String? details;
  final ReportStatus status;
  final String? resolutionNote;
  final DateTime createdAt;

  factory ContentReport.fromDoc(String id, Map<String, dynamic> map) {
    return ContentReport(
      id: id,
      reporterId: Codec.str(map['reporterId']),
      target: Codec.str(map['target'], fallback: ReportTarget.application.name),
      targetId: Codec.str(map['targetId']),
      targetLabel: Codec.str(map['targetLabel']),
      reason: Codec.str(map['reason']),
      details: Codec.strOrNull(map['details']),
      status: ReportStatus.parse(map['status']),
      resolutionNote: Codec.strOrNull(map['resolutionNote']),
      createdAt: Codec.dateOrNow(map['createdAt']),
    );
  }
}
