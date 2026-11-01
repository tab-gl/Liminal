import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/constants/categories.dart';
import '../../../core/theme/sproutly_shapes.dart';
import '../../../core/theme/theme_context.dart';
import '../../../core/widgets/sproutly_page.dart';

class CategoriesScreen extends StatelessWidget {
  const CategoriesScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return SproutlyPage(
      title: 'Categories',
      subtitle: 'Find your people by what they build',
      scrollable: true,
      child: GridView.builder(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        padding: const EdgeInsets.only(top: 8),
        gridDelegate: const SliverGridDelegateWithMaxCrossAxisExtent(
          maxCrossAxisExtent: 220,
          mainAxisSpacing: 12,
          crossAxisSpacing: 12,
          childAspectRatio: 1.35,
        ),
        itemCount: AppCategory.all.length,
        itemBuilder: (BuildContext context, int index) {
          final AppCategory category = AppCategory.all[index];
          return InkWell(
            borderRadius: SproutlyShapes.cardRadius(),
            onTap: () => context.push('/category/${category.id}'),
            child: Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: context.colors.surfaceContainerLowest,
                borderRadius: SproutlyShapes.cardRadius(),
                border: Border.all(color: context.colors.outlineVariant),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: <Widget>[
                  Container(
                    height: 34,
                    width: 34,
                    decoration: BoxDecoration(
                      color: context.colors.primaryContainer,
                      borderRadius: SproutlyShapes.radiusSm,
                    ),
                    child: Icon(
                      category.icon,
                      size: 18,
                      color: context.colors.onPrimaryContainer,
                    ),
                  ),
                  const Spacer(),
                  Text(
                    category.label,
                    maxLines: 2,
                    overflow: TextOverflow.ellipsis,
                    style: context.text.titleSmall,
                  ),
                  const SizedBox(height: 2),
                  Text(
                    category.blurb,
                    maxLines: 1,
                    overflow: TextOverflow.ellipsis,
                    style: context.text.labelSmall,
                  ),
                ],
              ),
            ),
          );
        },
      ),
    );
  }
}
