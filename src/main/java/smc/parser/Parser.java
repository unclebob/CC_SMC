package smc.parser;

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

import smc.lexer.TokenCollector;

import java.util.function.Consumer;

import static smc.parser.ParserEvent.*;
import static smc.parser.ParserState.*;

public class Parser implements TokenCollector {
  private ParserState state = HEADER;
  private Builder builder;

  public Parser(Builder builder) {
    this.builder = builder;
  }

  public void openBrace(int line, int pos) {
    handleEvent(OPEN_BRACE, line, pos);
  }

  public void closedBrace(int line, int pos) {
    handleEvent(CLOSED_BRACE, line, pos);
  }

  public void openParen(int line, int pos) {
    handleEvent(OPEN_PAREN, line, pos);
  }

  public void closedParen(int line, int pos) {
    handleEvent(CLOSED_PAREN, line, pos);
  }

  public void openAngle(int line, int pos) {
    handleEvent(OPEN_ANGLE, line, pos);
  }

  public void closedAngle(int line, int pos) {
    handleEvent(CLOSED_ANGLE, line, pos);
  }

  public void dash(int line, int pos) {
    handleEvent(DASH, line, pos);
  }

  public void colon(int line, int pos) {
    handleEvent(COLON, line, pos);
  }

  public void name(String name, int line, int pos) {
    builder.setName(name);
    handleEvent(NAME, line, pos);
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
    new Transition(HEADER, NAME, HEADER_COLON, t -> t.newHeaderWithName()),
    new Transition(HEADER, OPEN_BRACE, STATE_SPEC, null),
    new Transition(HEADER_COLON, COLON, HEADER_VALUE, null),
    new Transition(HEADER_VALUE, NAME, HEADER, t -> t.addHeaderWithValue()),
    new Transition(STATE_SPEC, OPEN_PAREN, SUPER_STATE_NAME, null),
    new Transition(STATE_SPEC, NAME, STATE_MODIFIER, t -> t.setStateName()),
    new Transition(STATE_SPEC, CLOSED_BRACE, END, t -> t.done()),
    new Transition(SUPER_STATE_NAME, NAME, SUPER_STATE_CLOSE, t -> t.setSuperStateName()),
    new Transition(SUPER_STATE_CLOSE, CLOSED_PAREN, STATE_MODIFIER, null),
    new Transition(STATE_MODIFIER, OPEN_ANGLE, ENTRY_ACTION, null),
    new Transition(STATE_MODIFIER, CLOSED_ANGLE, EXIT_ACTION, null),
    new Transition(STATE_MODIFIER, COLON, STATE_BASE, null),
    new Transition(STATE_MODIFIER, NAME, SINGLE_EVENT, t -> t.setEvent()),
    new Transition(STATE_MODIFIER, DASH, SINGLE_EVENT, t -> t.setNullEvent()),
    new Transition(STATE_MODIFIER, OPEN_BRACE, SUBTRANSITION_GROUP, null),
    new Transition(ENTRY_ACTION, NAME, STATE_MODIFIER, t -> t.setEntryAction()),
    new Transition(ENTRY_ACTION, OPEN_BRACE, MULTIPLE_ENTRY_ACTIONS, null),
    new Transition(MULTIPLE_ENTRY_ACTIONS, NAME, MULTIPLE_ENTRY_ACTIONS, t -> t.setEntryAction()),
    new Transition(MULTIPLE_ENTRY_ACTIONS, CLOSED_BRACE, STATE_MODIFIER, null),
    new Transition(EXIT_ACTION, NAME, STATE_MODIFIER, t -> t.setExitAction()),
    new Transition(EXIT_ACTION, OPEN_BRACE, MULTIPLE_EXIT_ACTIONS, null),
    new Transition(MULTIPLE_EXIT_ACTIONS, NAME, MULTIPLE_EXIT_ACTIONS, t -> t.setExitAction()),
    new Transition(MULTIPLE_EXIT_ACTIONS, CLOSED_BRACE, STATE_MODIFIER, null),
    new Transition(STATE_BASE, NAME, STATE_MODIFIER, t -> t.setStateBase()),
    new Transition(SINGLE_EVENT, NAME, SINGLE_NEXT_STATE, t -> t.setNextState()),
    new Transition(SINGLE_EVENT, DASH, SINGLE_NEXT_STATE, t -> t.setNullNextState()),
    new Transition(SINGLE_NEXT_STATE, NAME, STATE_SPEC, t -> t.transitionWithAction()),
    new Transition(SINGLE_NEXT_STATE, DASH, STATE_SPEC, t -> t.transitionNullAction()),
    new Transition(SINGLE_NEXT_STATE, OPEN_BRACE, SINGLE_ACTION_GROUP, null),
    new Transition(SINGLE_ACTION_GROUP, NAME, SINGLE_ACTION_GROUP_NAME, t -> t.addAction()),
    new Transition(SINGLE_ACTION_GROUP, CLOSED_BRACE, STATE_SPEC, t -> t.transitionNullAction()),
    new Transition(SINGLE_ACTION_GROUP_NAME, NAME, SINGLE_ACTION_GROUP_NAME, t -> t.addAction()),
    new Transition(SINGLE_ACTION_GROUP_NAME, CLOSED_BRACE, STATE_SPEC, t -> t.transitionWithActions()),
    new Transition(SUBTRANSITION_GROUP, CLOSED_BRACE, STATE_SPEC, null),
    new Transition(SUBTRANSITION_GROUP, NAME, GROUP_EVENT, t -> t.setEvent()),
    new Transition(SUBTRANSITION_GROUP, DASH, GROUP_EVENT, t -> t.setNullEvent()),
    new Transition(GROUP_EVENT, NAME, GROUP_NEXT_STATE, t -> t.setNextState()),
    new Transition(GROUP_EVENT, DASH, GROUP_NEXT_STATE, t -> t.setNullNextState()),
    new Transition(GROUP_NEXT_STATE, NAME, SUBTRANSITION_GROUP, t -> t.transitionWithAction()),
    new Transition(GROUP_NEXT_STATE, DASH, SUBTRANSITION_GROUP, t -> t.transitionNullAction()),
    new Transition(GROUP_NEXT_STATE, OPEN_BRACE, GROUP_ACTION_GROUP, null),
    new Transition(GROUP_ACTION_GROUP, NAME, GROUP_ACTION_GROUP_NAME, t -> t.addAction()),
    new Transition(GROUP_ACTION_GROUP, CLOSED_BRACE, SUBTRANSITION_GROUP, t -> t.transitionNullAction()),
    new Transition(GROUP_ACTION_GROUP_NAME, NAME, GROUP_ACTION_GROUP_NAME, t -> t.addAction()),
    new Transition(GROUP_ACTION_GROUP_NAME, CLOSED_BRACE, SUBTRANSITION_GROUP, t -> t.transitionWithActions()),
    new Transition(END, ParserEvent.EOF, END, null)
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
