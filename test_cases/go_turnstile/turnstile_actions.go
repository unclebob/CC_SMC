package twocointurnstile

// TurnstileActions represents the possible actions that can be performed.
type TurnstileActions interface {
	Lock()
	Unlock()
	AlarmOn()
	AlarmOff()
	Thankyou()
	UnexpectedTransition(state, event string)
}
