import 'package:material_ui/material_ui.dart';

/// The fixed launch taxonomy. Ids are stable and are what Firestore stores;
/// labels can be localised without a migration.
class AppCategory {
  const AppCategory({
    required this.id,
    required this.label,
    required this.icon,
    required this.blurb,
  });

  final String id;
  final String label;
  final IconData icon;
  final String blurb;

  static const List<AppCategory> all = <AppCategory>[
    AppCategory(
      id: 'ai_ml',
      label: 'AI & Machine Learning',
      icon: Icons.auto_awesome_outlined,
      blurb: 'Models, agents and intelligent tools',
    ),
    AppCategory(
      id: 'developer_tools',
      label: 'Developer Tools',
      icon: Icons.terminal_outlined,
      blurb: 'Build, test and ship faster',
    ),
    AppCategory(
      id: 'productivity',
      label: 'Productivity',
      icon: Icons.checklist_rtl_outlined,
      blurb: 'Plan, focus and get things done',
    ),
    AppCategory(
      id: 'design',
      label: 'Design',
      icon: Icons.palette_outlined,
      blurb: 'Interfaces, type and creative craft',
    ),
    AppCategory(
      id: 'education',
      label: 'Education',
      icon: Icons.school_outlined,
      blurb: 'Learning for every stage',
    ),
    AppCategory(
      id: 'finance',
      label: 'Finance',
      icon: Icons.account_balance_wallet_outlined,
      blurb: 'Money, markets and tracking',
    ),
    AppCategory(
      id: 'business',
      label: 'Business',
      icon: Icons.work_outline,
      blurb: 'Teams, customers and operations',
    ),
    AppCategory(
      id: 'entertainment',
      label: 'Entertainment',
      icon: Icons.movie_outlined,
      blurb: 'Watch, listen and play',
    ),
    AppCategory(
      id: 'lifestyle',
      label: 'Lifestyle',
      icon: Icons.spa_outlined,
      blurb: 'Health, home and everyday life',
    ),
    AppCategory(
      id: 'security',
      label: 'Security',
      icon: Icons.shield_outlined,
      blurb: 'Privacy and protection',
    ),
    AppCategory(
      id: 'utilities',
      label: 'Utilities',
      icon: Icons.build_outlined,
      blurb: 'Small tools that do one thing well',
    ),
    AppCategory(
      id: 'open_source',
      label: 'Open Source',
      icon: Icons.code_outlined,
      blurb: 'Community-built and freely licensed',
    ),
    AppCategory(
      id: 'other',
      label: 'Other',
      icon: Icons.more_horiz_outlined,
      blurb: 'Everything that resists a label',
    ),
  ];

  static AppCategory byId(String id) => all.firstWhere(
        (AppCategory c) => c.id == id,
        orElse: () => all.last,
      );

  static List<String> get ids => all.map((AppCategory c) => c.id).toList();
}
