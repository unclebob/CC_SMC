#ifndef TURNSTILEACTIONS_H
#define TURNSTILEACTIONS_H

class TurnstileActions {
public:
	virtual void lock() = 0;
	virtual void unlock() = 0;
	virtual void alarmOn() = 0;
	virtual void alarmOff() = 0;
	virtual void thankyou() = 0;
	virtual void unexpected_transition(const char* state, const char* event) = 0;	
};

#endif