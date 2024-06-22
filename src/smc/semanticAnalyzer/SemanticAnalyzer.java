package smc.semanticAnalyzer;

import smc.parser.FsmSyntax;

import java.util.*;
import java.util.stream.Collectors;

import static smc.parser.FsmSyntax.*;
import static smc.semanticAnalyzer.SemanticStateMachine.*;
import static smc.semanticAnalyzer.SemanticStateMachine.AnalysisError.ID.*;

public class SemanticAnalyzer {
  private SemanticStateMachine semanticStateMachine;
  private final Header fsmHeader = Header.NullHeader();
  private final Header actionsHeader = new Header();
  private final Header initialHeader = new Header();

  public SemanticStateMachine analyze(FsmSyntax fsm) {
    semanticStateMachine = new SemanticStateMachine();
    analyzeHeaders(fsm);
    checkSemanticValidity(fsm);
    produceSemanticStateMachine(fsm);
    return semanticStateMachine;
  }

  private void analyzeHeaders(FsmSyntax fsm) {
    setHeaders(fsm);
    checkMissingHeaders();
  }

  private void setHeaders(FsmSyntax fsm) {
    for (Header header : fsm.headers) {
      if (isNamed(header, "fsm"))
        setHeader(fsmHeader, header);
      else if (isNamed(header, "actions"))
        setHeader(actionsHeader, header);
      else if (isNamed(header, "initial"))
        setHeader(initialHeader, header);
      else
        semanticStateMachine.addError(new AnalysisError(INVALID_HEADER, header));
    }
  }

  private boolean isNamed(Header header, String headerName) {
    return header.name.equalsIgnoreCase(headerName);
  }

  private void setHeader(Header targetHeader, Header header) {
    if (isNullHeader(targetHeader)) {
      targetHeader.name = header.name;
      targetHeader.value = header.value;
    } else {
        semanticStateMachine.addError(new AnalysisError(EXTRA_HEADER_IGNORED, header));
    }
  }

  private void checkMissingHeaders() {
    if (isNullHeader(fsmHeader)) {
        semanticStateMachine.addError(new AnalysisError(AnalysisError.ID.NO_FSM));
    }
    if (isNullHeader(initialHeader)) {
        semanticStateMachine.addError(new AnalysisError(AnalysisError.ID.NO_INITIAL));
    }
  }

  private boolean isNullHeader(Header header) {
    return header.name == null;
  }

  private void checkSemanticValidity(FsmSyntax fsm) {
    createStateEventAndActionLists(fsm);
    checkUndefinedStates(fsm);
    checkForUnusedStates(fsm);
    checkForDuplicateTransitions(fsm);
    checkThatAbstractStatesAreNotTargets(fsm);
    checkForInconsistentAbstraction(fsm);
    checkForMultiplyDefinedStateActions(fsm);
  }

  private void createStateEventAndActionLists(FsmSyntax fsm) {
    addStateNamesToStateList(fsm);
    addEntryAndExitActionsToActionList(fsm);
    addEventsToEventList(fsm);
    addTransitionActionsToActionList(fsm);
  }

  private void addTransitionActionsToActionList(FsmSyntax fsm) {
      fsm.logic.stream().flatMap(t ->
              t.subTransitions.stream()).forEach(st ->
                semanticStateMachine.actions.addAll(st.actions));
  }

  private void addEventsToEventList(FsmSyntax fsm) {
      fsm.logic.stream().flatMap(t ->
              t.subTransitions.stream()).filter(st ->
                st.event != null).forEachOrdered(st -> semanticStateMachine.events.add(st.event));
  }

  private void addEntryAndExitActionsToActionList(FsmSyntax fsm) {
      fsm.logic.forEach(t -> {
          semanticStateMachine.actions.addAll(t.state.entryActions);
          semanticStateMachine.actions.addAll(t.state.exitActions);
      });
  }

  private void addStateNamesToStateList(FsmSyntax fsm) {
      fsm.logic.stream().map(t ->
              new SemanticState(t.state.name)).forEachOrdered(state -> semanticStateMachine.states.put(state.name, state));
  }

  private void checkUndefinedStates(FsmSyntax fsm) {
      fsm.logic.forEach(t -> {
          t.state.superStates.forEach(superState -> checkUndefinedState(superState, UNDEFINED_SUPER_STATE));
          t.subTransitions.forEach(st -> checkUndefinedState(st.nextState, UNDEFINED_STATE));
      });

    if (initialHeader.value != null && !semanticStateMachine.states.containsKey(initialHeader.value))
      semanticStateMachine.errors.add(new AnalysisError(UNDEFINED_STATE, "initial: " + initialHeader.value));
  }

  private void checkForUnusedStates(FsmSyntax fsm) {
    findStatesDefinedButNotUsed(findUsedStates(fsm));
  }

  private Set<String> findUsedStates(FsmSyntax fsm) {
    Set<String> usedStates = new HashSet<>();
    usedStates.add(initialHeader.value);
    usedStates.addAll(getSuperStates(fsm));
    usedStates.addAll(getNextStates(fsm));
    return usedStates;
  }

  private Set<String> getNextStates(FsmSyntax fsm) {
    Set<String> nextStates = new HashSet<>();
      fsm.logic.forEach(t -> t.subTransitions.forEach(st -> {
          if (st.nextState == null) // implicit use of current state.
          {
              nextStates.add(t.state.name);
          } else {
              nextStates.add(st.nextState);
          }
      }));
    return nextStates;
  }

  private Set<String> getSuperStates(FsmSyntax fsm) {
      return fsm.logic.stream().flatMap(t -> t.state.superStates.stream()).collect(Collectors.toSet());
  }

  private void findStatesDefinedButNotUsed(Set<String> usedStates) {
      semanticStateMachine.states.keySet().stream().filter(definedState ->
              !usedStates.contains(definedState)).forEach(definedState ->
                semanticStateMachine.errors.add(new AnalysisError(UNUSED_STATE, definedState)));
  }

  private void checkForDuplicateTransitions(FsmSyntax fsm) {
    Set<String> transitionKeys = new HashSet<>();
      fsm.logic.forEach(t -> t.subTransitions.stream().map(st -> String.format("%s(%s)", t.state.name, st.event)).forEach(key -> {
          if (transitionKeys.contains(key)) {
              semanticStateMachine.errors.add(new AnalysisError(DUPLICATE_TRANSITION, key));
          } else {
              transitionKeys.add(key);
          }
      }));
  }

  private void checkThatAbstractStatesAreNotTargets(FsmSyntax fsm) {
    Set<String> abstractStates = findAbstractStates(fsm);
      fsm.logic.forEach(t -> t.subTransitions.stream().filter(st -> abstractStates.contains(st.nextState)).forEachOrdered(st ->
              semanticStateMachine.errors.add(
                new AnalysisError(
                      ABSTRACT_STATE_USED_AS_NEXT_STATE,
                      String.format("%s(%s)->%s", t.state.name, st.event, st.nextState)))));
  }

  private Set<String> findAbstractStates(FsmSyntax fsm) {
     return fsm.logic.stream().filter(t ->
            t.state.abstractState).map(t -> t.state.name).collect(Collectors.toSet());
  }

  private void checkForInconsistentAbstraction(FsmSyntax fsm) {
    Set<String> abstractStates = findAbstractStates(fsm);
      fsm.logic.stream().filter(t -> !t.state.abstractState && abstractStates.contains(t.state.name)).forEachOrdered(t ->
              semanticStateMachine.warnings.add(new AnalysisError(INCONSISTENT_ABSTRACTION, t.state.name)));
  }

  private void checkForMultiplyDefinedStateActions(FsmSyntax fsm) {
    Map<String, String> firstActionsForState = new HashMap<>();
      fsm.logic.stream().filter(this::specifiesStateActions).forEachOrdered(t -> {
          String actionsKey = makeActionsKey(t);
          if (firstActionsForState.containsKey(t.state.name)) {
              if (!firstActionsForState.get(t.state.name).equals(actionsKey)) {
                  semanticStateMachine.errors.add(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, t.state.name));
              }
          } else
              firstActionsForState.put(t.state.name, actionsKey);
      });
  }

  private boolean specifiesStateActions(Transition t) {
    return !t.state.entryActions.isEmpty() || !t.state.exitActions.isEmpty();
  }

  private String makeActionsKey(Transition t) {
    List<String> actions = new ArrayList<>();
    actions.addAll(t.state.entryActions);
    actions.addAll(t.state.exitActions);
    return commaList(actions);
  }

  private String commaList(List<String> list) {
    String commaList = "";
    if (list.isEmpty()) {
        return "";
    }
    for (String s : list) {
        commaList += s + ",";
    }
    return commaList.substring(0, commaList.length() - 1);

  }

  private void checkUndefinedState(String referencedState, AnalysisError.ID errorCode) {
    if (referencedState != null && !semanticStateMachine.states.containsKey(referencedState)) {
      semanticStateMachine.errors.add(new AnalysisError(errorCode, referencedState));
    }
  }

  private void produceSemanticStateMachine(FsmSyntax fsm) {
    if (semanticStateMachine.errors.isEmpty()) {
      compileHeaders();
      for (Transition t : fsm.logic) {
        SemanticState state = compileState(t);
        compileTransitions(t, state);
      }

      new SuperClassCrawler().checkSuperClassTransitions();
    }
  }

  private void compileHeaders() {
    semanticStateMachine.initialState = semanticStateMachine.states.get(initialHeader.value);
    semanticStateMachine.actionClass = actionsHeader.value;
    semanticStateMachine.fsmName = fsmHeader.value;
  }

  private SemanticState compileState(Transition t) {
    SemanticState state = semanticStateMachine.states.get(t.state.name);
    state.entryActions.addAll(t.state.entryActions);
    state.exitActions.addAll(t.state.exitActions);
    state.abstractState |= t.state.abstractState;
    t.state.superStates.forEach(superStateName -> state.superStates.add(semanticStateMachine.states.get(superStateName)));
    return state;
  }

  private void compileTransitions(Transition t, SemanticState state) {
      t.subTransitions.forEach(st -> compileTransition(state, st));
  }

  private void compileTransition(SemanticState state, SubTransition st) {
    SemanticTransition semanticTransition = new SemanticTransition();
    semanticTransition.event = st.event;
    semanticTransition.nextState = st.nextState == null ? state : semanticStateMachine.states.get(st.nextState);
    semanticTransition.actions.addAll(st.actions);
    state.transitions.add(semanticTransition);
  }

  private class SuperClassCrawler {
    record TransitionTuple(String currentState, String event, String nextState, List<String> actions) {

      public boolean equals(Object obj) {
            if (obj instanceof TransitionTuple tt) {
              return
                      Objects.equals(currentState, tt.currentState) &&
                              Objects.equals(event, tt.event) &&
                              Objects.equals(nextState, tt.nextState) &&
                              Objects.equals(actions, tt.actions);
            }
            return false;
          }

    }

    private SemanticState concreteState = null;
    private Map<String, TransitionTuple> transitionTuples;

    private void checkSuperClassTransitions() {
      for (SemanticState state : semanticStateMachine.states.values()) {
        if (!state.abstractState) {
          concreteState = state;
          transitionTuples = new HashMap<>();
          checkTransitionsForState(concreteState);
        }
      }
    }

    private void checkTransitionsForState(SemanticState state) {
      state.superStates.forEach(this::checkTransitionsForState);
      checkStateForPreviouslyDefinedTransition(state);
    }

    private void checkStateForPreviouslyDefinedTransition(SemanticState state) {
        state.transitions.forEach(st -> checkTransitionForPreviousDefinition(state, st));
    }

    private void checkTransitionForPreviousDefinition(SemanticState state, SemanticTransition st) {
      TransitionTuple thisTuple = new TransitionTuple(state.name, st.event, st.nextState.name, st.actions);
      if (transitionTuples.containsKey(thisTuple.event)) {
        determineIfThePreviousDefinitionIsAnError(state, thisTuple);
      } else
        transitionTuples.put(thisTuple.event, thisTuple);
    }

    private void determineIfThePreviousDefinitionIsAnError(SemanticState state, TransitionTuple thisTuple) {
      TransitionTuple previousTuple = transitionTuples.get(thisTuple.event);
      if (!transitionsHaveSameOutcomes(thisTuple, previousTuple)) {
          checkForOverriddenTransition(state, thisTuple, previousTuple);
      }
    }

    private void checkForOverriddenTransition(SemanticState state, TransitionTuple thisTuple, TransitionTuple previousTuple) {
      SemanticState definingState = semanticStateMachine.states.get(previousTuple.currentState);
      if (!isSuperStateOf(definingState, state)) {
        semanticStateMachine.errors.add(new AnalysisError(CONFLICTING_SUPERSTATES, concreteState.name + "|" + thisTuple.event));
      } else {
          transitionTuples.put(thisTuple.event, thisTuple);
      }
    }

    private boolean transitionsHaveSameOutcomes(TransitionTuple t1, TransitionTuple t2) {
      return
        Objects.equals(t1.nextState, t2.nextState) &&
          Objects.equals(t1.actions, t2.actions);
    }
  }

  private boolean isSuperStateOf(SemanticState possibleSuperState, SemanticState state) {
    if (state == possibleSuperState) {
        return true;
    }
    return state.superStates.stream().anyMatch(superState -> isSuperStateOf(possibleSuperState, superState));
  }
}
