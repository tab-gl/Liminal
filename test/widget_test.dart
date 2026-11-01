import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sproutly/core/theme/sproutly_theme.dart';
import 'package:sproutly/core/widgets/skeleton.dart';
import 'package:sproutly/core/widgets/sproutly_button.dart';
import 'package:sproutly/shared/models/application.dart';
import 'package:sproutly/shared/widgets/application_card.dart';

/// App-level smoke test.
///
/// Deliberately does not boot `SproutlyApp`: that requires Firebase, and a test
/// that needs a backend is a test that runs nowhere. These pump the pieces that
/// carry the design system and the feed's most-rendered widget.
Widget _host(Widget child) => MaterialApp(
      theme: SproutlyTheme.light(),
      home: Scaffold(body: SingleChildScrollView(child: child)),
    );

void main() {
  testWidgets('the theme builds with the brand tokens attached', (WidgetTester tester) async {
    await tester.pumpWidget(_host(const SizedBox.shrink()));
    final ThemeData theme = Theme.of(tester.element(find.byType(Scaffold)));
    expect(theme.colorScheme.primary, const Color(0xFF3F4C06));
    expect(theme.textTheme.bodyMedium?.fontFamily, 'DMSans');
  });

  testWidgets('a primary button renders its label and fires once', (WidgetTester tester) async {
    int taps = 0;
    await tester.pumpWidget(_host(SproutlyButton(label: 'Publish', onPressed: () => taps++)));
    expect(find.text('Publish'), findsOneWidget);
    await tester.tap(find.byType(SproutlyButton));
    await tester.pumpAndSettle();
    expect(taps, 1);
  });

  testWidgets('a busy button ignores taps and shows progress', (WidgetTester tester) async {
    int taps = 0;
    await tester.pumpWidget(
      _host(SproutlyButton(label: 'Saving', isLoading: true, onPressed: () => taps++)),
    );
    await tester.tap(find.byType(SproutlyButton));
    await tester.pump(const Duration(milliseconds: 50));
    expect(taps, 0);
    expect(find.byType(CircularProgressIndicator), findsOneWidget);
  });

  testWidgets('a listing card shows the full decision set', (WidgetTester tester) async {
    final AppListing listing = AppListing.fromDoc('a1', <String, dynamic>{
      'ownerId': 'dev-1',
      'ownerName': 'Asha Rao',
      'name': 'Sprout Notes',
      'tagline': 'Notes that grow with you',
      'categoryId': 'productivity',
      'status': 'approved',
      'rating': 4.5,
      'reviewCount': 12,
      'pricingModel': 'free',
      'createdAt': DateTime.utc(2026, 1, 1),
      'updatedAt': DateTime.utc(2026, 1, 1),
    });

    await tester.pumpWidget(_host(ApplicationCard(listing: listing, onTap: () {})));
    await tester.pumpAndSettle();

    expect(find.text('Sprout Notes'), findsOneWidget);
    expect(find.text('Notes that grow with you'), findsOneWidget);
    expect(find.text('4.5'), findsOneWidget);
    expect(find.text('Asha Rao'), findsOneWidget);
    expect(find.text('Free'), findsOneWidget);
    expect(find.text('Sponsored'), findsNothing);
  });

  testWidgets('skeletons render without a shimmer animation when motion is reduced',
      (WidgetTester tester) async {
    await tester.pumpWidget(
      MaterialApp(
        theme: SproutlyTheme.light(),
        home: const MediaQuery(
          data: MediaQueryData(disableAnimations: true),
          child: Scaffold(body: AppCardSkeleton()),
        ),
      ),
    );
    await tester.pump();
    expect(find.byType(AppCardSkeleton), findsOneWidget);
  });
}
