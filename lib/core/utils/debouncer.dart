import 'dart:async';

/// Collapses rapid calls into one, after [duration] of quiet.
class Debouncer {
  Debouncer({this.duration = const Duration(milliseconds: 320)});

  final Duration duration;
  Timer? _timer;

  void run(void Function() action) {
    _timer?.cancel();
    _timer = Timer(duration, action);
  }

  void cancel() {
    _timer?.cancel();
    _timer = null;
  }

  void dispose() => cancel();
}
