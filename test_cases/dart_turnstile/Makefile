all : clean TwoCoinTurnstile.dart turnstile_test.dart
	pub get
	pub run test turnstile_test.dart

clean :
	rm -f TwoCoinTurnstile.dart

TwoCoinTurnstile.dart : twoCoinTurnstile.sm
	java -jar ../../build/jar/smc.jar -l Dart $?
	dartfmt -w *.dart
