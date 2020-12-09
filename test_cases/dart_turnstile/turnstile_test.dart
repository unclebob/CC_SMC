// -*- compile-command: "pub run test turnstile_test.dart"; -*-

import 'package:test/test.dart';

import 'TurnstileActions.dart';
import 'TwoCoinTurnstile.dart';

class MyTwoCoinTurnstile extends TwoCoinTurnstile implements TurnstileActions {
  String output;
  MyTwoCoinTurnstile({this.output = ''});

  lock() {
    output += 'L';
  }

  unlock() {
    output += 'U';
  }

  thankyou() {
    output += 'T';
  }

  alarmOn() {
    output += 'A';
  }

  alarmOff() {
    output += 'O';
  }

  unexpected_transition(final String state, final String event) {
    output += 'X($state,$event)';
  }
}

main() {
  MyTwoCoinTurnstile fsm;

  setUp(() {
    fsm = MyTwoCoinTurnstile();
  });

  test('NormalBehavior', () {
    fsm.Coin();
    fsm.Coin();
    fsm.Pass();
    expect(fsm.output, equals('UL'));
  });

  test('Alarm', () {
    fsm.Pass();
    expect(fsm.output, equals('A'));
  });

  test('Thankyou', () {
    fsm.Coin();
    fsm.Coin();
    fsm.Coin();
    expect(fsm.output, equals('UT'));
  });

  test('NormalManyThanksAndAlarm', () {
    fsm.Coin();
    fsm.Coin();
    fsm.Pass();
    fsm.Coin();
    fsm.Coin();
    fsm.Coin();
    fsm.Pass();
    fsm.Pass();
    expect(fsm.output, equals('ULUTLA'));
  });

  test('Undefined', () {
    fsm.Pass();
    fsm.Pass();
    expect(fsm.output, equals('AX(Alarming,Pass)'));
  });

  test('Reset', () {
    fsm.Pass();
    fsm.Reset();
    expect(fsm.output, equals('AOL'));
  });
}
