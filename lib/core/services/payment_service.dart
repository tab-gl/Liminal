import 'dart:async';

import 'package:flutter/foundation.dart';
import 'package:razorpay_flutter/razorpay_flutter.dart';
import 'package:url_launcher/url_launcher.dart';

import '../../firebase/callable_client.dart';
import '../config/app_config.dart';
import '../errors/app_exception.dart';
import '../errors/error_mapper.dart';

/// Outcome of a checkout attempt.
enum PaymentStatus { success, cancelled, failed, unavailable }

class PaymentResult {
  const PaymentResult(this.status, {this.paymentId, this.message, this.redirectUrl});

  final PaymentStatus status;
  final String? paymentId;
  final String? message;

  /// Stripe hosted-checkout URL, opened in the system browser.
  final String? redirectUrl;
}

/// Payment handling for campaign funding.
///
/// The client never computes money. A Cloud Function creates the order, the
/// native Razorpay sheet collects the instrument, and a webhook - not the
/// client - marks the payment as captured and credits the campaign budget.
/// That is why [PaymentStatus.success] means "submitted for verification",
/// not "money received".
class PaymentService {
  PaymentService({CallableClient? callables, Razorpay? razorpay})
      : _callables = callables ?? CallableClient(),
        _razorpay = razorpay;

  final CallableClient _callables;
  Razorpay? _razorpay;
  Completer<PaymentResult>? _pending;

  bool get isRazorpayAvailable => AppConfig.hasRazorpay;
  bool get isStripeAvailable => AppConfig.hasStripe;

  void _ensureRazorpay() {
    if (_razorpay != null) return;
    final Razorpay instance = Razorpay();
    instance.on(Razorpay.EVENT_PAYMENT_SUCCESS, _onSuccess);
    instance.on(Razorpay.EVENT_PAYMENT_ERROR, _onError);
    instance.on(Razorpay.EVENT_EXTERNAL_WALLET, _onExternalWallet);
    _razorpay = instance;
  }

  /// Opens the native Razorpay sheet for [amountPaise] funding [campaignId].
  Future<PaymentResult> payWithRazorpay({
    required String campaignId,
    required int amountPaise,
  }) async {
    if (!isRazorpayAvailable) {
      return const PaymentResult(
        PaymentStatus.unavailable,
        message: 'Razorpay is not configured in this build.',
      );
    }
    _ensureRazorpay();
    try {
      // Order id, amount and currency are decided server side.
      final Map<String, dynamic> order = await _callables.call('createRazorpayOrder', <String, dynamic>{
        'campaignId': campaignId,
        'amountPaise': amountPaise,
      });
      final String orderId = order['orderId'] as String? ?? '';
      if (orderId.isEmpty) {
        throw const AppException(AppErrorCode.paymentFailed, 'The payment could not be started.');
      }

      _pending = Completer<PaymentResult>();
      _razorpay!.open(<String, dynamic>{
        'key': AppConfig.razorpayKeyId,
        'amount': order['amountPaise'] ?? amountPaise,
        'currency': order['currency'] ?? 'INR',
        'name': 'Sproutly',
        'description': 'Campaign funding',
        'order_id': orderId,
        'prefill': order['prefill'] ?? <String, String>{},
        'theme': <String, String>{'color': '#3F4C06'},
        'retry': <String, dynamic>{'enabled': true, 'max_count': 3},
      });
      return await _pending!.future.timeout(
        const Duration(minutes: 10),
        onTimeout: () => const PaymentResult(
          PaymentStatus.cancelled,
          message: 'The payment window timed out.',
        ),
      );
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  /// International fallback: a Stripe Checkout session created server side and
  /// opened in the system browser. Card data never touches the app.
  Future<PaymentResult> payWithStripe({
    required String campaignId,
    required int amountPaise,
  }) async {
    if (!isStripeAvailable) {
      return const PaymentResult(
        PaymentStatus.unavailable,
        message: 'Stripe is not configured in this build.',
      );
    }
    try {
      final Map<String, dynamic> session = await _callables.call('createStripeCheckout', <String, dynamic>{
        'campaignId': campaignId,
        'amountPaise': amountPaise,
      });
      final String? url = session['url'] as String?;
      if (url == null) {
        throw const AppException(AppErrorCode.paymentFailed, 'Checkout could not be created.');
      }
      final bool opened = await launchUrl(Uri.parse(url), mode: LaunchMode.externalApplication);
      if (!opened) {
        return PaymentResult(PaymentStatus.failed, message: 'Could not open the browser.', redirectUrl: url);
      }
      // Verification happens through the Stripe webhook; the client polls the
      // campaign document for the credited budget.
      return PaymentResult(PaymentStatus.success, redirectUrl: url);
    } catch (error, stack) {
      throw mapError(error, stack);
    }
  }

  void _onSuccess(PaymentSuccessResponse response) {
    _complete(PaymentResult(
      PaymentStatus.success,
      paymentId: response.paymentId,
      message: 'Payment submitted. Your campaign budget updates once the bank confirms.',
    ));
  }

  void _onError(PaymentFailureResponse response) {
    final String message = response.message ?? 'The payment did not go through.';
    final bool cancelled = message.toLowerCase().contains('cancel');
    _complete(PaymentResult(
      cancelled ? PaymentStatus.cancelled : PaymentStatus.failed,
      message: message,
    ));
  }

  void _onExternalWallet(ExternalWalletResponse response) {
    if (kDebugMode) debugPrint('Sproutly: external wallet ${response.walletName}');
  }

  void _complete(PaymentResult result) {
    if (_pending != null && !_pending!.isCompleted) _pending!.complete(result);
    _pending = null;
  }

  void dispose() {
    _razorpay?.clear();
    _razorpay = null;
  }
}
