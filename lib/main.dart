import 'app/bootstrap.dart';

void main() {
  // Any synchronous throw inside bootstrap is a configuration error (most often
  // a missing firebase_options.dart) and surfaces with its own message.
  bootstrap();
}
