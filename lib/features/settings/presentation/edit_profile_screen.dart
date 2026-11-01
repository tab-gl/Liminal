import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart' show ImageSource, XFile;
import 'package:material_ui/material_ui.dart';

import '../../../core/providers/app_providers.dart';
import '../../../core/services/media_service.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/app_image.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/widgets/sproutly_page.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/user_profile.dart';
import '../../authentication/presentation/auth_controller.dart';

class EditProfileScreen extends ConsumerStatefulWidget {
  const EditProfileScreen({super.key});

  @override
  ConsumerState<EditProfileScreen> createState() => _EditProfileScreenState();
}

class _EditProfileScreenState extends ConsumerState<EditProfileScreen> {
  final TextEditingController _name = TextEditingController();
  final TextEditingController _bio = TextEditingController();
  final TextEditingController _website = TextEditingController();
  final TextEditingController _github = TextEditingController();
  final TextEditingController _x = TextEditingController();
  bool _isPublic = true;
  bool _busy = false;
  bool _initialised = false;
  String? _photoUrl;

  void _hydrate(UserProfile profile) {
    if (_initialised) return;
    _initialised = true;
    _name.text = profile.displayName;
    _bio.text = profile.bio ?? '';
    _website.text = profile.websiteUrl ?? '';
    _github.text = profile.links['github'] ?? '';
    _x.text = profile.links['x'] ?? '';
    _isPublic = profile.isPublic;
    _photoUrl = profile.photoUrl;
  }

  @override
  void dispose() {
    for (final TextEditingController controller in <TextEditingController>[
      _name,
      _bio,
      _website,
      _github,
      _x,
    ]) {
      controller.dispose();
    }
    super.dispose();
  }

  Future<void> _pickAvatar() async {
    final String? uid = ref.read(currentUserProvider)?.uid;
    if (uid == null) return;
    final MediaService media = ref.read(mediaProvider);
    final XFile? picked = await media.pickImage(
      source: ImageSource.gallery,
      maxWidth: 800,
      maxHeight: 800,
    );
    if (picked == null) return;
    setState(() => _busy = true);
    try {
      final String url = await media.upload(
        file: picked,
        bucket: MediaBucket.avatar,
        ownerId: uid,
      );
      setState(() => _photoUrl = url);
    } catch (error) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(error.toString())));
      }
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _save() async {
    if (Validators.required(_name.text, label: 'Name') != null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('A display name is required.')),
      );
      return;
    }
    setState(() => _busy = true);
    final bool ok = await ref.read(authControllerProvider.notifier).updateProfile(
          displayName: _name.text,
          bio: _bio.text,
          websiteUrl: _website.text,
          photoUrl: _photoUrl,
          isPublic: _isPublic,
          links: <String, String>{
            if (_github.text.trim().isNotEmpty) 'github': _github.text.trim(),
            if (_x.text.trim().isNotEmpty) 'x': _x.text.trim(),
          },
        );
    if (!mounted) return;
    setState(() => _busy = false);
    if (ok) {
      ScaffoldMessenger.of(context).showSnackBar(const SnackBar(content: Text('Profile updated')));
      context.pop();
    }
  }

  @override
  Widget build(BuildContext context) {
    final AsyncValue<UserProfile?> async = ref.watch(userProfileProvider);

    return SproutlyPage(
      title: 'Edit profile',
      showBack: true,
      scrollable: false,
      padding: EdgeInsets.zero,
      child: async.when(
        loading: () => const Center(child: SproutlyLoader()),
        error: (Object error, StackTrace _) => ErrorStateView(error: error),
        data: (UserProfile? profile) {
          if (profile == null) {
            return const EmptyView(
              title: 'Profile unavailable',
              message: 'Try again in a moment.',
              icon: Icons.person_off_outlined,
            );
          }
          _hydrate(profile);
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
            children: <Widget>[
              Center(
                child: Column(
                  children: <Widget>[
                    GestureDetector(
                      onTap: _busy ? null : _pickAvatar,
                      child: ClipOval(
                        child: AppImage(
                          url: _photoUrl,
                          height: 92,
                          width: 92,
                          radius: BorderRadius.circular(92),
                          icon: Icons.person_rounded,
                          isIcon: true,
                        ),
                      ),
                    ),
                    TextButton(
                      onPressed: _busy ? null : _pickAvatar,
                      child: const Text('Change photo'),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 8),
              SproutlyField(
                label: 'Display name',
                controller: _name,
                maxLength: 60,
                textCapitalization: TextCapitalization.words,
              ),
              const SizedBox(height: 14),
              SproutlyField(
                label: 'Bio',
                controller: _bio,
                hint: 'What you build, in a sentence or two.',
                maxLines: 4,
                maxLength: 280,
                showCounter: true,
              ),
              const SizedBox(height: 14),
              SproutlyField(
                label: 'Website',
                controller: _website,
                hint: 'https://yourdomain.com',
                keyboardType: TextInputType.url,
                textCapitalization: TextCapitalization.none,
                validator: (String? v) => Validators.httpsUrl(v),
              ),
              const SizedBox(height: 14),
              SproutlyField(
                label: 'GitHub username',
                controller: _github,
                hint: 'octocat',
                textCapitalization: TextCapitalization.none,
                validator: Validators.handle,
              ),
              const SizedBox(height: 14),
              SproutlyField(
                label: 'X handle',
                controller: _x,
                hint: 'yourhandle',
                textCapitalization: TextCapitalization.none,
                validator: Validators.handle,
              ),
              const SizedBox(height: 8),
              SwitchListTile(
                contentPadding: EdgeInsets.zero,
                title: Text('Public profile', style: context.text.bodyMedium),
                subtitle: Text(
                  'Off hides your profile from discovery. Your listings stay visible.',
                  style: context.text.labelSmall,
                ),
                value: _isPublic,
                onChanged: (bool value) => setState(() => _isPublic = value),
              ),
              const SizedBox(height: 16),
              SproutlyButton(
                label: 'Save changes',
                expand: true,
                isLoading: _busy,
                onPressed: _save,
              ),
            ],
          );
        },
      ),
    );
  }
}
