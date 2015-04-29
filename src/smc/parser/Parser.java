package smc.parser;

//@formatter:off
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
*/ //@formatter:on

import static smc.parser.ParserEvent.CLOSED_ANGLE;
import static smc.parser.ParserEvent.CLOSED_BRACE;
import static smc.parser.ParserEvent.CLOSED_PAREN;
import static smc.parser.ParserEvent.COLON;
import static smc.parser.ParserEvent.DASH;
import static smc.parser.ParserEvent.NAME;
import static smc.parser.ParserEvent.OPEN_ANGLE;
import static smc.parser.ParserEvent.OPEN_BRACE;
import static smc.parser.ParserEvent.OPEN_PAREN;
import static smc.parser.ParserState.END;
import static smc.parser.ParserState.ENTRY_ACTION;
import static smc.parser.ParserState.EXIT_ACTION;
import static smc.parser.ParserState.GROUP_ACTION_GROUP;
import static smc.parser.ParserState.GROUP_ACTION_GROUP_NAME;
import static smc.parser.ParserState.GROUP_EVENT;
import static smc.parser.ParserState.GROUP_NEXT_STATE;
import static smc.parser.ParserState.HEADER;
import static smc.parser.ParserState.HEADER_COLON;
import static smc.parser.ParserState.HEADER_VALUE;
import static smc.parser.ParserState.MULTIPLE_ENTRY_ACTIONS;
import static smc.parser.ParserState.MULTIPLE_EXIT_ACTIONS;
import static smc.parser.ParserState.SINGLE_ACTION_GROUP;
import static smc.parser.ParserState.SINGLE_ACTION_GROUP_NAME;
import static smc.parser.ParserState.SINGLE_EVENT;
import static smc.parser.ParserState.SINGLE_NEXT_STATE;
import static smc.parser.ParserState.STATE_BASE;
import static smc.parser.ParserState.STATE_MODIFIER;
import static smc.parser.ParserState.STATE_SPEC;
import static smc.parser.ParserState.SUBTRANSITION_GROUP;
import static smc.parser.ParserState.SUPER_STATE_CLOSE;
import static smc.parser.ParserState.SUPER_STATE_NAME;

import java.util.function.Consumer;

import smc.lexer.TokenCollector;

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
    Transition(ParserState currentState, ParserEvent event, ParserState newState,
        Consumer<Builder> action) {
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

  Transition[] transitions = new Transition[] {
      row(HEADER, NAME, HEADER_COLON, t -> t.newHeaderWithName()),
      row(HEADER, OPEN_BRACE, STATE_SPEC, null),
      row(HEADER_COLON, COLON, HEADER_VALUE, null),
      row(HEADER_VALUE, NAME, HEADER, t -> t.addHeaderWithValue()),
      row(STATE_SPEC, OPEN_PAREN, SUPER_STATE_NAME, null),
      row(STATE_SPEC, NAME, STATE_MODIFIER, t -> t.setStateName()),
      row(STATE_SPEC, CLOSED_BRACE, END, t -> t.done()),
      row(SUPER_STATE_NAME, NAME, SUPER_STATE_CLOSE, t -> t.setSuperStateName()),
      row(SUPER_STATE_CLOSE, CLOSED_PAREN, STATE_MODIFIER, null),
      row(STATE_MODIFIER, OPEN_ANGLE, ENTRY_ACTION, null),
      row(STATE_MODIFIER, CLOSED_ANGLE, EXIT_ACTION, null),
      row(STATE_MODIFIER, COLON, STATE_BASE, null),
      row(STATE_MODIFIER, NAME, SINGLE_EVENT, t -> t.setEvent()),
      row(STATE_MODIFIER, DASH, SINGLE_EVENT, t -> t.setNullEvent()),
      row(STATE_MODIFIER, OPEN_BRACE, SUBTRANSITION_GROUP, null),
      row(ENTRY_ACTION, NAME, STATE_MODIFIER, t -> t.setEntryAction()),
      row(ENTRY_ACTION, OPEN_BRACE, MULTIPLE_ENTRY_ACTIONS, null),
      row(MULTIPLE_ENTRY_ACTIONS, NAME, MULTIPLE_ENTRY_ACTIONS, t -> t.setEntryAction()),
      row(MULTIPLE_ENTRY_ACTIONS, CLOSED_BRACE, STATE_MODIFIER, null),
      row(EXIT_ACTION, NAME, STATE_MODIFIER, t -> t.setExitAction()),
      row(EXIT_ACTION, OPEN_BRACE, MULTIPLE_EXIT_ACTIONS, null),
      row(MULTIPLE_EXIT_ACTIONS, NAME, MULTIPLE_EXIT_ACTIONS, t -> t.setExitAction()),
      row(MULTIPLE_EXIT_ACTIONS, CLOSED_BRACE, STATE_MODIFIER, null),
      row(STATE_BASE, NAME, STATE_MODIFIER, t -> t.setStateBase()),
      row(SINGLE_EVENT, NAME, SINGLE_NEXT_STATE, t -> t.setNextState()),
      row(SINGLE_EVENT, DASH, SINGLE_NEXT_STATE, t -> t.setNullNextState()),
      row(SINGLE_NEXT_STATE, NAME, STATE_SPEC, t -> t.transitionWithAction()),
      row(SINGLE_NEXT_STATE, DASH, STATE_SPEC, t -> t.transitionNullAction()),
      row(SINGLE_NEXT_STATE, OPEN_BRACE, SINGLE_ACTION_GROUP, null),
      row(SINGLE_ACTION_GROUP, NAME, SINGLE_ACTION_GROUP_NAME, t -> t.addAction()),
      row(SINGLE_ACTION_GROUP, CLOSED_BRACE, STATE_SPEC, t -> t.transitionNullAction()),
      row(SINGLE_ACTION_GROUP_NAME, NAME, SINGLE_ACTION_GROUP_NAME, t -> t.addAction()),
      row(SINGLE_ACTION_GROUP_NAME, CLOSED_BRACE, STATE_SPEC, t -> t.transitionWithActions()),
      row(SUBTRANSITION_GROUP, CLOSED_BRACE, STATE_SPEC, null),
      row(SUBTRANSITION_GROUP, NAME, GROUP_EVENT, t -> t.setEvent()),
      row(SUBTRANSITION_GROUP, DASH, GROUP_EVENT, t -> t.setNullEvent()),
      row(GROUP_EVENT, NAME, GROUP_NEXT_STATE, t -> t.setNextState()),
      row(GROUP_EVENT, DASH, GROUP_NEXT_STATE, t -> t.setNullNextState()),
      row(GROUP_NEXT_STATE, NAME, SUBTRANSITION_GROUP, t -> t.transitionWithAction()),
      row(GROUP_NEXT_STATE, DASH, SUBTRANSITION_GROUP, t -> t.transitionNullAction()),
      row(GROUP_NEXT_STATE, OPEN_BRACE, GROUP_ACTION_GROUP, null),
      row(GROUP_ACTION_GROUP, NAME, GROUP_ACTION_GROUP_NAME, t -> t.addAction()),
      row(GROUP_ACTION_GROUP, CLOSED_BRACE, SUBTRANSITION_GROUP, t -> t.transitionNullAction()),
      row(GROUP_ACTION_GROUP_NAME, NAME, GROUP_ACTION_GROUP_NAME, t -> t.addAction()),
      row(GROUP_ACTION_GROUP_NAME, CLOSED_BRACE, SUBTRANSITION_GROUP, t -> t.transitionWithActions()),
      row(END, ParserEvent.EOF, END, null) 
  };

  private Transition row(ParserState current, ParserEvent event, ParserState next,
      Consumer<Builder> action) {
    return new Transition(current, event, next, action);
  }

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
    default:
      break;
    }
  }
}
