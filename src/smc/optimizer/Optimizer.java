package smc.optimizer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import smc.OptimizedStateMachine;
import smc.OptimizedStateMachine.Header;
import smc.OptimizedStateMachine.SubTransition;
import smc.OptimizedStateMachine.Transition;
import smc.semanticAnalyzer.SemanticStateMachine;
import smc.semanticAnalyzer.SemanticStateMachine.SemanticState;
import smc.semanticAnalyzer.SemanticStateMachine.SemanticTransition;

public class Optimizer {
  private OptimizedStateMachine optimizedStateMachine;
  private SemanticStateMachine semanticStateMachine;

  public OptimizedStateMachine optimize(SemanticStateMachine ast) {
    this.semanticStateMachine = ast;
    optimizedStateMachine = new OptimizedStateMachine();
    addHeader(ast);
    addLists();
    addTransitions();
    return optimizedStateMachine;
  }

  private void addTransitions() {
    for (SemanticState s : semanticStateMachine.states.values())
      if (!s.abstractState)
        new StateOptimizer(s).addTransitionsForState();
  }

  private class StateOptimizer {
    private SemanticState currentState;
    private Set<String> eventsForThisState = new HashSet<>();

    public StateOptimizer(SemanticState currentState) {
      this.currentState = currentState;
    }

    private void addTransitionsForState() {
      Transition transition = new Transition();
      transition.currentState = currentState.name;
      addSubTransitions(transition);
      optimizedStateMachine.transitions.add(transition);
    }

    private void addSubTransitions(Transition transition) {
      for (SemanticState stateInHierarchy : makeRootFirstHierarchyOfStates())
        addStateTransitions(transition, stateInHierarchy);
    }

    private List<SemanticState> makeRootFirstHierarchyOfStates() {
      List<SemanticState> hierarchy = new ArrayList<>();
      addAllStatesInHiearchyLeafFirst(currentState, hierarchy);
      Collections.reverse(hierarchy);
      return hierarchy;
    }

    private void addStateTransitions(Transition transition, SemanticState state) {
      for (SemanticTransition semanticTransition : state.transitions) {
        if (eventExistsAndHasNotBeenOverridden(semanticTransition.event))
          addSubTransition(semanticTransition, transition);
      }
    }

    private boolean eventExistsAndHasNotBeenOverridden(String event) {
      return event != null && !eventsForThisState.contains(event);
    }

    private void addSubTransition(SemanticTransition semanticTransition,
        Transition transition) {
      eventsForThisState.add(semanticTransition.event);
      SubTransition subTransition = new SubTransition();
      new SubTransitionOptimizer(semanticTransition, subTransition).optimize();
      transition.subTransitions.add(subTransition);
    }

    private class SubTransitionOptimizer {
      private SemanticTransition semanticTransition;
      private SubTransition subTransition;

      public SubTransitionOptimizer(SemanticTransition semanticTransition,
          SubTransition subTransition) {
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

      private void addEntryActions(SemanticState entryState) {
        List<SemanticState> hierarchy = new ArrayList<>();
        addAllStatesInHiearchyLeafFirst(entryState, hierarchy);
        for (SemanticState superState : hierarchy) {
          subTransition.actions.addAll(superState.entryActions);
        }
      }

      private void addExitActions(SemanticState exitState) {
        List<SemanticState> hierarchy = new ArrayList<>();
        addAllStatesInHiearchyLeafFirst(exitState, hierarchy);
        Collections.reverse(hierarchy);
        for (SemanticState superState : hierarchy) {
          subTransition.actions.addAll(superState.exitActions);
        }
      }
    } // SubTransitionOptimizer
  } // StateOptimizer

  private void addAllStatesInHiearchyLeafFirst(SemanticState state,
      List<SemanticState> hierarchy) {
    for (SemanticState superState : state.superStates) {
      if (!hierarchy.contains(superState))
        addAllStatesInHiearchyLeafFirst(superState, hierarchy);
    }
    hierarchy.add(state);
  }

  private void addHeader(SemanticStateMachine ast) {
    optimizedStateMachine.header = new Header();
    optimizedStateMachine.header.fsm = ast.fsmName;
    optimizedStateMachine.header.initial = ast.initialState.name;
    optimizedStateMachine.header.actions = ast.actionClass;
  }

  private void addLists() {
    addStates();
    addEvents();
    addActions();
  }

  private void addStates() {
    for (SemanticState s : semanticStateMachine.states.values())
      if (!s.abstractState)
        optimizedStateMachine.states.add(s.name);
  }

  private void addEvents() {
    optimizedStateMachine.events.addAll(semanticStateMachine.events);
  }

  private void addActions() {
    optimizedStateMachine.actions.addAll(semanticStateMachine.actions);
  }
}
