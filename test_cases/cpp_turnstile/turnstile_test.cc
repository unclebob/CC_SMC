#include <stdio.h>
#include <strings.h>
#include <string.h>
#include <stdlib.h>

#include "TurnstileActions.h"
#include "TwoCoinTurnstile.h"

static char output[80];
static int position;
static int error_count = 0;

class MyTwoCoinTurnstile : public TwoCoinTurnstile {
public:		
	virtual void lock() {
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

	void unexpected_transition (const char* state, const char* event) {
		char error[80];
		bzero(error, 80);
		sprintf(error, "X(%s,%s)", state, event);
		strcpy(output+position, error);
		position += strlen(error);
	}
};

static MyTwoCoinTurnstile* fsm;

static void setup() {
	bzero(output, 80);
	position = 0;
		
	fsm = new MyTwoCoinTurnstile();
}

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

