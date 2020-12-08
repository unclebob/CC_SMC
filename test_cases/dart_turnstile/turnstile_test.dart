// -*- compile-command: "pub run test turnstile_test.dart"; -*-

import 'package:test/test.dart';

import 'TwoCoinTurnstile.dart';

class MyTwoCoinTurnstile implements TwoCoinTurnstile {
  String output;
  MyTwoCoinTurnstile({this.output = ""});
  
  void lock() {
		output += 'L';
    position++;
	}

	void unlock() {
		output += 'U';
	}

	void thankyou() {
		output[position++] = 'T';
	}

	void alarmOn() {
		output[position++] = 'A';
	}

	void alarmOff() {
		output[position++] = 'O';
	}

	void unexpected_transition(final String state, final String event) {
		char error[80];
		bzero(error, 80);
		sprintf(error, "X(%s,%s)", state, event);
		strcpy(output+position, error);
		position += strlen(error);
	}
}

main() {
  MyTwoCoinTurnstile fsm;
  
  setUp(() {
      fsm = MyTwoCoinTurnstile();
  });

  void check(const char* function, const char* expected) {
	  int result = strcmp(output, expected);
	  if (result) {
		  printf("\n%s failed.  expected: %s, but was: %s", function, expected, output);
		  error_count++;
	  }
	  else
		printf(".");	
  }

  static void testNormalBehavior() {
	  setup();
	  fsm->Coin();
	  fsm->Coin();
	  fsm->Pass();
	  check("testNormalBehavior", "UL");
  }

  static void testAlarm() {
	  setup();
	  fsm->Pass();
	  check("testAlarm", "A");
  }

  static void testThankyou() {
	  setup();
	  fsm->Coin();
	  fsm->Coin();
	  fsm->Coin();
	  check("testThankyou", "UT");
  }

  static void testNormalManyThanksAndAlarm() {
	  setup();
	  fsm->Coin();
	  fsm->Coin();
	  fsm->Pass();
	  fsm->Coin();
	  fsm->Coin();
	  fsm->Coin();
	  fsm->Pass();
	  fsm->Pass();
	  check("testNormalManyThanksAndAlarm", "ULUTLA");
  }


  static void testUndefined() {
	  setup();
	  fsm->Pass();
	  fsm->Pass();
	  check("testUndefined", "AX(Alarming,Pass)");
  }

  static void testReset() {
	  setup();
	  fsm->Pass();
	  fsm->Reset();
	  check("testReset", "AOL");
  }
}
