package smc.optimizer;

import smc.StateMachine;
import smc.semanticAnalyzer.AbstractSyntaxTree;

import static smc.StateMachine.Header;
import static smc.StateMachine.SubTransition;
import static smc.StateMachine.Transition;
import static smc.semanticAnalyzer.AbstractSyntaxTree.SemanticTransition;
import static smc.semanticAnalyzer.AbstractSyntaxTree.State;

public class Optimizer {
  private StateMachine sm;
  private AbstractSyntaxTree ast;

  public StateMachine optimize(AbstractSyntaxTree ast) {
    this.ast = ast;
    sm = new StateMachine();
    addHeader(ast);
    addLists();
    addTransitions();
    return sm;
  }

  private void addTransitions() {
    for (State s : ast.states.values())
      addTransitionsForState(s);
  }

  private void addTransitionsForState(State state) {
    Transition transition = new Transition();
    transition.currentState = state.name;
    addSubTransitions(transition, state);
    sm.transitions.add(transition);
  }

  private void addSubTransitions(Transition transition, State s) {
    for (SemanticTransition semanticTransition : s.transitions)
      addSubTransition(semanticTransition, transition);
  }

  private void addSubTransition(SemanticTransition semanticTransition, Transition transition) {
    SubTransition subTransition = new SubTransition();
    subTransition.event = semanticTransition.event;
    subTransition.nextState = semanticTransition.nextState.name;
    subTransition.actions.addAll(semanticTransition.nextState.entryActions);
    subTransition.actions.addAll(semanticTransition.actions);
    subTransition.actions.addAll(semanticTransition.nextState.exitActions);
    transition.subTransitions.add(subTransition);
  }

  private void addHeader(AbstractSyntaxTree ast) {
    sm.header = new Header();
    sm.header.fsm = ast.fsmName;
    sm.header.initial = ast.initialState.name;
    sm.header.actions = ast.actionClass;
  }

  private void addLists() {
    addStates();
    addEvents();
    addActions();
  }

  private void addStates() {
    for (State s : ast.states.values())
      sm.states.add(s.name);
  }

  private void addEvents() {
    sm.events.addAll(ast.events);
  }

  private void addActions() {
    sm.actions.addAll(ast.actions);
  }
}
