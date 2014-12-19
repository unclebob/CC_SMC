#include <stdio.h>
#include <strings.h>
#include <string.h>
#include <stdlib.h>
#include "twoCoinTurnstile.h"
#include "turnstileActions.h"

static struct TwoCoinTurnstile* fsm;
static char output[80];
static int position;
static int error_count = 0;

void lock() {
	output[position++] = 'L';
}

void unlock() {
	output[position++] = 'U';
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

void unexpected(char* state, char* event) {
	char error[80];
	bzero(error, 80);
	sprintf(error, "X(%s,%s)", state, event);
	strcpy(output+position, error);
	position += strlen(error);
}

static void setup() {
	bzero(output, 80);
	position = 0;
	
	struct TurnstileActions *actions = malloc(sizeof(struct TurnstileActions));
	actions->lock = lock;
	actions->unlock = unlock;
	actions->alarmOn = alarmOn;
	actions->alarmOff = alarmOff;
	actions->thankyou = thankyou;
	actions->unexpected_transition = unexpected;
	
	fsm = make_TwoCoinTurnstile(actions);
}

void check(char* function, char* expected) {
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
	TwoCoinTurnstile_Coin(fsm);
	TwoCoinTurnstile_Coin(fsm);
	TwoCoinTurnstile_Pass(fsm);
	check("testNormalBehavior", "UL");
}

static void testAlarm() {
	setup();
	TwoCoinTurnstile_Pass(fsm);
	check("testAlarm", "A");
}

static void testThankyou() {
	setup();
	TwoCoinTurnstile_Coin(fsm);
	TwoCoinTurnstile_Coin(fsm);
	TwoCoinTurnstile_Coin(fsm);
	check("testThankyou", "UT");
}

static void testNormalManyThanksAndAlarm() {
	setup();
	TwoCoinTurnstile_Coin(fsm);
	TwoCoinTurnstile_Coin(fsm);
	TwoCoinTurnstile_Pass(fsm);
	TwoCoinTurnstile_Coin(fsm);
	TwoCoinTurnstile_Coin(fsm);
	TwoCoinTurnstile_Coin(fsm);
	TwoCoinTurnstile_Pass(fsm);
	TwoCoinTurnstile_Pass(fsm);
	check("testNormalManyThanksAndAlarm", "ULUTLA");
}


static void testUndefined() {
	setup();
	TwoCoinTurnstile_Pass(fsm);
	TwoCoinTurnstile_Pass(fsm);
	check("testUndefined", "AX(Alarming,Pass)");
}

static void testReset() {
	setup();
	TwoCoinTurnstile_Pass(fsm);
	TwoCoinTurnstile_Reset(fsm);
	check("testReset", "AOL");
}

int main(int ac, char** av) {
	printf("Turnstile test begins\n");
	testNormalBehavior();
	testAlarm();
	testThankyou();
	testNormalManyThanksAndAlarm();
	testUndefined();
	testReset();
	if (error_count)
		printf("\nTests Complete with %d errors.\n", error_count);
	else
		printf("\nOK\n");
}

