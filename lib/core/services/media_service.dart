import 'dart:io';

import 'package:firebase_storage/firebase_storage.dart';
import 'package:flutter/foundation.dart';
import 'package:image_picker/image_picker.dart';
import 'package:uuid/uuid.dart';

import '../errors/app_exception.dart';
import '../errors/error_mapper.dart';

/// Where an uploaded asset lives, which decides its Storage path and its
/// ownership check in storage.rules.
enum MediaBucket {
  appIcon,
  screenshot,
  banner,
  campaignCreative,
  avatar,
}

/// Picks images with the native picker and uploads them to Cloud Storage.
///
/// Sizes are enforced twice: here (fast, friendly message) and in
/// storage.rules (authoritative, cannot be bypassed).
class MediaService {
  MediaService({FirebaseStorage? storage, ImagePicker? picker})
      : _storage = storage ?? FirebaseStorage.instance,
        _picker = picker ?? ImagePicker();

  final FirebaseStorage _storage;
  final ImagePicker _picker;
  static const Uuid _uuid = Uuid();

  static const Map<MediaBucket, int> _maxBytes = <MediaBucket, int>{
    MediaBucket.appIcon: 2 * 1024 * 1024,
    MediaBucket.screenshot: 8 * 1024 * 1024,
    MediaBucket.banner: 8 * 1024 * 1024,
    MediaBucket.campaignCreative: 4 * 1024 * 1024,
    MediaBucket.avatar: 2 * 1024 * 1024,
  };

  static String _folder(MediaBucket bucket) {
    switch (bucket) {
      case MediaBucket.appIcon:
        return 'app_icons';
      case MediaBucket.screenshot:
        return 'screenshots';
      case MediaBucket.banner:
        return 'banners';
      case MediaBucket.campaignCreative:
        return 'campaign_creatives';
      case MediaBucket.avatar:
        return 'avatars';
    }
  }

  Future<XFile?> pickImage({
    required ImageSource source,
    double? maxWidth,
    double? maxHeight,
    // image_picker 1.2 changed imageQuality from 0-1 double to 0-100 int.
    int imageQuality = 90,
  }) async {
    try {
      return await _picker.pickImage(
        source: source,
        maxWidth: maxWidth,
        maxHeight: maxHeight,
        imageQuality: imageQuality,
        requestFullMetadata: false,
      );
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  Future<List<XFile>> pickMultiple({int limit = 8}) async {
    try {
      return await _picker.pickMultiImage(
        imageQuality: 90,
        limit: limit,
        requestFullMetadata: false,
      );
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  /// Uploads [file] and returns its public download URL.
  Future<String> upload({
    required XFile file,
    required MediaBucket bucket,
    required String ownerId,
    void Function(double progress)? onProgress,
  }) async {
    final int size = await file.length();
    final int max = _maxBytes[bucket]!;
    if (size > max) {
      throw AppException(
        AppErrorCode.invalidInput,
        'That image is ${(size / 1024 / 1024).toStringAsFixed(1)} MB. The limit is ${max ~/ (1024 * 1024)} MB.',
      );
    }
    final String ext = _extension(file.name);
    final String path = '${_folder(bucket)}/$ownerId/${_uuid.v4()}.$ext';
    final Reference ref = _storage.ref(path);
    try {
      final UploadTask task = ref.putFile(
        File(file.path),
        SettableMetadata(
          contentType: 'image/$ext',
          cacheControl: 'public, max-age=31536000',
        ),
      );
      if (onProgress != null) {
        task.snapshotEvents.listen((TaskSnapshot snapshot) {
          if (snapshot.totalBytes > 0) {
            onProgress(snapshot.bytesTransferred / snapshot.totalBytes);
          }
        });
      }
      await task;
      return await ref.getDownloadURL();
    } catch (error, stack) {
      if (kDebugMode) debugPrint('Sproutly upload failed: $error');
      throw mapError(error, stack);
    }
  }

  Future<void> delete(String downloadUrl) async {
    try {
      await _storage.refFromURL(downloadUrl).delete();
    } catch (_) {
      // Already gone, or not ours. Not worth interrupting the user.
    }
  }

  static String _extension(String name) {
    final int dot = name.lastIndexOf('.');
    final String ext = dot == -1 ? 'jpg' : name.substring(dot + 1).toLowerCase();
    return <String>['png', 'jpeg', 'jpg', 'webp'].contains(ext) ? ext : 'jpg';
  }
}
