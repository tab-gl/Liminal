import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/errors/app_exception.dart';
import '../../../core/utils/validators.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/theme/theme_context.dart';
import 'auth_controller.dart';
import 'widgets/auth_scaffold.dart';

class ForgotPasswordScreen extends ConsumerStatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  ConsumerState<ForgotPasswordScreen> createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends ConsumerState<ForgotPasswordScreen> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _email = TextEditingController();
  bool _busy = false;
  bool _sent = false;

  @override
  void initState() {
    super.initState();
    // An error from another auth screen must not greet a fresh visit.
    Future<void>.microtask(() {
      if (mounted) ref.read(authControllerProvider.notifier).clearError();
    });
  }

  @override
  void dispose() {
    _email.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    final bool ok = await ref.read(authControllerProvider.notifier).sendPasswordReset(_email.text);
    if (!mounted) return;
    setState(() {
      _busy = false;
      _sent = ok;
    });
  }

  @override
  Widget build(BuildContext context) {
    final AppException? error = ref.watch(authControllerProvider);
    return AuthScaffold(
      title: _sent ? 'Check your inbox' : 'Reset your password',
      subtitle: _sent
          ? 'If an account exists for ${_email.text.trim()}, a reset link is on its way. The link expires in one hour.'
          : 'We will email you a secure link to choose a new password.',
      children: <Widget>[
        if (error != null) AuthErrorBanner(message: error.message),
        if (!_sent) ...<Widget>[
          Form(
            key: _formKey,
            child: SproutlyField(
              label: 'Email',
              controller: _email,
              hint: 'you@example.com',
              keyboardType: TextInputType.emailAddress,
              textCapitalization: TextCapitalization.none,
              prefixIcon: Icons.alternate_email_rounded,
              validator: Validators.email,
              autofocus: true,
            ),
          ),
          const SizedBox(height: 22),
          SproutlyButton(
            label: 'Send reset link',
            onPressed: _submit,
            isLoading: _busy,
            expand: true,
          ),
        ] else
          SproutlyButton(
            label: 'Back to sign in',
            variant: SproutlyButtonVariant.tonal,
            expand: true,
            onPressed: () => Navigator.of(context).maybePop(),
          ),
      ],
    );
  }
}
