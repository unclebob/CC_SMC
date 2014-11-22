package smcsrc.smc.parser;

/*
<FSM> ::= <header>* <logic>
<header> ::= "Actions:" <name> | "FSM:" <name> | "Initial:" <name>

<logic> ::= "{" <transition>* "}"

<transition> ::= <state-spec> <subtransition>
             |   <state-spec> "{" <subtransition>* "}"

<subtransition>   ::= <event-spec> <next-state> <action-spec>
<action-spec>     ::= <action> | "{" <action>* "}" | "-"
<state-spec>      ::= <state> <state-modifiers>
<state-modifiers> ::= "" | <state-modifier> | <state-modifier> <state-modifiers>
<state-modifier>  ::= ":" <state>
                  |   "<" <action-spec>
                  |   ">" <action-spec>

<next-state> ::= <state> | "-"
<event-spec> :: <event> | "-"
<action> ::= <name>
<state> ::= <name>
<event> ::= <name>
*/

import smcsrc.smc.lexer.TokenCollector;

import java.util.function.Consumer;

public class Parser implements TokenCollector {
  private ParserState state = ParserState.HEADER;
  private Builder builder;

  public Parser(Builder builder) {
    this.builder = builder;
  }

  public void openBrace(int line, int pos) {
    handleEvent(ParserEvent.OPEN_BRACE, line, pos);
  }

  public void closedBrace(int line, int pos) {
    handleEvent(ParserEvent.CLOSED_BRACE, line, pos);
  }

  public void openParen(int line, int pos) {
    handleEvent(ParserEvent.OPEN_PAREN, line, pos);
  }

  public void closedParen(int line, int pos) {
    handleEvent(ParserEvent.CLOSED_PAREN, line, pos);
  }

  public void openAngle(int line, int pos) {
    handleEvent(ParserEvent.OPEN_ANGLE, line, pos);
  }

  public void closedAngle(int line, int pos) {
    handleEvent(ParserEvent.CLOSED_ANGLE, line, pos);
  }

  public void dash(int line, int pos) {
    handleEvent(ParserEvent.DASH, line, pos);
  }

  public void colon(int line, int pos) {
    handleEvent(ParserEvent.COLON, line, pos);
  }

  public void name(String name, int line, int pos) {
    builder.setName(name);
    handleEvent(ParserEvent.NAME, line, pos);
  }

  public void error(int line, int pos) {
    builder.syntaxError(line, pos);
  }

  class Transition {
    Transition(ParserState currentState, ParserEvent event,
               ParserState newState, Consumer<Builder> action) {
      this.currentState = currentState;
      this.event = event;
      this.newState = newState;
      this.action = action;
    }

    public ParserState currentState;
    public ParserEvent event;
    public ParserState newState;
    public Consumer<Builder> action;
  }

  Transition[] transitions = new Transition[]{
    new Transition(ParserState.HEADER, ParserEvent.NAME, ParserState.HEADER_COLON, t -> t.newHeaderWithName()),
    new Transition(ParserState.HEADER, ParserEvent.OPEN_BRACE, ParserState.STATE_SPEC, null),
    new Transition(ParserState.HEADER_COLON, ParserEvent.COLON, ParserState.HEADER_VALUE, null),
    new Transition(ParserState.HEADER_VALUE, ParserEvent.NAME, ParserState.HEADER, t -> t.addHeaderWithValue()),
    new Transition(ParserState.STATE_SPEC, ParserEvent.OPEN_PAREN, ParserState.SUPER_STATE_NAME, null),
    new Transition(ParserState.STATE_SPEC, ParserEvent.NAME, ParserState.STATE_MODIFIER, t -> t.setStateName()),
    new Transition(ParserState.STATE_SPEC, ParserEvent.CLOSED_BRACE, ParserState.END, t -> t.done()),
    new Transition(ParserState.SUPER_STATE_NAME, ParserEvent.NAME, ParserState.SUPER_STATE_CLOSE, t -> t.setSuperStateName()),
    new Transition(ParserState.SUPER_STATE_CLOSE, ParserEvent.CLOSED_PAREN, ParserState.STATE_MODIFIER, null),
    new Transition(ParserState.STATE_MODIFIER, ParserEvent.OPEN_ANGLE, ParserState.ENTRY_ACTION, null),
    new Transition(ParserState.STATE_MODIFIER, ParserEvent.CLOSED_ANGLE, ParserState.EXIT_ACTION, null),
    new Transition(ParserState.STATE_MODIFIER, ParserEvent.COLON, ParserState.STATE_BASE, null),
    new Transition(ParserState.STATE_MODIFIER, ParserEvent.NAME, ParserState.SINGLE_EVENT, t -> t.setEvent()),
    new Transition(ParserState.STATE_MODIFIER, ParserEvent.DASH, ParserState.SINGLE_EVENT, t -> t.setNullEvent()),
    new Transition(ParserState.STATE_MODIFIER, ParserEvent.OPEN_BRACE, ParserState.SUBTRANSITION_GROUP, null),
    new Transition(ParserState.ENTRY_ACTION, ParserEvent.NAME, ParserState.STATE_MODIFIER, t -> t.setEntryAction()),
    new Transition(ParserState.ENTRY_ACTION, ParserEvent.OPEN_BRACE, ParserState.MULTIPLE_ENTRY_ACTIONS, null),
    new Transition(ParserState.MULTIPLE_ENTRY_ACTIONS, ParserEvent.NAME, ParserState.MULTIPLE_ENTRY_ACTIONS, t -> t.setEntryAction()),
    new Transition(ParserState.MULTIPLE_ENTRY_ACTIONS, ParserEvent.CLOSED_BRACE, ParserState.STATE_MODIFIER, null),
    new Transition(ParserState.EXIT_ACTION, ParserEvent.NAME, ParserState.STATE_MODIFIER, t -> t.setExitAction()),
    new Transition(ParserState.EXIT_ACTION, ParserEvent.OPEN_BRACE, ParserState.MULTIPLE_EXIT_ACTIONS, null),
    new Transition(ParserState.MULTIPLE_EXIT_ACTIONS, ParserEvent.NAME, ParserState.MULTIPLE_EXIT_ACTIONS, t -> t.setExitAction()),
    new Transition(ParserState.MULTIPLE_EXIT_ACTIONS, ParserEvent.CLOSED_BRACE, ParserState.STATE_MODIFIER, null),
    new Transition(ParserState.STATE_BASE, ParserEvent.NAME, ParserState.STATE_MODIFIER, t -> t.setStateBase()),
    new Transition(ParserState.SINGLE_EVENT, ParserEvent.NAME, ParserState.SINGLE_NEXT_STATE, t -> t.setNextState()),
    new Transition(ParserState.SINGLE_EVENT, ParserEvent.DASH, ParserState.SINGLE_NEXT_STATE, t -> t.setNullNextState()),
    new Transition(ParserState.SINGLE_NEXT_STATE, ParserEvent.NAME, ParserState.STATE_SPEC, t -> t.transitionWithAction()),
    new Transition(ParserState.SINGLE_NEXT_STATE, ParserEvent.DASH, ParserState.STATE_SPEC, t -> t.transitionNullAction()),
    new Transition(ParserState.SINGLE_NEXT_STATE, ParserEvent.OPEN_BRACE, ParserState.SINGLE_ACTION_GROUP, null),
    new Transition(ParserState.SINGLE_ACTION_GROUP, ParserEvent.NAME, ParserState.SINGLE_ACTION_GROUP_NAME, t -> t.addAction()),
    new Transition(ParserState.SINGLE_ACTION_GROUP, ParserEvent.CLOSED_BRACE, ParserState.STATE_SPEC, t -> t.transitionNullAction()),
    new Transition(ParserState.SINGLE_ACTION_GROUP_NAME, ParserEvent.NAME, ParserState.SINGLE_ACTION_GROUP_NAME, t -> t.addAction()),
    new Transition(ParserState.SINGLE_ACTION_GROUP_NAME, ParserEvent.CLOSED_BRACE, ParserState.STATE_SPEC, t -> t.transitionWithActions()),
    new Transition(ParserState.SUBTRANSITION_GROUP, ParserEvent.CLOSED_BRACE, ParserState.STATE_SPEC, null),
    new Transition(ParserState.SUBTRANSITION_GROUP, ParserEvent.NAME, ParserState.GROUP_EVENT, t -> t.setEvent()),
    new Transition(ParserState.SUBTRANSITION_GROUP, ParserEvent.DASH, ParserState.GROUP_EVENT, t -> t.setNullEvent()),
    new Transition(ParserState.GROUP_EVENT, ParserEvent.NAME, ParserState.GROUP_NEXT_STATE, t -> t.setNextState()),
    new Transition(ParserState.GROUP_EVENT, ParserEvent.DASH, ParserState.GROUP_NEXT_STATE, t -> t.setNullNextState()),
    new Transition(ParserState.GROUP_NEXT_STATE, ParserEvent.NAME, ParserState.SUBTRANSITION_GROUP, t -> t.transitionWithAction()),
    new Transition(ParserState.GROUP_NEXT_STATE, ParserEvent.DASH, ParserState.SUBTRANSITION_GROUP, t -> t.transitionNullAction()),
    new Transition(ParserState.GROUP_NEXT_STATE, ParserEvent.OPEN_BRACE, ParserState.GROUP_ACTION_GROUP, null),
    new Transition(ParserState.GROUP_ACTION_GROUP, ParserEvent.NAME, ParserState.GROUP_ACTION_GROUP_NAME, t -> t.addAction()),
    new Transition(ParserState.GROUP_ACTION_GROUP, ParserEvent.CLOSED_BRACE, ParserState.SUBTRANSITION_GROUP, t -> t.transitionNullAction()),
    new Transition(ParserState.GROUP_ACTION_GROUP_NAME, ParserEvent.NAME, ParserState.GROUP_ACTION_GROUP_NAME, t -> t.addAction()),
    new Transition(ParserState.GROUP_ACTION_GROUP_NAME, ParserEvent.CLOSED_BRACE, ParserState.SUBTRANSITION_GROUP, t -> t.transitionWithActions()),
    new Transition(ParserState.END, ParserEvent.EOF, ParserState.END, null)
  };

  public void handleEvent(ParserEvent event, int line, int pos) {
    for (Transition t : transitions) {
      if (t.currentState == state && t.event == event) {
        state = t.newState;
        if (t.action != null)
          t.action.accept(builder);
        return;
      }
    }
    handleEventError(event, line, pos);
  }

  private void handleEventError(ParserEvent event, int line, int pos) {
    switch (state) {
      case HEADER:
      case HEADER_COLON:
      case HEADER_VALUE:
        builder.headerError(state, event, line, pos);
        break;

      case STATE_SPEC:
      case SUPER_STATE_NAME:
      case SUPER_STATE_CLOSE:
      case STATE_MODIFIER:
      case EXIT_ACTION:
      case ENTRY_ACTION:
      case STATE_BASE:
        builder.stateSpecError(state, event, line, pos);
        break;

      case SINGLE_EVENT:
      case SINGLE_NEXT_STATE:
      case SINGLE_ACTION_GROUP:
      case SINGLE_ACTION_GROUP_NAME:
        builder.transitionError(state, event, line, pos);
        break;

      case SUBTRANSITION_GROUP:
      case GROUP_EVENT:
      case GROUP_NEXT_STATE:
      case GROUP_ACTION_GROUP:
      case GROUP_ACTION_GROUP_NAME:
        builder.transitionGroupError(state, event, line, pos);
        break;

      case END:
        builder.endError(state, event, line, pos);
        break;
    }
  }
}
