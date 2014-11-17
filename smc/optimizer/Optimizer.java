package smc.optimizer;

import smc.StateMachine;
import smc.semanticAnalyzer.AbstractSyntaxTree;

import java.util.*;

import static smc.StateMachine.*;
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
      if (!s.abstractState)
        new StateOptimizer(s).addTransitionsForState();
  }

  private class StateOptimizer {
    private State currentState;
    private Set<String> eventsForThisState = new HashSet<>();

    public StateOptimizer(State currentState) {
      this.currentState = currentState;
    }

    private void addTransitionsForState() {
      Transition transition = new Transition();
      transition.currentState = currentState.name;
      addSubTransitions(transition);
      sm.transitions.add(transition);
    }

    private void addSubTransitions(Transition transition) {
      for (State stateInHierarchy : makeRootFirstHierarchyOfStates())
        addStateTransitions(transition, stateInHierarchy);
    }

    private List<State> makeRootFirstHierarchyOfStates() {
      List<State> hierarchy = new ArrayList<>();
      addAllStatesInHiearchyLeafFirst(currentState, hierarchy);
      Collections.reverse(hierarchy);
      return hierarchy;
    }

    private void addStateTransitions(Transition transition, State state) {
      for (SemanticTransition semanticTransition : state.transitions)
        if (eventExistsAndHasNotBeenOverridden(semanticTransition.event))
          addSubTransition(semanticTransition, transition);
    }

    private boolean eventExistsAndHasNotBeenOverridden(String event) {
      return event != null && !eventsForThisState.contains(event);
    }

    private void addSubTransition(SemanticTransition semanticTransition, Transition transition) {
      eventsForThisState.add(semanticTransition.event);
      SubTransition subTransition = new SubTransition();
      new SubTransitionOptimizer(semanticTransition, subTransition).optimize();
      transition.subTransitions.add(subTransition);
    }

    private class SubTransitionOptimizer {
      private SemanticTransition semanticTransition;
      private SubTransition subTransition;

      public SubTransitionOptimizer(SemanticTransition semanticTransition, SubTransition subTransition) {
        this.semanticTransition = semanticTransition;
        this.subTransition = subTransition;
      }

      public void optimize() {
        subTransition.event = semanticTransition.event;
        subTransition.nextState = semanticTransition.nextState.name;
        addExitActions(currentState);
        addEntryActions(semanticTransition.nextState);
        subTransition.actions.addAll(semanticTransition.actions);
      }

      private void addEntryActions(State entryState) {
        List<State> hierarchy = new ArrayList<>();
        addAllStatesInHiearchyLeafFirst(entryState, hierarchy);
        for (State superState : hierarchy) {
          subTransition.actions.addAll(superState.entryActions);
        }
      }

      private void addExitActions(State exitState) {
        List<State> hierarchy = new ArrayList<>();
        addAllStatesInHiearchyLeafFirst(exitState, hierarchy);
        Collections.reverse(hierarchy);
        for (State superState : hierarchy) {
          subTransition.actions.addAll(superState.exitActions);
        }
      }
    } // SubTransitionOptimizer
  } // StateOptimizer

  private void addAllStatesInHiearchyLeafFirst(State state, List<State> hierarchy) {
    for (State superState : state.superStates) {
      if (!hierarchy.contains(superState))
        addAllStatesInHiearchyLeafFirst(superState, hierarchy);
    }
    hierarchy.add(state);
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
