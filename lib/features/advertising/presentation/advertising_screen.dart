import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/constants/app_constants.dart';
import '../../../core/constants/categories.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/widgets/sproutly_page.dart';
import '../../../core/widgets/sproutly_sheet.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/application.dart';
import '../../../shared/models/campaign.dart';
import '../../../shared/models/enums.dart';
import '../../publishing/data/publishing_repository_impl.dart';
import '../../publishing/presentation/publish_controller.dart';
import '../data/campaign_repository_impl.dart';

class AdvertisingScreen extends ConsumerWidget {
  const AdvertisingScreen({super.key, this.preselectAppId});

  final String? preselectAppId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<List<Campaign>> async = ref.watch(myCampaignsProvider);

    return SproutlyPage(
      title: 'Advertising',
      subtitle: 'Self-service promotion, priced per thousand impressions',
      showBack: true,
      scrollable: false,
      padding: EdgeInsets.zero,
      child: async.when(
        loading: () => const Center(child: SproutlyLoader()),
        error: (Object error, StackTrace _) => ErrorStateView(
          error: error,
          onRetry: () => ref.invalidate(myCampaignsProvider),
        ),
        data: (List<Campaign> campaigns) => ListView(
          padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
          children: <Widget>[
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: context.colors.primaryContainer.withValues(alpha: 0.55),
                borderRadius: SproutlyShapes.radiusLg,
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Text(
                    'How Sproutly ads work',
                    style: context.text.titleSmall!.copyWith(color: context.colors.onPrimaryContainer),
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'You set a budget. Delivery is capped daily and every '
                    'placement is labelled Sponsored. Impressions and clicks are '
                    'counted server side, so the numbers you see are the numbers '
                    'you pay for.',
                    style: context.text.bodySmall!.copyWith(
                      color: context.colors.onPrimaryContainer,
                      height: 1.5,
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
            SproutlyButton(
              label: 'New campaign',
              icon: Icons.add_rounded,
              expand: true,
              onPressed: () => _openComposer(context, ref, preselectAppId),
            ),
            const SizedBox(height: 20),
            Text('Your campaigns', style: context.text.titleLarge),
            const SizedBox(height: 10),
            if (campaigns.isEmpty)
              EmptyView(
                title: 'No campaigns yet',
                message: 'Campaigns go live after a short approval check.',
                icon: Icons.campaign_outlined,
                compact: true,
              )
            else
              for (final Campaign campaign in campaigns)
                Padding(
                  padding: const EdgeInsets.only(bottom: 10),
                  child: _CampaignRow(campaign: campaign),
                ),
          ],
        ),
      ),
    );
  }

  Future<void> _openComposer(BuildContext context, WidgetRef ref, String? preselect) async {
    final List<AppListing> apps = await ref.read(publishingRepositoryProvider).myApps();
    if (!context.mounted) return;
    if (apps.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Publish an app before promoting it.')),
      );
      return;
    }
    await showModalBottomSheet<void>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) => _CampaignComposer(
        apps: apps,
        preselectAppId: preselect,
      ),
    );
  }
}

class _CampaignRow extends StatelessWidget {
  const _CampaignRow({required this.campaign});

  final Campaign campaign;

  @override
  Widget build(BuildContext context) {
    return InkWell(
      borderRadius: SproutlyShapes.radiusLg,
      onTap: () => context.push('/campaign/${campaign.id}'),
      child: Container(
        padding: const EdgeInsets.all(14),
        decoration: BoxDecoration(
          color: context.colors.surfaceContainerLowest,
          borderRadius: SproutlyShapes.radiusLg,
          border: Border.all(color: context.colors.outlineVariant),
        ),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            Row(
              children: <Widget>[
                Expanded(
                  child: Text(
                    campaign.name,
                    style: context.text.titleSmall,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                  ),
                ),
                StatusChip(status: campaign.status.wire),
              ],
            ),
            const SizedBox(height: 4),
            Text(
              campaign.applicationName.isEmpty ? campaign.applicationId : campaign.applicationName,
              style: context.text.labelSmall,
            ),
            const SizedBox(height: 10),
            ClipRRect(
              borderRadius: BorderRadius.circular(99),
              child: LinearProgressIndicator(
                value: campaign.spendRatio,
                minHeight: 6,
                backgroundColor: context.colors.surfaceContainerHigh,
                color: context.colors.primary,
              ),
            ),
            const SizedBox(height: 8),
            Row(
              children: <Widget>[
                Text(
                  '${Formatters.inr(campaign.spendPaise)} of ${Formatters.inr(campaign.budgetPaise)}',
                  style: context.text.labelSmall,
                ),
                const Spacer(),
                Text(
                  '${campaign.impressions} impressions · ${Formatters.percent(campaign.ctr)} CTR',
                  style: context.text.labelSmall,
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}

class _CampaignComposer extends ConsumerStatefulWidget {
  const _CampaignComposer({required this.apps, this.preselectAppId});

  final List<AppListing> apps;
  final String? preselectAppId;

  @override
  ConsumerState<_CampaignComposer> createState() => _CampaignComposerState();
}

class _CampaignComposerState extends ConsumerState<_CampaignComposer> {
  final TextEditingController _name = TextEditingController();
  final TextEditingController _budget = TextEditingController(text: '2000');
  final TextEditingController _headline = TextEditingController();
  late String _appId = widget.preselectAppId ?? widget.apps.first.id;
  CampaignObjective _objective = CampaignObjective.awareness;
  final Set<AdPlacement> _placements = <AdPlacement>{AdPlacement.sponsoredCard};
  final Set<String> _categories = <String>{};
  late DateTime _start = DateTime.now().toUtc();
  late DateTime _end = DateTime.now().toUtc().add(const Duration(days: 14));
  bool _busy = false;
  String? _error;

  @override
  void dispose() {
    _name.dispose();
    _budget.dispose();
    _headline.dispose();
    super.dispose();
  }

  Future<void> _pickDate({required bool isStart}) async {
    final DateTime? picked = await showDatePicker(
      context: context,
      initialDate: isStart ? _start : _end,
      firstDate: DateTime.now(),
      lastDate: DateTime.now().add(const Duration(days: 365)),
    );
    if (picked == null) return;
    setState(() {
      if (isStart) {
        _start = picked.toUtc();
        if (_end.isBefore(_start)) _end = _start.add(const Duration(days: 7));
      } else {
        _end = picked.toUtc();
      }
    });
  }

  Future<void> _create() async {
    final int? paise = Formatters.rupeesToPaise(_budget.text);
    if (_name.text.trim().isEmpty) {
      setState(() => _error = 'Give the campaign a name.');
      return;
    }
    if (paise == null || Validators.campaignBudget(_budget.text) != null) {
      setState(() => _error = Validators.campaignBudget(_budget.text) ?? 'Enter a budget.');
      return;
    }
    if (_placements.isEmpty) {
      setState(() => _error = 'Choose at least one placement.');
      return;
    }
    setState(() {
      _busy = true;
      _error = null;
    });
    final AppListing app = widget.apps.firstWhere((AppListing a) => a.id == _appId);
    try {
      final String id = await ref.read(campaignRepositoryProvider).create(
            Campaign(
              id: '',
              ownerId: '',
              applicationId: app.id,
              applicationName: app.name,
              applicationIconUrl: app.iconUrl,
              name: _name.text.trim(),
              objective: _objective,
              budgetPaise: paise,
              startDate: _start,
              endDate: _end,
              createdAt: DateTime.now().toUtc(),
              updatedAt: DateTime.now().toUtc(),
              placements: _placements.toList(),
              targeting: CampaignTargeting(categoryIds: _categories.toList()),
              creative: CampaignCreative(
                headline: _headline.text.trim().isEmpty ? app.tagline : _headline.text.trim(),
                body: app.tagline,
                imageUrl: app.iconUrl,
              ),
            ),
          );
      await ref.read(analyticsProvider).campaignCreated(id, paise);
      ref.invalidate(myCampaignsProvider);
      if (!mounted) return;
      Navigator.of(context).pop();
      context.push('/campaign/$id');
    } catch (error) {
      if (mounted) {
        setState(() {
          _busy = false;
          _error = error.toString();
        });
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    return Padding(
      padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
      child: SproutlySheet(
        title: 'New campaign',
        subtitle: 'Goes live after approval, usually within a few hours.',
        actions: <Widget>[
          SproutlyButton(
            label: 'Create campaign',
            expand: true,
            isLoading: _busy,
            onPressed: _busy ? null : _create,
          ),
        ],
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: <Widget>[
            if (_error != null)
              Padding(
                padding: const EdgeInsets.only(bottom: 12),
                child: Text(
                  _error!,
                  style: context.text.labelSmall!.copyWith(color: context.colors.error),
                ),
              ),
            SproutlyField(
              label: 'Campaign name',
              controller: _name,
              hint: 'June launch push',
              maxLength: AppConstants.maxCampaignNameLength,
            ),
            const SizedBox(height: 14),
            Text('Promoting', style: context.text.labelMedium),
            const SizedBox(height: 6),
            DropdownButtonFormField<String>(
              initialValue: _appId,
              items: <DropdownMenuItem<String>>[
                for (final AppListing app in widget.apps)
                  DropdownMenuItem<String>(value: app.id, child: Text(app.name)),
              ],
              onChanged: (String? value) => setState(() => _appId = value ?? _appId),
              decoration: const InputDecoration(border: OutlineInputBorder()),
            ),
            const SizedBox(height: 14),
            SproutlyField(
              label: 'Budget (INR)',
              controller: _budget,
              hint: '2000',
              keyboardType: TextInputType.number,
              helper: 'Minimum Rs ${AppConstants.minimumCampaignBudgetPaise ~/ 100}. '
                  'Charged only for delivered impressions.',
            ),
            const SizedBox(height: 14),
            SproutlyField(
              label: 'Ad headline',
              controller: _headline,
              hint: 'Optional - defaults to your tagline',
              maxLength: 80,
            ),
            const SizedBox(height: 14),
            Text('Objective', style: context.text.labelMedium),
            const SizedBox(height: 6),
            Wrap(
              spacing: 8,
              children: <Widget>[
                for (final CampaignObjective objective in CampaignObjective.values)
                  ChoiceChip(
                    label: Text(objective.label),
                    selected: _objective == objective,
                    onSelected: (_) => setState(() => _objective = objective),
                  ),
              ],
            ),
            const SizedBox(height: 14),
            Text('Placements', style: context.text.labelMedium),
            const SizedBox(height: 6),
            for (final AdPlacement placement in AdPlacement.values)
              CheckboxListTile(
                value: _placements.contains(placement),
                onChanged: (bool? on) => setState(() {
                  if (on == true) {
                    _placements.add(placement);
                  } else {
                    _placements.remove(placement);
                  }
                }),
                title: Text(placement.label, style: context.text.bodySmall),
                contentPadding: EdgeInsets.zero,
                dense: true,
                controlAffinity: ListTileControlAffinity.leading,
              ),
            const SizedBox(height: 10),
            Text('Target categories (optional)', style: context.text.labelMedium),
            const SizedBox(height: 6),
            Wrap(
              spacing: 6,
              runSpacing: 6,
              children: <Widget>[
                for (final AppCategory category in AppCategory.all)
                  FilterChip(
                    label: Text(category.label),
                    selected: _categories.contains(category.id),
                    onSelected: (bool on) => setState(() {
                      if (on) {
                        _categories.add(category.id);
                      } else {
                        _categories.remove(category.id);
                      }
                    }),
                  ),
              ],
            ),
            const SizedBox(height: 14),
            Row(
              children: <Widget>[
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _pickDate(isStart: true),
                    icon: const Icon(Icons.calendar_today_rounded, size: 15),
                    label: Text('Start ${_start.day}/${_start.month}'),
                  ),
                ),
                const SizedBox(width: 10),
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () => _pickDate(isStart: false),
                    icon: const Icon(Icons.event_available_rounded, size: 15),
                    label: Text('End ${_end.day}/${_end.month}'),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
