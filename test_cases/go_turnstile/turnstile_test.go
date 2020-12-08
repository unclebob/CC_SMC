package twocointurnstile

import (
	"fmt"
	"testing"
)

var _ TurnstileActions = &MyTwoCoinTurnstile{}

// MyTwoCoinTurnstile implements the TurnstileActions interface.
type MyTwoCoinTurnstile struct {
	output   string
	position int
	t        *testing.T
}

func (m *MyTwoCoinTurnstile) Lock() {
	m.output += "L"
	m.position++
}

func (m *MyTwoCoinTurnstile) Unlock() {
	m.output += "U"
	m.position++
}

func (m *MyTwoCoinTurnstile) Thankyou() {
	m.output += "T"
	m.position++
}

func (m *MyTwoCoinTurnstile) AlarmOn() {
	m.output += "A"
	m.position++
}

func (m *MyTwoCoinTurnstile) AlarmOff() {
	m.output += "O"
	m.position++
}

func (m *MyTwoCoinTurnstile) UnexpectedTransition(state, event string) {
	e := fmt.Sprintf("X(%v,%v)", state, event)
	m.output += e
	m.position += len(e)
}

func (m *MyTwoCoinTurnstile) check(function, want string) {
	m.t.Helper()

	if m.output != want {
		m.t.Errorf("%s failed: got %v, want %v", function, m.output, want)
	}
}

func setup(t *testing.T) (*TwoCoinTurnstile, *MyTwoCoinTurnstile) {
	m := &MyTwoCoinTurnstile{t: t}
	fsm := New(m)
	return fsm, m
}

func TestNormalBehavior(t *testing.T) {
	fsm, m := setup(t)
	fsm.Coin()
	fsm.Coin()
	fsm.Pass()
	m.check("testNormalBehavior", "UL")
}

func TestAlarm(t *testing.T) {
	fsm, m := setup(t)
	fsm.Pass()
	m.check("testAlarm", "A")
}

func TestThankyou(t *testing.T) {
	fsm, m := setup(t)
	fsm.Coin()
	fsm.Coin()
	fsm.Coin()
	m.check("testThankyou", "UT")
}

func TestNormalManyThanksAndAlarm(t *testing.T) {
	fsm, m := setup(t)
	fsm.Coin()
	fsm.Coin()
	fsm.Pass()
	fsm.Coin()
	fsm.Coin()
	fsm.Coin()
	fsm.Pass()
	fsm.Pass()
	m.check("testNormalManyThanksAndAlarm", "ULUTLA")
}

func TestUndefined(t *testing.T) {
	fsm, m := setup(t)
	fsm.Pass()
	fsm.Pass()
	m.check("testUndefined", "AX(Alarming,Pass)")
}

func TestReset(t *testing.T) {
	fsm, m := setup(t)
	fsm.Pass()
	fsm.Reset()
	m.check("testReset", "AOL")
}
