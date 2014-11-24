package smc.optimizer;

import smc.StateMachine;
import smc.semanticAnalyzer.AbstractSyntaxTree;

import java.util.*;

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
    for (AbstractSyntaxTree.State s : ast.states.values())
      if (!s.abstractState)
        new StateOptimizer(s).addTransitionsForState();
  }

  private class StateOptimizer {
    private AbstractSyntaxTree.State currentState;
    private Set<String> eventsForThisState = new HashSet<>();

    public StateOptimizer(AbstractSyntaxTree.State currentState) {
      this.currentState = currentState;
    }

    private void addTransitionsForState() {
      StateMachine.Transition transition = new StateMachine.Transition();
      transition.currentState = currentState.name;
      addSubTransitions(transition);
      sm.transitions.add(transition);
    }

    private void addSubTransitions(StateMachine.Transition transition) {
      for (AbstractSyntaxTree.State stateInHierarchy : makeRootFirstHierarchyOfStates())
        addStateTransitions(transition, stateInHierarchy);
    }

    private List<AbstractSyntaxTree.State> makeRootFirstHierarchyOfStates() {
      List<AbstractSyntaxTree.State> hierarchy = new ArrayList<>();
      addAllStatesInHiearchyLeafFirst(currentState, hierarchy);
      Collections.reverse(hierarchy);
      return hierarchy;
    }

    private void addStateTransitions(StateMachine.Transition transition, AbstractSyntaxTree.State state) {
      for (AbstractSyntaxTree.SemanticTransition semanticTransition : state.transitions)
        if (eventExistsAndHasNotBeenOverridden(semanticTransition.event))
          addSubTransition(semanticTransition, transition);
    }

    private boolean eventExistsAndHasNotBeenOverridden(String event) {
      return event != null && !eventsForThisState.contains(event);
    }

    private void addSubTransition(AbstractSyntaxTree.SemanticTransition semanticTransition, StateMachine.Transition transition) {
      eventsForThisState.add(semanticTransition.event);
      StateMachine.SubTransition subTransition = new StateMachine.SubTransition();
      new SubTransitionOptimizer(semanticTransition, subTransition).optimize();
      transition.subTransitions.add(subTransition);
    }

    private class SubTransitionOptimizer {
      private AbstractSyntaxTree.SemanticTransition semanticTransition;
      private StateMachine.SubTransition subTransition;

      public SubTransitionOptimizer(AbstractSyntaxTree.SemanticTransition semanticTransition, StateMachine.SubTransition subTransition) {
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

      private void addEntryActions(AbstractSyntaxTree.State entryState) {
        List<AbstractSyntaxTree.State> hierarchy = new ArrayList<>();
        addAllStatesInHiearchyLeafFirst(entryState, hierarchy);
        for (AbstractSyntaxTree.State superState : hierarchy) {
          subTransition.actions.addAll(superState.entryActions);
        }
      }

      private void addExitActions(AbstractSyntaxTree.State exitState) {
        List<AbstractSyntaxTree.State> hierarchy = new ArrayList<>();
        addAllStatesInHiearchyLeafFirst(exitState, hierarchy);
        Collections.reverse(hierarchy);
        for (AbstractSyntaxTree.State superState : hierarchy) {
          subTransition.actions.addAll(superState.exitActions);
        }
      }
    } // SubTransitionOptimizer
  } // StateOptimizer

  private void addAllStatesInHiearchyLeafFirst(AbstractSyntaxTree.State state, List<AbstractSyntaxTree.State> hierarchy) {
    for (AbstractSyntaxTree.State superState : state.superStates) {
      if (!hierarchy.contains(superState))
        addAllStatesInHiearchyLeafFirst(superState, hierarchy);
    }
    hierarchy.add(state);
  }

  private void addHeader(AbstractSyntaxTree ast) {
    sm.header = new StateMachine.Header();
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
    for (AbstractSyntaxTree.State s : ast.states.values())
      sm.states.add(s.name);
  }

  private void addEvents() {
    sm.events.addAll(ast.events);
  }

  private void addActions() {
    sm.actions.addAll(ast.actions);
  }
}
