#ifndef TURNSTILE_ACTIONS_H
#define TURNSTILE_ACTIONS_H

struct TurnstileActions {
	void (*lock)();
	void (*unlock)();
	void (*thankyou)();
	void (*alarmOn)();
	void (*alarmOff)();
	void (*unexpected_transition)(char* state, char* event);
};

#endif

