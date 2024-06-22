public abstract class TwoCoinTurnstile implements TurnstileActions {
    public abstract void unhandledTransition(String state, String event);

    private enum State {Alarming, FirstCoin, Locked, Unlocked}

    private enum Event {Coin, Pass, Reset}

    private State state = State.Locked;

    private void setState(State s) {
        state = s;
    }

    public void Coin() {
        handleEvent(Event.Coin);
    }

    public void Pass() {
        handleEvent(Event.Pass);
    }

    public void Reset() {
        handleEvent(Event.Reset);
    }

    private void handleEvent(Event event) {
        switch (state) {
            case Alarming:
                switch (event) {
                    case Reset:
                        setState(State.Locked);
                        alarmOff();
                        lock();
                        break;
                    default:
                        unhandledTransition(state.name(), event.name());
                        break;
                }
                break;
            case FirstCoin:
                switch (event) {
                    case Pass:
                        setState(State.Alarming);
                        alarmOn();
                        break;
                    case Coin:
                        setState(State.Unlocked);
                        unlock();
                        break;
                    case Reset:
                        setState(State.Locked);
                        lock();
                        break;
                    default:
                        unhandledTransition(state.name(), event.name());
                        break;
                }
                break;
            case Locked:
                switch (event) {
                    case Pass:
                        setState(State.Alarming);
                        alarmOn();
                        break;
                    case Coin:
                        setState(State.FirstCoin);
                        break;
                    case Reset:
                        setState(State.Locked);
                        lock();
                        break;
                    default:
                        unhandledTransition(state.name(), event.name());
                        break;
                }
                break;
            case Unlocked:
                switch (event) {
                    case Pass, Reset:
                        setState(State.Locked);
                        lock();
                        break;
                    case Coin:
                        setState(State.Unlocked);
                        thankyou();
                        break;
                    default:
                        unhandledTransition(state.name(), event.name());
                        break;
                }
                break;
        }
    }
}
