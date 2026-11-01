import 'package:material_ui/material_ui.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:sproutly/core/theme/sproutly_theme.dart';
import 'package:sproutly/shared/models/application.dart';
import 'package:sproutly/shared/widgets/application_card.dart';

AppListing _listing({bool sponsored = false}) => AppListing.fromDoc('app-1', <String, dynamic>{
      'ownerId': 'dev-1',
      'ownerName': 'Asha Rao',
      'name': 'Sprout Notes',
      'tagline': 'Notes that grow with you',
      'description': 'A' * 80,
      'categoryId': 'productivity',
      'status': 'approved',
      'rating': 4.5,
      'reviewCount': 12,
      'pricingModel': 'free',
      'sponsored': sponsored,
      'createdAt': DateTime.utc(2026, 1, 1),
      'updatedAt': DateTime.utc(2026, 1, 1),
    });

Widget _wrap(Widget child) => MaterialApp(
      theme: SproutlyTheme.light(),
      home: Scaffold(body: SingleChildScrollView(child: child)),
    );

void main() {
  testWidgets('renders the decision set: name, tagline, rating, developer', (WidgetTester tester) async {
    await tester.pumpWidget(_wrap(ApplicationCard(listing: _listing(), onTap: () {})));
    await tester.pumpAndSettle();

    expect(find.text('Sprout Notes'), findsOneWidget);
    expect(find.text('Notes that grow with you'), findsOneWidget);
    expect(find.text('4.5'), findsOneWidget);
    expect(find.textContaining('(12)'), findsOneWidget);
    expect(find.text('Asha Rao'), findsOneWidget);
    expect(find.text('Free'), findsOneWidget);
    expect(find.text('Sponsored'), findsNothing);
  });

  testWidgets('labels a paid placement', (WidgetTester tester) async {
    await tester.pumpWidget(_wrap(ApplicationCard(listing: _listing(sponsored: true), onTap: () {})));
    await tester.pumpAndSettle();
    expect(find.text('Sponsored'), findsOneWidget);
  });

  testWidgets('reports taps', (WidgetTester tester) async {
    int taps = 0;
    await tester.pumpWidget(_wrap(ApplicationCard(listing: _listing(), onTap: () => taps++)));
    await tester.tap(find.byType(ApplicationCard));
    await tester.pumpAndSettle();
    expect(taps, 1);
  });
}
