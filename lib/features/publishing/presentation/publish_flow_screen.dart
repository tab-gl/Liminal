import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:image_picker/image_picker.dart' show ImageSource, XFile;
import 'package:material_ui/material_ui.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/constants/categories.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/services/haptics_service.dart';
import '../../../core/services/media_service.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/app_image.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/widgets/sproutly_sheet.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/enums.dart';
import '../data/publishing_repository_impl.dart';
import '../domain/publishing_repository.dart';
import 'publish_controller.dart';

/// The five-step publish flow.
///
/// Each step validates before it will let you move on, the draft can be saved at
/// any point, and the final step is a real preview of the listing rather than a
/// summary of form fields.
class PublishFlowScreen extends ConsumerStatefulWidget {
  const PublishFlowScreen({super.key, this.applicationId});

  final String? applicationId;

  @override
  ConsumerState<PublishFlowScreen> createState() => _PublishFlowScreenState();
}

class _PublishFlowScreenState extends ConsumerState<PublishFlowScreen> {
  final TextEditingController _name = TextEditingController();
  final TextEditingController _tagline = TextEditingController();
  final TextEditingController _description = TextEditingController();
  final TextEditingController _tagInput = TextEditingController();
  final TextEditingController _website = TextEditingController();
  final TextEditingController _launch = TextEditingController();
  final TextEditingController _download = TextEditingController();
  final TextEditingController _price = TextEditingController();
  final TextEditingController _featureInput = TextEditingController();
  final TextEditingController _docs = TextEditingController();
  final TextEditingController _support = TextEditingController();
  final TextEditingController _privacy = TextEditingController();
  final TextEditingController _terms = TextEditingController();

  bool _bootstrapping = true;
  bool _busy = false;
  double _uploadProgress = 0;

  @override
  void initState() {
    super.initState();
    WidgetsBinding.instance.addPostFrameCallback((_) => _bootstrap());
  }

  Future<void> _bootstrap() async {
    if (widget.applicationId != null) {
      final AppListing? listing =
          await ref.read(publishingRepositoryProvider).fetch(widget.applicationId!);
      if (listing != null && mounted) {
        ref.read(publishDraftProvider.notifier).load(listing);
      }
    } else {
      ref.read(publishDraftProvider.notifier).reset();
    }
    _syncControllers(ref.read(publishDraftProvider));
    if (mounted) setState(() => _bootstrapping = false);
    await ref.read(analyticsProvider).publishStarted();
  }

  void _syncControllers(PublishDraft draft) {
    _name.text = draft.name;
    _tagline.text = draft.tagline;
    _description.text = draft.description;
    _website.text = draft.websiteUrl ?? '';
    _launch.text = draft.launchUrl ?? '';
    _download.text = draft.downloadUrl ?? '';
    _price.text = draft.priceLabel ?? '';
    _docs.text = draft.documentationUrl ?? '';
    _support.text = draft.supportEmail ?? '';
    _privacy.text = draft.privacyPolicyUrl ?? '';
    _terms.text = draft.termsUrl ?? '';
  }

  @override
  void dispose() {
    for (final TextEditingController controller in <TextEditingController>[
      _name,
      _tagline,
      _description,
      _tagInput,
      _website,
      _launch,
      _download,
      _price,
      _featureInput,
      _docs,
      _support,
      _privacy,
      _terms,
    ]) {
      controller.dispose();
    }
    super.dispose();
  }

  PublishDraft get _draft => ref.read(publishDraftProvider);
  PublishController get _controller => ref.read(publishDraftProvider.notifier);

  Future<void> _pickIcon() async {
    final MediaService media = ref.read(mediaProvider);
    final String? uid = ref.read(currentUserProvider)?.uid;
    if (uid == null) return;
    final XFile? picked =
        await media.pickImage(source: ImageSource.gallery, maxWidth: 1024, maxHeight: 1024);
    if (picked == null) return;
    setState(() {
      _busy = true;
      _uploadProgress = 0;
    });
    try {
      final String url = await media.upload(
        file: picked,
        bucket: MediaBucket.appIcon,
        ownerId: _draft.id.isEmpty ? uid : _draft.id,
        onProgress: (double p) => setState(() => _uploadProgress = p),
      );
      _controller.patch((PublishDraft d) => d.copyWith(iconUrl: url));
    } catch (error) {
      _toast(error.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _pickScreenshots() async {
    final MediaService media = ref.read(mediaProvider);
    final String? uid = ref.read(currentUserProvider)?.uid;
    if (uid == null) return;
    final List<XFile> files =
        await media.pickMultiple(limit: AppConstants.maxScreenshots - _draft.screenshots.length);
    if (files.isEmpty) return;
    setState(() => _busy = true);
    try {
      final List<String> uploaded = <String>[];
      for (final XFile file in files) {
        uploaded.add(await media.upload(
          file: file,
          bucket: MediaBucket.screenshot,
          ownerId: _draft.id.isEmpty ? uid : _draft.id,
        ));
      }
      _controller.patch((PublishDraft d) => d.copyWith(
            screenshots: <String>[...d.screenshots, ...uploaded]
                .take(AppConstants.maxScreenshots)
                .toList(),
          ));
    } catch (error) {
      _toast(error.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _saveDraft() async {
    setState(() => _busy = true);
    try {
      final String id = await ref.read(publishingRepositoryProvider).save(_draft.toListing());
      _controller.setSaved(id);
      _toast('Draft saved');
    } catch (error) {
      _toast(error.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _submit() async {
    final List<String> problems = _draft.validateStep(4);
    if (problems.isNotEmpty) {
      await _showProblems(problems);
      return;
    }
    setState(() => _busy = true);
    try {
      final PublishingRepository repo = ref.read(publishingRepositoryProvider);
      String id = _draft.id;
      if (id.isEmpty) {
        id = await repo.save(_draft.toListing());
        _controller.setSaved(id);
      } else {
        await repo.save(_draft.toListing());
      }
      await repo.submit(id);
      await ref.read(analyticsProvider).publishSubmitted(id);
      ref.invalidate(myAppsProvider);
      if (!mounted) return;
      await showDialog<void>(
        context: context,
        builder: (BuildContext context) => AlertDialog(
          shape: RoundedRectangleBorder(borderRadius: SproutlyShapes.radiusXl),
          title: const Text('Submitted for review'),
          content: const Text(
            'A moderator checks every listing, usually within 24 hours. You will '
            'get a notification either way, and you can keep editing the draft '
            'until it goes live.',
          ),
          actions: <Widget>[
            SproutlyButton(
              label: 'Go to dashboard',
              size: SproutlyButtonSize.small,
              onPressed: () => Navigator.of(context).pop(),
            ),
          ],
        ),
      );
      if (!mounted) return;
      _controller.reset();
      context.go('/dashboard');
    } catch (error) {
      _toast(error.toString());
    } finally {
      if (mounted) setState(() => _busy = false);
    }
  }

  Future<void> _showProblems(List<String> problems) {
    return showModalBottomSheet<void>(
      context: context,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) => SproutlySheet(
        title: 'Almost there',
        subtitle: 'Fix these before submitting:',
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            for (final String problem in problems)
              Padding(
                padding: const EdgeInsets.only(bottom: 8),
                child: Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Icon(Icons.error_outline_rounded, size: 15, color: context.colors.error),
                    const SizedBox(width: 8),
                    Expanded(child: Text(problem, style: context.text.bodyMedium)),
                  ],
                ),
              ),
          ],
        ),
      ),
    );
  }

  void _toast(String message) {
    if (!mounted) return;
    ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
  }

  Future<bool> _continue() async {
    final List<String> problems = _draft.validateStep(_draft.step);
    if (problems.isNotEmpty) {
      await _showProblems(problems);
      return false;
    }
    Haptics.tap();
    _controller.goToStep(_draft.step + 1);
    return true;
  }

  @override
  Widget build(BuildContext context) {
    final PublishDraft draft = ref.watch(publishDraftProvider);

    if (_bootstrapping) {
      return Scaffold(
        backgroundColor: context.colors.surface,
        body: const Center(child: SproutlyLoader()),
      );
    }

    return Scaffold(
      backgroundColor: context.colors.surface,
      body: SafeArea(
        child: Column(
          children: <Widget>[
            _ProgressBar(
              step: draft.step,
              onBack: () {
                if (draft.step == 0) {
                  context.pop();
                } else {
                  _controller.goToStep(draft.step - 1);
                }
              },
              onSave: _busy ? null : _saveDraft,
            ),
            Expanded(
              child: ListView(
                padding: const EdgeInsets.fromLTRB(20, 8, 20, 24),
                children: <Widget>[
                  Text(
                    PublishDraft.stepTitles[draft.step],
                    style: context.text.headlineSmall,
                  ),
                  const SizedBox(height: 4),
                  Text(
                    _stepBlurb(draft.step),
                    style: context.text.bodySmall,
                  ),
                  const SizedBox(height: 20),
                  ..._stepContent(draft),
                ],
              ),
            ),
            Padding(
              padding: EdgeInsets.fromLTRB(20, 8, 20, 12 + MediaQuery.paddingOf(context).bottom),
              child: Row(
                children: <Widget>[
                  if (draft.step > 0)
                    Expanded(
                      child: SproutlyButton(
                        label: 'Back',
                        variant: SproutlyButtonVariant.outline,
                        onPressed: () => _controller.goToStep(draft.step - 1),
                      ),
                    ),
                  if (draft.step > 0) const SizedBox(width: 10),
                  Expanded(
                    flex: 2,
                    child: SproutlyButton(
                      label: draft.step == 4 ? 'Submit for review' : 'Continue',
                      isLoading: _busy,
                      trailingIcon: draft.step == 4 ? Icons.send_rounded : Icons.arrow_forward_rounded,
                      onPressed: () async {
                        if (draft.step == 4) {
                          await _submit();
                        } else {
                          await _continue();
                        }
                      },
                    ),
                  ),
                ],
              ),
            ),
          ],
        ),
      ),
    );
  }

  String _stepBlurb(int step) => switch (step) {
        0 => 'Only the name, a category and an icon are required. Everything else helps you get taps.',
        1 => 'Where people go when they tap your app, and what it costs.',
        2 => 'Optional, but listings with screenshots get far more taps.',
        3 => 'Trust signals: docs, support and the legal links.',
        _ => 'This is exactly how your listing will look.',
      };

  List<Widget> _stepContent(PublishDraft draft) {
    switch (draft.step) {
      case 0:
        return <Widget>[
          _IconPicker(
            url: draft.iconUrl,
            busy: _busy,
            progress: _uploadProgress,
            onPick: _pickIcon,
            onClear: () => _controller.patch((PublishDraft d) => d.copyWith(iconUrl: '')),
          ),
          const SizedBox(height: 18),
          SproutlyField(
            label: 'App name',
            controller: _name,
            hint: 'Sproutly Notes',
            maxLength: AppConstants.maxNameLength,
            textCapitalization: TextCapitalization.words,
            onChanged: (String v) => _controller.patch((PublishDraft d) => d.copyWith(name: v)),
          ),
          const SizedBox(height: 14),
          SproutlyField(
            label: 'Tagline (optional)',
            controller: _tagline,
            hint: 'Notes that grow with you',
            maxLength: AppConstants.maxTaglineLength,
            onChanged: (String v) => _controller.patch((PublishDraft d) => d.copyWith(tagline: v)),
          ),
          const SizedBox(height: 14),
          SproutlyField(
            label: 'Description (optional)',
            controller: _description,
            hint: 'What it does, who it is for, and why it exists.',
            maxLines: 6,
            maxLength: AppConstants.maxDescriptionLength,
            showCounter: true,
            onChanged: (String v) => _controller.patch((PublishDraft d) => d.copyWith(description: v)),
          ),
          const SizedBox(height: 16),
          Text('Category', style: context.text.labelMedium),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: <Widget>[
              for (final AppCategory category in AppCategory.all)
                ChoiceChip(
                  label: Text(category.label),
                  avatar: Icon(category.icon, size: 15),
                  selected: draft.categoryId == category.id,
                  onSelected: (_) {
                    Haptics.tap();
                    _controller.patch((PublishDraft d) => d.copyWith(categoryId: category.id));
                  },
                ),
            ],
          ),
          const SizedBox(height: 16),
          _TagEditor(
            tags: draft.tags,
            controller: _tagInput,
            onAdd: (String tag) {
              final String clean = tag.trim().toLowerCase();
              if (clean.isEmpty || draft.tags.contains(clean)) return;
              if (draft.tags.length >= AppConstants.maxTags) return;
              _controller.patch((PublishDraft d) => d.copyWith(tags: <String>[...d.tags, clean]));
              _tagInput.clear();
            },
            onRemove: (String tag) => _controller.patch(
              (PublishDraft d) => d.copyWith(tags: d.tags.where((String t) => t != tag).toList()),
            ),
          ),
        ];
      case 1:
        return <Widget>[
          SproutlyField(
            label: 'Website',
            controller: _website,
            hint: 'https://yourdomain.com',
            keyboardType: TextInputType.url,
            textCapitalization: TextCapitalization.none,
            validator: (String? v) => Validators.httpsUrl(v),
            onChanged: (String v) => _controller.patch((PublishDraft d) => d.copyWith(websiteUrl: v)),
          ),
          const SizedBox(height: 14),
          SproutlyField(
            label: 'Launch / live app URL',
            controller: _launch,
            hint: 'https://app.yourdomain.com',
            keyboardType: TextInputType.url,
            textCapitalization: TextCapitalization.none,
            onChanged: (String v) => _controller.patch((PublishDraft d) => d.copyWith(launchUrl: v)),
          ),
          const SizedBox(height: 14),
          SproutlyField(
            label: 'Download URL',
            controller: _download,
            hint: 'Play Store or App Store link',
            keyboardType: TextInputType.url,
            textCapitalization: TextCapitalization.none,
            onChanged: (String v) => _controller.patch((PublishDraft d) => d.copyWith(downloadUrl: v)),
          ),
          const SizedBox(height: 18),
          Text('Supported platforms', style: context.text.labelMedium),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: <Widget>[
              for (final AppPlatform platform in AppPlatform.values)
                FilterChip(
                  label: Text(platform.label),
                  selected: draft.platforms.contains(platform),
                  onSelected: (bool on) {
                    Haptics.tap();
                    final List<AppPlatform> next = <AppPlatform>[...draft.platforms];
                    if (on) {
                      next.add(platform);
                    } else {
                      next.remove(platform);
                    }
                    _controller.patch((PublishDraft d) => d.copyWith(platforms: next));
                  },
                ),
            ],
          ),
          const SizedBox(height: 18),
          Text('Pricing', style: context.text.labelMedium),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: <Widget>[
              for (final PricingModel model in PricingModel.values)
                ChoiceChip(
                  label: Text(model.label),
                  selected: draft.pricingModel == model,
                  onSelected: (_) =>
                      _controller.patch((PublishDraft d) => d.copyWith(pricingModel: model)),
                ),
            ],
          ),
          if (draft.pricingModel != PricingModel.free) ...<Widget>[
            const SizedBox(height: 14),
            SproutlyField(
              label: 'Price label',
              controller: _price,
              hint: 'Rs 299 / month',
              onChanged: (String v) => _controller.patch((PublishDraft d) => d.copyWith(priceLabel: v)),
            ),
          ],
          const SizedBox(height: 8),
          SwitchListTile(
            contentPadding: EdgeInsets.zero,
            title: Text('Open source', style: context.text.bodyMedium),
            subtitle: Text('Show a badge and link your repository', style: context.text.labelSmall),
            value: draft.isOpenSource,
            onChanged: (bool v) => _controller.patch((PublishDraft d) => d.copyWith(isOpenSource: v)),
          ),
        ];
      case 2:
        return <Widget>[
          _ScreenshotGrid(
            urls: draft.screenshots,
            busy: _busy,
            onAdd: _pickScreenshots,
            onRemove: (String url) => _controller.patch(
              (PublishDraft d) => d.copyWith(
                screenshots: d.screenshots.where((String s) => s != url).toList(),
              ),
            ),
          ),
          const SizedBox(height: 18),
          Text(
            'Portrait screenshots work best. Up to ${AppConstants.maxScreenshots}, '
            '8 MB each. You can submit without any and add them later.',
            style: context.text.labelSmall!.copyWith(height: 1.5),
          ),
        ];
      case 3:
        return <Widget>[
          _FeatureEditor(
            features: draft.features,
            controller: _featureInput,
            onAdd: (String value) {
              if (value.trim().isEmpty) return;
              if (draft.features.length >= AppConstants.maxFeatures) return;
              _controller.patch(
                (PublishDraft d) => d.copyWith(features: <String>[...d.features, value.trim()]),
              );
              _featureInput.clear();
            },
            onRemove: (String value) => _controller.patch(
              (PublishDraft d) => d.copyWith(
                features: d.features.where((String f) => f != value).toList(),
              ),
            ),
          ),
          const SizedBox(height: 18),
          SproutlyField(
            label: 'Documentation URL (optional)',
            controller: _docs,
            hint: 'https://docs.yourdomain.com',
            keyboardType: TextInputType.url,
            textCapitalization: TextCapitalization.none,
            onChanged: (String v) =>
                _controller.patch((PublishDraft d) => d.copyWith(documentationUrl: v)),
          ),
          const SizedBox(height: 14),
          SproutlyField(
            label: 'Support email (optional)',
            controller: _support,
            hint: 'help@yourdomain.com',
            keyboardType: TextInputType.emailAddress,
            textCapitalization: TextCapitalization.none,
            onChanged: (String v) => _controller.patch((PublishDraft d) => d.copyWith(supportEmail: v)),
          ),
          const SizedBox(height: 14),
          SproutlyField(
            label: 'Privacy policy URL (optional)',
            controller: _privacy,
            hint: 'https://yourdomain.com/privacy',
            keyboardType: TextInputType.url,
            textCapitalization: TextCapitalization.none,
            helper: 'Recommended if the app collects personal data.',
            onChanged: (String v) =>
                _controller.patch((PublishDraft d) => d.copyWith(privacyPolicyUrl: v)),
          ),
          const SizedBox(height: 14),
          SproutlyField(
            label: 'Terms URL (optional)',
            controller: _terms,
            hint: 'https://yourdomain.com/terms',
            keyboardType: TextInputType.url,
            textCapitalization: TextCapitalization.none,
            onChanged: (String v) => _controller.patch((PublishDraft d) => d.copyWith(termsUrl: v)),
          ),
        ];
      default:
        return <Widget>[
          _PreviewCard(draft: draft),
          const SizedBox(height: 16),
          Container(
            padding: const EdgeInsets.all(14),
            decoration: BoxDecoration(
              color: context.colors.primaryContainer.withValues(alpha: 0.5),
              borderRadius: SproutlyShapes.radiusLg,
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: <Widget>[
                Text(
                  'What happens next',
                  style: context.text.titleSmall!.copyWith(color: context.colors.onPrimaryContainer),
                ),
                const SizedBox(height: 6),
                Text(
                  'A moderator reviews the listing for broken links, copied content and '
                  'misleading claims. If anything needs changing you will get a '
                  'notification with the reason, and the draft stays editable.',
                  style: context.text.bodySmall!.copyWith(
                    color: context.colors.onPrimaryContainer,
                    height: 1.5,
                  ),
                ),
              ],
            ),
          ),
        ];
    }
  }
}

class _ProgressBar extends StatelessWidget {
  const _ProgressBar({required this.step, required this.onBack, required this.onSave});

  final int step;
  final VoidCallback onBack;
  final VoidCallback? onSave;

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: const EdgeInsets.fromLTRB(8, 6, 12, 6),
      child: Row(
        children: <Widget>[
          SproutlyIconButton(icon: Icons.close_rounded, tooltip: 'Close', onPressed: onBack),
          const SizedBox(width: 8),
          Expanded(
            child: Row(
              children: <Widget>[
                for (int i = 0; i < 5; i++)
                  Expanded(
                    child: AnimatedContainer(
                      duration: const Duration(milliseconds: 220),
                      height: 5,
                      margin: const EdgeInsets.only(right: 5),
                      decoration: BoxDecoration(
                        color: i <= step ? context.colors.primary : context.colors.outlineVariant,
                        borderRadius: SproutlyShapes.radiusPill,
                      ),
                    ),
                  ),
              ],
            ),
          ),
          const SizedBox(width: 8),
          TextButton(onPressed: onSave, child: const Text('Save')),
        ],
      ),
    );
  }
}

class _IconPicker extends StatelessWidget {
  const _IconPicker({
    required this.url,
    required this.busy,
    required this.progress,
    required this.onPick,
    required this.onClear,
  });

  final String? url;
  final bool busy;
  final double progress;
  final VoidCallback onPick;
  final VoidCallback onClear;

  @override
  Widget build(BuildContext context) {
    final bool hasIcon = url != null && url!.isNotEmpty;
    return Center(
      child: Column(
        children: <Widget>[
          GestureDetector(
            onTap: busy ? null : onPick,
            child: Container(
              height: 96,
              width: 96,
              decoration: BoxDecoration(
                color: context.colors.primaryContainer,
                borderRadius: BorderRadius.circular(30),
                border: Border.all(color: context.colors.outlineVariant),
              ),
              clipBehavior: Clip.antiAlias,
              child: busy
                  ? Center(
                      child: SizedBox(
                        height: 26,
                        width: 26,
                        child: CircularProgressIndicator(
                          value: progress == 0 ? null : progress,
                          strokeWidth: 2.4,
                          color: context.colors.onPrimaryContainer,
                        ),
                      ),
                    )
                  : hasIcon
                      ? AppImage(url: url, fit: BoxFit.cover, radius: BorderRadius.circular(30))
                      : Icon(
                          Icons.add_photo_alternate_outlined,
                          size: 30,
                          color: context.colors.onPrimaryContainer,
                        ),
            ),
          ),
          const SizedBox(height: 8),
          Row(
            mainAxisAlignment: MainAxisAlignment.center,
            children: <Widget>[
              TextButton(onPressed: busy ? null : onPick, child: Text(hasIcon ? 'Replace icon' : 'Add icon')),
              if (hasIcon)
                TextButton(onPressed: busy ? null : onClear, child: const Text('Remove')),
            ],
          ),
        ],
      ),
    );
  }
}

class _TagEditor extends StatelessWidget {
  const _TagEditor({
    required this.tags,
    required this.controller,
    required this.onAdd,
    required this.onRemove,
  });

  final List<String> tags;
  final TextEditingController controller;
  final ValueChanged<String> onAdd;
  final ValueChanged<String> onRemove;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        SproutlyField(
          label: 'Tags',
          controller: controller,
          hint: 'Type a tag and press enter',
          textInputAction: TextInputAction.done,
          textCapitalization: TextCapitalization.none,
          onSubmitted: onAdd,
          helper: '${tags.length}/${AppConstants.maxTags} used. Tags help search.',
        ),
        if (tags.isNotEmpty) ...<Widget>[
          const SizedBox(height: 8),
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: <Widget>[
              for (final String tag in tags)
                InputChip(
                  label: Text(tag),
                  onDeleted: () => onRemove(tag),
                  deleteIcon: const Icon(Icons.close_rounded, size: 14),
                ),
            ],
          ),
        ],
      ],
    );
  }
}

class _FeatureEditor extends StatelessWidget {
  const _FeatureEditor({
    required this.features,
    required this.controller,
    required this.onAdd,
    required this.onRemove,
  });

  final List<String> features;
  final TextEditingController controller;
  final ValueChanged<String> onAdd;
  final ValueChanged<String> onRemove;

  @override
  Widget build(BuildContext context) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        SproutlyField(
          label: 'Key features',
          controller: controller,
          hint: 'Offline-first sync',
          textInputAction: TextInputAction.done,
          onSubmitted: onAdd,
          helper: '${features.length}/${AppConstants.maxFeatures} added',
        ),
        const SizedBox(height: 8),
        for (final String feature in features)
          Padding(
            padding: const EdgeInsets.only(bottom: 6),
            child: Row(
              children: <Widget>[
                Icon(Icons.check_circle_rounded, size: 15, color: context.tokens.success),
                const SizedBox(width: 8),
                Expanded(child: Text(feature, style: context.text.bodyMedium)),
                IconButton(
                  icon: const Icon(Icons.close_rounded, size: 16),
                  onPressed: () => onRemove(feature),
                ),
              ],
            ),
          ),
      ],
    );
  }
}

class _ScreenshotGrid extends StatelessWidget {
  const _ScreenshotGrid({
    required this.urls,
    required this.busy,
    required this.onAdd,
    required this.onRemove,
  });

  final List<String> urls;
  final bool busy;
  final VoidCallback onAdd;
  final ValueChanged<String> onRemove;

  @override
  Widget build(BuildContext context) {
    return GridView.builder(
      shrinkWrap: true,
      physics: const NeverScrollableScrollPhysics(),
      gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
        crossAxisCount: 3,
        crossAxisSpacing: 10,
        mainAxisSpacing: 10,
        childAspectRatio: 0.62,
      ),
      itemCount: urls.length + 1,
      itemBuilder: (BuildContext context, int index) {
        if (index == urls.length) {
          return InkWell(
            onTap: busy ? null : onAdd,
            borderRadius: SproutlyShapes.radiusMd,
            child: Container(
              decoration: BoxDecoration(
                color: context.colors.surfaceContainerHigh,
                borderRadius: SproutlyShapes.radiusMd,
                border: Border.all(color: context.colors.outlineVariant),
              ),
              child: busy
                  ? const Center(child: SproutlyLoader(size: 20))
                  : Icon(Icons.add_rounded, color: context.colors.onSurfaceVariant),
            ),
          );
        }
        return Stack(
          children: <Widget>[
            Positioned.fill(
              child: AppImage(
                url: urls[index],
                radius: SproutlyShapes.radiusMd,
                icon: Icons.image_outlined,
              ),
            ),
            Positioned(
              top: 4,
              right: 4,
              child: GestureDetector(
                onTap: () => onRemove(urls[index]),
                child: Container(
                  padding: const EdgeInsets.all(3),
                  decoration: BoxDecoration(
                    color: Colors.black.withValues(alpha: 0.55),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.close_rounded, size: 13, color: Colors.white),
                ),
              ),
            ),
          ],
        );
      },
    );
  }
}

class _PreviewCard extends StatelessWidget {
  const _PreviewCard({required this.draft});

  final PublishDraft draft;

  @override
  Widget build(BuildContext context) {
    final AppCategory category = AppCategory.byId(draft.categoryId);
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: context.colors.surfaceContainerLowest,
        borderRadius: SproutlyShapes.cardRadius(),
        border: Border.all(color: context.colors.outlineVariant),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: <Widget>[
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              AppImage(
                url: draft.iconUrl,
                height: 56,
                width: 56,
                radius: BorderRadius.circular(20),
                isIcon: true,
              ),
              const SizedBox(width: 12),
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: <Widget>[
                    Text(
                      draft.name.isEmpty ? 'Your app name' : draft.name,
                      style: context.text.titleMedium,
                    ),
                    const SizedBox(height: 2),
                    Text(
                      draft.tagline.isEmpty ? 'Your one-line pitch' : draft.tagline,
                      style: context.text.bodySmall,
                    ),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),
          Wrap(
            spacing: 6,
            runSpacing: 6,
            children: <Widget>[
              if (draft.categoryId.isNotEmpty)
                Chip(
                  label: Text(category.label),
                  avatar: Icon(category.icon, size: 14),
                  visualDensity: VisualDensity.compact,
                ),
              for (final AppPlatform platform in draft.platforms)
                Chip(label: Text(platform.label), visualDensity: VisualDensity.compact),
              Chip(label: Text(draft.pricingModel.label), visualDensity: VisualDensity.compact),
            ],
          ),
          if (draft.screenshots.isNotEmpty) ...<Widget>[
            const SizedBox(height: 12),
            SizedBox(
              height: 150,
              child: ListView.separated(
                scrollDirection: Axis.horizontal,
                itemCount: draft.screenshots.length,
                separatorBuilder: (_, __) => const SizedBox(width: 8),
                itemBuilder: (BuildContext context, int index) => AppImage(
                  url: draft.screenshots[index],
                  width: 92,
                  radius: SproutlyShapes.radiusMd,
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
