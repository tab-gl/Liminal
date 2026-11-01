import 'package:fl_chart/fl_chart.dart';
import 'package:material_ui/material_ui.dart';

import '../../../../core/theme/theme_context.dart';
import '../../domain/dashboard_repository.dart';

/// Daily views as a bar chart.
///
/// Deliberately plain: no gradients, no animation on load, thin bars with a
/// rounded cap. Numbers come straight from the daily rollup documents, so an
/// empty series renders an honest "no activity" line rather than a fake curve.
class ViewsChart extends StatelessWidget {
  const ViewsChart({required this.days, super.key, this.metric = 'views'});

  final List<DailyStat> days;
  final String metric;

  int _value(DailyStat day) => switch (metric) {
        'clicks' => day.clicks,
        'downloads' => day.downloads,
        'bookmarks' => day.bookmarks,
        _ => day.views,
      };

  @override
  Widget build(BuildContext context) {
    if (days.isEmpty) {
      return Container(
        height: 160,
        alignment: Alignment.center,
        decoration: BoxDecoration(
          color: context.colors.surfaceContainerLow,
          borderRadius: BorderRadius.circular(18),
        ),
        child: Text(
          'No activity recorded yet.\nStats appear within an hour of the first view.',
          textAlign: TextAlign.center,
          style: context.text.labelSmall!.copyWith(height: 1.5),
        ),
      );
    }

    final double maxValue = days
        .map((DailyStat d) => _value(d).toDouble())
        .fold<double>(0, (double a, double b) => a > b ? a : b);

    return SizedBox(
      height: 170,
      child: BarChart(
        BarChartData(
          maxY: maxValue == 0 ? 4 : maxValue * 1.25,
          alignment: BarChartAlignment.spaceAround,
          gridData: FlGridData(
            show: true,
            drawVerticalLine: false,
            horizontalInterval: maxValue == 0 ? 1 : (maxValue * 1.25) / 3,
            getDrawingHorizontalLine: (double value) => FlLine(
              color: context.colors.outlineVariant.withValues(alpha: 0.6),
              strokeWidth: 1,
            ),
          ),
          borderData: FlBorderData(show: false),
          titlesData: FlTitlesData(
            topTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            rightTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            leftTitles: const AxisTitles(sideTitles: SideTitles(showTitles: false)),
            bottomTitles: AxisTitles(
              sideTitles: SideTitles(
                showTitles: true,
                reservedSize: 22,
                getTitlesWidget: (double value, TitleMeta meta) {
                  final int index = value.toInt();
                  if (index < 0 || index >= days.length) return const SizedBox.shrink();
                  // Label roughly every fifth day to avoid collisions.
                  if (days.length > 8 && index % 5 != 0) return const SizedBox.shrink();
                  return Padding(
                    padding: const EdgeInsets.only(top: 6),
                    child: Text(
                      '${days[index].date.day}/${days[index].date.month}',
                      style: context.text.labelSmall!.copyWith(fontSize: 9),
                    ),
                  );
                },
              ),
            ),
          ),
          barTouchData: BarTouchData(
            touchTooltipData: BarTouchTooltipData(
              getTooltipColor: (BarChartGroupData group) => context.colors.inverseSurface,
              getTooltipItem: (BarChartGroupData group, int groupIndex, BarChartRodData rod, int rodIndex) {
                final DailyStat day = days[group.x];
                return BarTooltipItem(
                  '${day.date.day}/${day.date.month}\n${_value(day)} $metric',
                  context.text.labelSmall!.copyWith(color: context.colors.onInverseSurface),
                );
              },
            ),
          ),
          barGroups: <BarChartGroupData>[
            for (int i = 0; i < days.length; i++)
              BarChartGroupData(
                x: i,
                barRods: <BarChartRodData>[
                  BarChartRodData(
                    toY: _value(days[i]).toDouble(),
                    width: days.length > 20 ? 5 : 9,
                    color: context.colors.primary,
                    borderRadius: const BorderRadius.vertical(top: Radius.circular(4)),
                  ),
                ],
              ),
          ],
        ),
      ),
    );
  }
}
