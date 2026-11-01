import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/utils/validators.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/theme/theme_context.dart';
import 'auth_controller.dart';
import 'widgets/auth_scaffold.dart';

class SignUpScreen extends ConsumerStatefulWidget {
  const SignUpScreen({super.key});

  @override
  ConsumerState<SignUpScreen> createState() => _SignUpScreenState();
}

class _SignUpScreenState extends ConsumerState<SignUpScreen> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _name = TextEditingController();
  final TextEditingController _email = TextEditingController();
  final TextEditingController _password = TextEditingController();
  final TextEditingController _confirm = TextEditingController();
  bool _busy = false;
  bool _obscure = true;

  @override
  void dispose() {
    _name.dispose();
    _email.dispose();
    _password.dispose();
    _confirm.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    final bool ok = await ref.read(authControllerProvider.notifier).signUp(
          email: _email.text,
          password: _password.text,
          displayName: _name.text,
        );
    if (!mounted) return;
    setState(() => _busy = false);
    if (ok) context.go('/onboarding');
  }

  @override
  Widget build(BuildContext context) {
    final Object? error = ref.watch(authControllerProvider);
    return AuthScaffold(
      title: 'Create your account',
      subtitle: 'Publishing on Sproutly is free. Forever.',
      children: <Widget>[
        if (error != null) AuthErrorBanner(message: error.toString()),
        Form(
          key: _formKey,
          child: Column(
            children: <Widget>[
              SproutlyField(
                label: 'Display name',
                controller: _name,
                hint: 'How developers will see you',
                textInputAction: TextInputAction.next,
                textCapitalization: TextCapitalization.words,
                prefixIcon: Icons.person_outline_rounded,
                validator: (String? v) => Validators.required(v, label: 'Name'),
                autofocus: true,
              ),
              const SizedBox(height: 16),
              SproutlyField(
                label: 'Email',
                controller: _email,
                hint: 'you@example.com',
                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.next,
                textCapitalization: TextCapitalization.none,
                prefixIcon: Icons.alternate_email_rounded,
                validator: Validators.email,
              ),
              const SizedBox(height: 16),
              SproutlyField(
                label: 'Password',
                controller: _password,
                hint: 'At least 8 characters, letters and numbers',
                obscureText: _obscure,
                textInputAction: TextInputAction.next,
                textCapitalization: TextCapitalization.none,
                prefixIcon: Icons.lock_outline_rounded,
                validator: Validators.password,
                suffix: IconButton(
                  icon: Icon(_obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined, size: 19),
                  onPressed: () => setState(() => _obscure = !_obscure),
                ),
              ),
              const SizedBox(height: 16),
              SproutlyField(
                label: 'Confirm password',
                controller: _confirm,
                hint: 'Type it again',
                obscureText: _obscure,
                textInputAction: TextInputAction.done,
                textCapitalization: TextCapitalization.none,
                prefixIcon: Icons.lock_reset_rounded,
                validator: (String? v) => Validators.confirmPassword(v, _password.text),
                onSubmitted: (_) => _submit(),
              ),
            ],
          ),
        ),
        const SizedBox(height: 22),
        SproutlyButton(
          label: 'Create account',
          onPressed: _submit,
          isLoading: _busy,
          expand: true,
          size: SproutlyButtonSize.large,
        ),
        const SizedBox(height: 14),
        Text(
          'By creating an account you agree to the Sproutly terms and privacy policy. '
          'We send a verification email - you can browse before confirming it, but '
          'publishing needs a verified address.',
          style: context.text.labelSmall!.copyWith(height: 1.5),
        ),
        const SizedBox(height: 16),
        Center(
          child: Wrap(
            crossAxisAlignment: WrapCrossAlignment.center,
            children: <Widget>[
              Text('Already have an account?', style: context.text.bodySmall),
              TextButton(
                onPressed: _busy ? null : () => context.pop(),
                child: const Text('Sign in'),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
