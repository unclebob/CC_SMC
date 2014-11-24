package smc.parser;

import static smc.parser.FsmSyntax.*;
import static smc.parser.FsmSyntax.SyntaxError.Type.*;

public class SyntaxBuilder implements Builder {
  private FsmSyntax fsm;
  private Header header;
  private String parsedName;
  private Transition transition;
  private SubTransition subtransition;

  public SyntaxBuilder() {
    fsm = new FsmSyntax();
  }

  public void newHeaderWithName() {
    header = new Header();
    header.name = parsedName;
  }

  public void addHeaderWithValue() {
    header.value = parsedName;
    fsm.headers.add(header);
  }

  public void setStateName() {
    transition = new Transition();
    fsm.logic.add(transition);
    transition.state = new StateSpec();
    transition.state.name = parsedName;
  }

  public void done() {
    fsm.done = true;
  }

  public void setSuperStateName() {
    setStateName();
    transition.state.abstractState = true;
  }

  public void setEvent() {
    subtransition = new SubTransition(parsedName);
  }

  public void setNullEvent() {
    subtransition = new SubTransition(null);
  }

  public void setEntryAction() {
    transition.state.entryActions.add(parsedName);
  }

  public void setExitAction() {
    transition.state.exitActions.add(parsedName);
  }

  public void setStateBase() {
    transition.state.superStates.add(parsedName);
  }

  public void setNextState() {
    subtransition.nextState = parsedName;
  }

  public void setNullNextState() {
    subtransition.nextState = null;
  }

  public void transitionWithAction() {
    subtransition.actions.add(parsedName);
    transition.subTransitions.add(subtransition);
  }

  public void transitionNullAction() {
    transition.subTransitions.add(subtransition);
  }

  public void addAction() {
    subtransition.actions.add(parsedName);
  }

  public void transitionWithActions() {
    transition.subTransitions.add(subtransition);
  }

  public void headerError(ParserState state, ParserEvent event, int line, int pos) {
    fsm.errors.add(new SyntaxError(HEADER, state+"|"+event, line, pos));
  }

  public void stateSpecError(ParserState state, ParserEvent event, int line, int pos) {
    fsm.errors.add(new SyntaxError(STATE, state+"|"+event, line, pos));

  }

  public void transitionError(ParserState state, ParserEvent event, int line, int pos) {
    fsm.errors.add(new SyntaxError(TRANSITION, state+"|"+event, line, pos));
  }

  public void transitionGroupError(ParserState state, ParserEvent event, int line, int pos) {
    fsm.errors.add(new SyntaxError(TRANSITION_GROUP, state+"|"+event, line, pos));
  }

  public void endError(ParserState state, ParserEvent event, int line, int pos) {
    fsm.errors.add(new SyntaxError(END, state+"|"+event, line, pos));
  }

  public void syntaxError(int line, int pos) {
    fsm.errors.add(new SyntaxError(SYNTAX, "", line, pos));
  }

  public void setName(String name) {
    parsedName = name;
  }

  public FsmSyntax getFsm() {
    return fsm;
  }
}
