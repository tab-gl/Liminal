import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/errors/error_mapper.dart';
import '../../../core/config/app_config.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/services/payment_service.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/badges.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/widgets/sproutly_page.dart';
import '../../../core/widgets/sproutly_sheet.dart';
import '../../../core/widgets/state_views.dart';
import '../../../shared/models/campaign.dart';
import '../../../shared/models/enums.dart';
import '../data/campaign_repository_impl.dart';

class CampaignDetailScreen extends ConsumerWidget {
  const CampaignDetailScreen({required this.campaignId, super.key});

  final String campaignId;

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final AsyncValue<Campaign?> async = ref.watch(campaignProvider(campaignId));

    return SproutlyPage(
      title: 'Campaign',
      showBack: true,
      scrollable: false,
      padding: EdgeInsets.zero,
      child: async.when(
        loading: () => const Center(child: SproutlyLoader()),
        error: (Object error, StackTrace _) => ErrorStateView(
          error: error,
          onRetry: () => ref.invalidate(campaignProvider(campaignId)),
        ),
        data: (Campaign? campaign) {
          if (campaign == null) {
            return const EmptyView(
              title: 'Campaign not found',
              message: 'It may have been removed.',
              icon: Icons.campaign_outlined,
            );
          }
          return ListView(
            padding: const EdgeInsets.fromLTRB(20, 8, 20, 120),
            children: <Widget>[
              Row(
                children: <Widget>[
                  Expanded(child: Text(campaign.name, style: context.text.headlineSmall)),
                  StatusChip(status: campaign.status.wire),
                ],
              ),
              const SizedBox(height: 4),
              Text(
                '${campaign.objective.label} · ${campaign.applicationName}',
                style: context.text.labelSmall,
              ),
              if (campaign.status == CampaignStatus.rejected && campaign.rejectionReason != null)
                Container(
                  margin: const EdgeInsets.only(top: 12),
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: context.colors.errorContainer,
                    borderRadius: SproutlyShapes.radiusMd,
                  ),
                  child: Text(
                    campaign.rejectionReason!,
                    style: context.text.bodySmall!.copyWith(color: context.colors.onErrorContainer),
                  ),
                ),
              const SizedBox(height: 16),
              GridView.count(
                crossAxisCount: 2,
                shrinkWrap: true,
                physics: const NeverScrollableScrollPhysics(),
                crossAxisSpacing: 10,
                mainAxisSpacing: 10,
                childAspectRatio: 1.75,
                children: <Widget>[
                  MetricTile(
                    label: 'Impressions',
                    value: Formatters.compactCount(campaign.impressions),
                    icon: Icons.visibility_outlined,
                    emphasis: true,
                  ),
                  MetricTile(
                    label: 'Clicks',
                    value: Formatters.compactCount(campaign.clicks),
                    icon: Icons.ads_click_rounded,
                  ),
                  MetricTile(
                    label: 'CTR',
                    value: Formatters.percent(campaign.ctr),
                    icon: Icons.trending_up_rounded,
                  ),
                  MetricTile(
                    label: 'Spent',
                    value: Formatters.inr(campaign.spendPaise),
                    icon: Icons.payments_outlined,
                  ),
                ],
              ),
              const SizedBox(height: 18),
              Text('Budget', style: context.text.titleSmall),
              const SizedBox(height: 8),
              ClipRRect(
                borderRadius: BorderRadius.circular(99),
                child: LinearProgressIndicator(
                  value: campaign.spendRatio,
                  minHeight: 9,
                  backgroundColor: context.colors.surfaceContainerHigh,
                  color: context.colors.primary,
                ),
              ),
              const SizedBox(height: 6),
              Row(
                children: <Widget>[
                  Text(
                    '${Formatters.inr(campaign.spendPaise)} spent',
                    style: context.text.labelSmall,
                  ),
                  const Spacer(),
                  Text(
                    '${Formatters.inr(campaign.remainingPaise)} remaining',
                    style: context.text.labelSmall,
                  ),
                ],
              ),
              const SizedBox(height: 18),
              Text('Schedule', style: context.text.titleSmall),
              const SizedBox(height: 4),
              Text(
                '${campaign.startDate.day}/${campaign.startDate.month}/${campaign.startDate.year} '
                'to ${campaign.endDate.day}/${campaign.endDate.month}/${campaign.endDate.year}',
                style: context.text.bodySmall,
              ),
              const SizedBox(height: 16),
              Text('Placements', style: context.text.titleSmall),
              const SizedBox(height: 6),
              Wrap(
                spacing: 6,
                runSpacing: 6,
                children: <Widget>[
                  for (final AdPlacement placement in campaign.placements)
                    SproutlyPill(label: placement.label, dense: true),
                ],
              ),
              const SizedBox(height: 16),
              Text('Targeting', style: context.text.titleSmall),
              const SizedBox(height: 4),
              Text(
                campaign.targeting.isBroad
                    ? 'Everyone on Sproutly.'
                    : '${campaign.targeting.categoryIds.length} categories · '
                        '${campaign.targeting.countries.isEmpty ? "worldwide" : campaign.targeting.countries.join(", ")}',
                style: context.text.bodySmall,
              ),
              const SizedBox(height: 20),
              if (campaign.status == CampaignStatus.active)
                SproutlyButton(
                  label: 'Pause campaign',
                  icon: Icons.pause_rounded,
                  variant: SproutlyButtonVariant.outline,
                  expand: true,
                  onPressed: () async {
                    await ref.read(campaignRepositoryProvider).pause(campaign.id);
                  },
                ),
              if (campaign.status == CampaignStatus.paused)
                SproutlyButton(
                  label: 'Resume campaign',
                  icon: Icons.play_arrow_rounded,
                  expand: true,
                  onPressed: () async {
                    await ref.read(campaignRepositoryProvider).resume(campaign.id);
                  },
                ),
              if (campaign.status == CampaignStatus.active ||
                  campaign.status == CampaignStatus.paused ||
                  campaign.status == CampaignStatus.exhausted) ...<Widget>[
                const SizedBox(height: 10),
                SproutlyButton(
                  label: 'Add budget',
                  icon: Icons.add_card_rounded,
                  variant: SproutlyButtonVariant.tonal,
                  expand: true,
                  onPressed: () => _addBudget(context, ref, campaign),
                ),
              ],
              const SizedBox(height: 18),
              Text(
                'Impressions and clicks are recorded server side with fraud checks. '
                'Budget is credited only after the payment provider confirms capture.',
                style: context.text.labelSmall!.copyWith(height: 1.5),
              ),
            ],
          );
        },
      ),
    );
  }

  Future<void> _addBudget(BuildContext context, WidgetRef ref, Campaign campaign) async {
    final TextEditingController amount = TextEditingController(text: '1000');
    final String? choice = await showModalBottomSheet<String>(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (BuildContext context) => Padding(
        padding: EdgeInsets.only(bottom: MediaQuery.viewInsetsOf(context).bottom),
        child: SproutlySheet(
          title: 'Add budget',
          subtitle: 'Money is credited to this campaign only, never to a wallet.',
          actions: <Widget>[
            SproutlyButton(
              label: AppConfig.hasRazorpay ? 'Pay with Razorpay' : 'Razorpay unavailable',
              expand: true,
              onPressed: AppConfig.hasRazorpay ? () => Navigator.pop(context, 'razorpay') : null,
            ),
          ],
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: <Widget>[
              SproutlyField(
                label: 'Amount (INR)',
                controller: amount,
                keyboardType: TextInputType.number,
                helper: 'Minimum Rs 500.',
              ),
              if (AppConfig.hasStripe) ...<Widget>[
                const SizedBox(height: 12),
                SproutlyButton(
                  label: 'Pay with card (Stripe)',
                  variant: SproutlyButtonVariant.outline,
                  expand: true,
                  onPressed: () => Navigator.pop(context, 'stripe'),
                ),
              ],
            ],
          ),
        ),
      ),
    );
    final int? paise = Formatters.rupeesToPaise(amount.text);
    amount.dispose();
    if (choice == null || paise == null) return;

    final PaymentService payments = ref.read(paymentProvider);
    try {
      final PaymentResult result = choice == 'razorpay'
          ? await payments.payWithRazorpay(campaignId: campaign.id, amountPaise: paise)
          : await payments.payWithStripe(campaignId: campaign.id, amountPaise: paise);
      if (!context.mounted) return;
      final String message = switch (result.status) {
        PaymentStatus.success => result.message ?? 'Payment submitted for verification.',
        PaymentStatus.cancelled => 'Payment cancelled.',
        PaymentStatus.failed => result.message ?? 'The payment failed.',
        PaymentStatus.unavailable => result.message ?? 'Payments are not configured.',
      };
      ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(message)));
    } catch (error) {
      if (context.mounted) {
        ScaffoldMessenger.of(context).showSnackBar(SnackBar(content: Text(errorMessage(error))));
      }
    }
  }
}
