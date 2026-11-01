import 'package:cloud_firestore/cloud_firestore.dart';

/// Shared (de)serialisation helpers.
///
/// All timestamps are stored as Firestore Timestamps and handled as UTC
/// DateTime in Dart. Every decoder tolerates a missing or malformed field,
/// because a single bad document must not take down a whole feed.
class Codec {
  const Codec._();

  static DateTime? date(Object? raw) {
    if (raw is Timestamp) return raw.toDate().toUtc();
    if (raw is DateTime) return raw.toUtc();
    if (raw is int) return DateTime.fromMillisecondsSinceEpoch(raw, isUtc: true);
    if (raw is String) return DateTime.tryParse(raw)?.toUtc();
    return null;
  }

  static DateTime dateOrNow(Object? raw) => date(raw) ?? DateTime.now().toUtc();

  static Object? stamp(DateTime? value) =>
      value == null ? null : Timestamp.fromDate(value.toUtc());

  static String str(Object? raw, {String fallback = ''}) => raw is String ? raw : fallback;

  static String? strOrNull(Object? raw) {
    if (raw is String && raw.trim().isNotEmpty) return raw;
    return null;
  }

  static int integer(Object? raw, {int fallback = 0}) {
    if (raw is int) return raw;
    if (raw is num) return raw.round();
    if (raw is String) return int.tryParse(raw) ?? fallback;
    return fallback;
  }

  static double decimal(Object? raw, {double fallback = 0}) {
    if (raw is num) return raw.toDouble();
    if (raw is String) return double.tryParse(raw) ?? fallback;
    return fallback;
  }

  static bool boolean(Object? raw, {bool fallback = false}) =>
      raw is bool ? raw : fallback;

  static List<String> strings(Object? raw) {
    if (raw is List) {
      return raw.whereType<String>().map((String s) => s.trim()).where((String s) => s.isNotEmpty).toList();
    }
    return const <String>[];
  }

  static Map<String, dynamic> map(Object? raw) {
    if (raw is Map<String, dynamic>) return raw;
    if (raw is Map) return raw.map((Object? k, Object? v) => MapEntry<String, dynamic>(k.toString(), v));
    return const <String, dynamic>{};
  }
}
