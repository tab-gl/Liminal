import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import 'package:material_ui/material_ui.dart';

import '../../../core/utils/validators.dart';
import '../../../core/widgets/sproutly_button.dart';
import '../../../core/widgets/sproutly_field.dart';
import '../../../core/theme/theme_context.dart';
import 'auth_controller.dart';
import 'widgets/auth_scaffold.dart';

class SignInScreen extends ConsumerStatefulWidget {
  const SignInScreen({super.key, this.redirectTo});

  final String? redirectTo;

  @override
  ConsumerState<SignInScreen> createState() => _SignInScreenState();
}

class _SignInScreenState extends ConsumerState<SignInScreen> {
  final GlobalKey<FormState> _formKey = GlobalKey<FormState>();
  final TextEditingController _email = TextEditingController();
  final TextEditingController _password = TextEditingController();
  bool _busy = false;
  bool _obscure = true;

  @override
  void dispose() {
    _email.dispose();
    _password.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;
    setState(() => _busy = true);
    final bool ok = await ref.read(authControllerProvider.notifier).signIn(
          email: _email.text,
          password: _password.text,
        );
    if (!mounted) return;
    setState(() => _busy = false);
    if (ok) context.go(widget.redirectTo ?? '/discover');
  }

  Future<void> _google() async {
    setState(() => _busy = true);
    final bool ok = await ref.read(authControllerProvider.notifier).signInWithGoogle();
    if (!mounted) return;
    setState(() => _busy = false);
    if (ok) context.go(widget.redirectTo ?? '/discover');
  }

  @override
  Widget build(BuildContext context) {
    final Object? error = ref.watch(authControllerProvider);
    return AuthScaffold(
      title: 'Welcome back',
      subtitle: 'Sign in to keep growing the apps you love.',
      children: <Widget>[
        if (error != null) AuthErrorBanner(message: error.toString()),
        Form(
          key: _formKey,
          child: Column(
            children: <Widget>[
              SproutlyField(
                label: 'Email',
                controller: _email,
                hint: 'you@example.com',
                keyboardType: TextInputType.emailAddress,
                textInputAction: TextInputAction.next,
                textCapitalization: TextCapitalization.none,
                prefixIcon: Icons.alternate_email_rounded,
                validator: Validators.email,
                autofocus: true,
              ),
              const SizedBox(height: 16),
              SproutlyField(
                label: 'Password',
                controller: _password,
                hint: 'At least 8 characters',
                obscureText: _obscure,
                textInputAction: TextInputAction.done,
                textCapitalization: TextCapitalization.none,
                prefixIcon: Icons.lock_outline_rounded,
                validator: Validators.password,
                onSubmitted: (_) => _submit(),
                suffix: IconButton(
                  icon: Icon(_obscure ? Icons.visibility_outlined : Icons.visibility_off_outlined, size: 19),
                  onPressed: () => setState(() => _obscure = !_obscure),
                ),
              ),
            ],
          ),
        ),
        Align(
          alignment: Alignment.centerRight,
          child: TextButton(
            onPressed: _busy ? null : () => context.push('/forgot-password'),
            child: const Text('Forgot password?'),
          ),
        ),
        const SizedBox(height: 8),
        SproutlyButton(
          label: 'Sign in',
          onPressed: _submit,
          isLoading: _busy,
          expand: true,
          size: SproutlyButtonSize.large,
        ),
        const SizedBox(height: 12),
        Row(
          children: <Widget>[
            const Expanded(child: Divider()),
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 12),
              child: Text('or', style: context.text.labelSmall),
            ),
            const Expanded(child: Divider()),
          ],
        ),
        const SizedBox(height: 12),
        SproutlyButton(
          label: 'Continue with Google',
          icon: Icons.g_mobiledata_rounded,
          variant: SproutlyButtonVariant.outline,
          onPressed: _busy ? null : _google,
          expand: true,
          size: SproutlyButtonSize.large,
        ),
        const SizedBox(height: 26),
        Center(
          child: Wrap(
            crossAxisAlignment: WrapCrossAlignment.center,
            children: <Widget>[
              Text('New to Sproutly?', style: context.text.bodySmall),
              TextButton(
                onPressed: _busy ? null : () => context.push('/sign-up'),
                child: const Text('Create an account'),
              ),
            ],
          ),
        ),
      ],
    );
  }
}
