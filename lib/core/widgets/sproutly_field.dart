import 'package:material_ui/material_ui.dart';

import '../theme/sproutly_shapes.dart';
import '../theme/theme_context.dart';

/// Text input.
///
/// Sproutly does not use the stock decorated input: the field is a filled
/// surface with a lime focus ring, the label sits above the field (not inside
/// it), and the counter only appears when it matters.
class SproutlyField extends StatelessWidget {
  const SproutlyField({
    required this.label,
    super.key,
    this.controller,
    this.hint,
    this.helper,
    this.errorText,
    this.keyboardType,
    this.textInputAction,
    this.maxLength,
    this.maxLines = 1,
    this.minLines,
    this.obscureText = false,
    this.validator,
    this.onChanged,
    this.onSubmitted,
    this.prefixIcon,
    this.suffix,
    this.enabled = true,
    this.autofocus = false,
    this.textCapitalization = TextCapitalization.sentences,
    this.showCounter = false,
  });

  final String label;
  final TextEditingController? controller;
  final String? hint;
  final String? helper;
  final String? errorText;
  final TextInputType? keyboardType;
  final TextInputAction? textInputAction;
  final int? maxLength;
  final int maxLines;
  final int? minLines;
  final bool obscureText;
  final String? Function(String?)? validator;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;
  final IconData? prefixIcon;
  final Widget? suffix;
  final bool enabled;
  final bool autofocus;
  final TextCapitalization textCapitalization;
  final bool showCounter;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: <Widget>[
        Padding(
          padding: const EdgeInsets.only(left: 4, bottom: 6),
          child: Text(
            label,
            style: theme.textTheme.labelMedium!.copyWith(
              color: errorText != null ? theme.colorScheme.error : theme.colorScheme.onSurfaceVariant,
            ),
          ),
        ),
        TextFormField(
          controller: controller,
          enabled: enabled,
          autofocus: autofocus,
          keyboardType: keyboardType ?? (maxLines > 1 ? TextInputType.multiline : TextInputType.text),
          textInputAction: textInputAction,
          maxLength: maxLength,
          maxLines: obscureText ? 1 : maxLines,
          minLines: obscureText ? 1 : minLines,
          obscureText: obscureText,
          validator: validator,
          onChanged: onChanged,
          onFieldSubmitted: onSubmitted,
          textCapitalization: textCapitalization,
          style: theme.textTheme.bodyLarge,
          cursorColor: theme.colorScheme.primary,
          buildCounter: (BuildContext context, {required int currentLength, required bool isFocused, required int? maxLength}) {
            if (!showCounter) return null;
            return Text(
              '$currentLength / $maxLength',
              style: theme.textTheme.labelSmall,
            );
          },
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: theme.textTheme.bodyMedium!.copyWith(
              color: theme.colorScheme.onSurfaceVariant.withValues(alpha: 0.7),
            ),
            filled: true,
            fillColor: enabled
                ? theme.colorScheme.surfaceContainerLowest
                : theme.colorScheme.surfaceContainerHigh,
            prefixIcon: prefixIcon == null
                ? null
                : Icon(prefixIcon, size: 19, color: theme.colorScheme.onSurfaceVariant),
            suffixIcon: suffix,
            contentPadding: EdgeInsets.symmetric(
              horizontal: 16,
              vertical: maxLines > 1 ? 14 : 16,
            ),
            border: OutlineInputBorder(
              borderRadius: SproutlyShapes.radiusMd,
              borderSide: BorderSide(color: theme.colorScheme.outlineVariant),
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: SproutlyShapes.radiusMd,
              borderSide: BorderSide(color: theme.colorScheme.outlineVariant),
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: SproutlyShapes.radiusMd,
              borderSide: BorderSide(color: theme.colorScheme.primary, width: 1.6),
            ),
            errorBorder: OutlineInputBorder(
              borderRadius: SproutlyShapes.radiusMd,
              borderSide: BorderSide(color: theme.colorScheme.error),
            ),
            focusedErrorBorder: OutlineInputBorder(
              borderRadius: SproutlyShapes.radiusMd,
              borderSide: BorderSide(color: theme.colorScheme.error, width: 1.6),
            ),
            errorStyle: theme.textTheme.labelSmall!.copyWith(color: theme.colorScheme.error),
            errorText: errorText,
          ),
        ),
        if (helper != null)
          Padding(
            padding: const EdgeInsets.only(left: 4, top: 6),
            child: Text(helper!, style: theme.textTheme.labelSmall),
          ),
      ],
    );
  }
}

/// The search field: pill shaped, with a clear button and a subtle lime fill
/// while focused.
class SproutlySearchField extends StatelessWidget {
  const SproutlySearchField({
    required this.controller,
    super.key,
    this.hint = 'Search apps, developers, tags',
    this.onChanged,
    this.onSubmitted,
    this.onClear,
    this.autofocus = false,
  });

  final TextEditingController controller;
  final String hint;
  final ValueChanged<String>? onChanged;
  final ValueChanged<String>? onSubmitted;
  final VoidCallback? onClear;
  final bool autofocus;

  @override
  Widget build(BuildContext context) {
    final ThemeData theme = context.theme;
    return ValueListenableBuilder<TextEditingValue>(
      valueListenable: controller,
      builder: (BuildContext context, TextEditingValue value, Widget? _) {
        return TextField(
          controller: controller,
          autofocus: autofocus,
          onChanged: onChanged,
          onSubmitted: onSubmitted,
          textInputAction: TextInputAction.search,
          style: theme.textTheme.bodyMedium,
          decoration: InputDecoration(
            hintText: hint,
            hintStyle: theme.textTheme.bodyMedium!.copyWith(color: theme.colorScheme.onSurfaceVariant),
            prefixIcon: Icon(Icons.search_rounded, color: theme.colorScheme.onSurfaceVariant, size: 20),
            suffixIcon: value.text.isEmpty
                ? null
                : IconButton(
                    icon: const Icon(Icons.close_rounded, size: 18),
                    onPressed: () {
                      controller.clear();
                      onClear?.call();
                    },
                  ),
            filled: true,
            fillColor: theme.colorScheme.surfaceContainerHigh,
            isDense: true,
            contentPadding: const EdgeInsets.symmetric(vertical: 14),
            border: OutlineInputBorder(
              borderRadius: SproutlyShapes.radiusPill,
              borderSide: BorderSide.none,
            ),
            enabledBorder: OutlineInputBorder(
              borderRadius: SproutlyShapes.radiusPill,
              borderSide: BorderSide.none,
            ),
            focusedBorder: OutlineInputBorder(
              borderRadius: SproutlyShapes.radiusPill,
              borderSide: BorderSide(color: theme.colorScheme.primary, width: 1.6),
            ),
          ),
        );
      },
    );
  }
}
