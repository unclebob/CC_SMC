package smc.semanticAnalyzer;

import smc.parser.FsmSyntax;

import java.util.*;

import static smc.parser.FsmSyntax.*;
import static smc.semanticAnalyzer.AbstractSyntaxTree.*;
import static smc.semanticAnalyzer.AbstractSyntaxTree.AnalysisError.ID.*;

public class SemanticAnalyzer {
  private AbstractSyntaxTree ast;
  private Header fsmHeader = Header.NullHeader();
  private Header actionsHeader = new Header();
  private Header initialHeader = new Header();

  public AbstractSyntaxTree analyze(FsmSyntax fsm) {
    ast = new AbstractSyntaxTree();
    analyzeHeaders(fsm);
    checkForErrorsAndWarnings(fsm);
    compile(fsm);
    return ast;
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
        ast.addError(new AnalysisError(INVALID_HEADER, header));
    }
  }

  private boolean isNamed(Header header, String headerName) {
    return header.name.equalsIgnoreCase(headerName);
  }

  private void setHeader(Header targetHeader, Header header) {
    if (isNullHeader(targetHeader)) {
      targetHeader.name = header.name;
      targetHeader.value = header.value;
    } else
      ast.addError(new AnalysisError(EXTRA_HEADER_IGNORED, header));
  }

  private void checkMissingHeaders() {
    if (isNullHeader(fsmHeader))
      ast.addError(new AnalysisError(AnalysisError.ID.NO_FSM));
    if (isNullHeader(initialHeader))
      ast.addError(new AnalysisError(AnalysisError.ID.NO_INITIAL));
  }

  private boolean isNullHeader(Header header) {
    return header.name == null;
  }

  private void checkForErrorsAndWarnings(FsmSyntax fsm) {
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
    for (Transition t : fsm.logic)
      for (SubTransition st : t.subTransitions)
        for (String action : st.actions)
          ast.actions.add(action);
  }

  private void addEventsToEventList(FsmSyntax fsm) {
    for (Transition t : fsm.logic)
      for (SubTransition st : t.subTransitions)
        if (st.event != null)
          ast.events.add(st.event);
  }

  private void addEntryAndExitActionsToActionList(FsmSyntax fsm) {
    for (Transition t : fsm.logic) {
      for (String entryAction : t.state.entryActions)
        ast.actions.add(entryAction);
      for (String exitAction : t.state.exitActions)
        ast.actions.add(exitAction);
    }
  }

  private void addStateNamesToStateList(FsmSyntax fsm) {
    for (Transition t : fsm.logic) {
      State state = new State(t.state.name);
      ast.states.put(state.name, state);
    }
  }

  private void checkUndefinedStates(FsmSyntax fsm) {
    for (Transition t : fsm.logic) {
      for (String superState : t.state.superStates)
        checkUndefinedState(superState, UNDEFINED_SUPER_STATE);

      for (SubTransition st : t.subTransitions)
        checkUndefinedState(st.nextState, UNDEFINED_STATE);
    }

    if (initialHeader.value != null && !ast.states.containsKey(initialHeader.value))
      ast.errors.add(new AnalysisError(UNDEFINED_STATE, "initial: " + initialHeader.value));
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
    for (Transition t : fsm.logic)
      for (SubTransition st : t.subTransitions)
        if (st.nextState == null) // implicit use of current state.
          nextStates.add(t.state.name);
        else
          nextStates.add(st.nextState);
    return nextStates;
  }

  private Set<String> getSuperStates(FsmSyntax fsm) {
    Set<String> superStates = new HashSet<>();
    for (Transition t : fsm.logic)
      for (String superState : t.state.superStates)
        superStates.add(superState);
    return superStates;
  }

  private void findStatesDefinedButNotUsed(Set<String> usedStates) {
    for (String definedState : ast.states.keySet())
      if (!usedStates.contains(definedState))
        ast.errors.add(new AnalysisError(UNUSED_STATE, definedState));
  }

  private void checkForDuplicateTransitions(FsmSyntax fsm) {
    Set<String> transitionKeys = new HashSet<>();
    for (Transition t : fsm.logic) {
      for (SubTransition st : t.subTransitions) {
        String key = String.format("%s(%s)", t.state.name, st.event);
        if (transitionKeys.contains(key))
          ast.errors.add(new AnalysisError(DUPLICATE_TRANSITION, key));
        else
          transitionKeys.add(key);
      }
    }
  }

  private void checkThatAbstractStatesAreNotTargets(FsmSyntax fsm) {
    Set<String> abstractStates = findAbstractStates(fsm);
    for (Transition t : fsm.logic)
      for (SubTransition st : t.subTransitions)
        if (abstractStates.contains(st.nextState))
          ast.errors.add(
            new AnalysisError(
              ABSTRACT_STATE_USED_AS_NEXT_STATE,
              String.format("%s(%s)->%s", t.state.name, st.event, st.nextState)));
  }

  private Set<String> findAbstractStates(FsmSyntax fsm) {
    Set<String> abstractStates = new HashSet<>();
    for (Transition t : fsm.logic)
      if (t.state.abstractState)
        abstractStates.add(t.state.name);
    return abstractStates;
  }

  private void checkForInconsistentAbstraction(FsmSyntax fsm) {
    Set<String> abstractStates = findAbstractStates(fsm);
    for (Transition t : fsm.logic)
      if (!t.state.abstractState && abstractStates.contains(t.state.name))
        ast.warnings.add(new AnalysisError(INCONSISTENT_ABSTRACTION, t.state.name));
  }

  private void checkForMultiplyDefinedStateActions(FsmSyntax fsm) {
    Map<String, String> firstActionsForState = new HashMap<>();
    for (Transition t : fsm.logic) {
      if (specifiesStateActions(t)) {
        String actionsKey = makeActionsKey(t);
        if (firstActionsForState.containsKey(t.state.name)) {
          if (!firstActionsForState.get(t.state.name).equals(actionsKey))
            ast.errors.add(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, t.state.name));
        } else
          firstActionsForState.put(t.state.name, actionsKey);
      }
    }
  }

  private boolean specifiesStateActions(Transition t) {
    return t.state.entryActions.size() != 0 || t.state.exitActions.size() != 0;
  }

  private String makeActionsKey(Transition t) {
    List<String> actions = new ArrayList<>();
    actions.addAll(t.state.entryActions);
    actions.addAll(t.state.exitActions);
    return commaList(actions);
  }

  private String commaList(List<String> list) {
    String commaList = "";
    if (list.size() == 0)
      return "";
    for (String s : list)
      commaList += s + ",";
    return commaList.substring(0, commaList.length() - 1);

  }

  private void checkUndefinedState(String referencedState, AnalysisError.ID errorCode) {
    if (referencedState != null && !ast.states.containsKey(referencedState)) {
      ast.errors.add(new AnalysisError(errorCode, referencedState));
    }
  }

  private void compile(FsmSyntax fsm) {
    if (ast.errors.size() == 0) {
      compileHeaders();
      for (Transition t : fsm.logic) {
        State state = compileState(t);
        compileTransitions(t, state);
      }

      new SuperClassCrawler().checkSuperClassTransitions();
    }
  }

  private void compileHeaders() {
    ast.initialState = ast.states.get(initialHeader.value);
    ast.actionClass = actionsHeader.value;
    ast.fsmName = fsmHeader.value;
  }

  private State compileState(Transition t) {
    State state = ast.states.get(t.state.name);
    state.entryActions.addAll(t.state.entryActions);
    state.exitActions.addAll(t.state.exitActions);
    state.abstractState |= t.state.abstractState;
    for (String superStateName : t.state.superStates)
      state.superStates.add(ast.states.get(superStateName));
    return state;
  }

  private void compileTransitions(Transition t, State state) {
    for (SubTransition st : t.subTransitions)
      compileTransition(state, st);
  }

  private void compileTransition(State state, SubTransition st) {
    SemanticTransition semanticTransition = new SemanticTransition();
    semanticTransition.event = st.event;
    semanticTransition.nextState = st.nextState == null ? state : ast.states.get(st.nextState);
    semanticTransition.actions.addAll(st.actions);
    state.transitions.add(semanticTransition);
  }

  private class SuperClassCrawler {
    class TransitionTuple {
      TransitionTuple(String currentState, String event, String nextState, List<String> actions) {
        this.currentState = currentState;
        this.event = event;
        this.nextState = nextState;
        this.actions = actions;
      }

      public int hashCode() {
        return Objects.hash(currentState, event, nextState, actions);
      }

      public boolean equals(Object obj) {
        if (obj instanceof TransitionTuple) {
          TransitionTuple tt = (TransitionTuple) obj;
          return
            Objects.equals(currentState, tt.currentState) &&
              Objects.equals(event, tt.event) &&
              Objects.equals(nextState, tt.nextState) &&
              Objects.equals(actions, tt.actions);
        }
        return false;
      }

      String currentState;
      String event;
      String nextState;
      List<String> actions;
    }

    private State concreteState = null;
    private Map<String, TransitionTuple> transitionTuples;

    private void checkSuperClassTransitions() {
      for (State state : ast.states.values()) {
        if (state.abstractState == false) {
          concreteState = state;
          transitionTuples = new HashMap<>();
          checkTransitionsForState(concreteState);
        }
      }
    }

    private void checkTransitionsForState(State state) {
      for (State superState : state.superStates)
        checkTransitionsForState(superState);
      checkStateForPreviouslyDefinedTransition(state);
    }

    private void checkStateForPreviouslyDefinedTransition(State state) {
      for (SemanticTransition st : state.transitions)
        checkTransitionForPreviousDefinition(state, st);
    }

    private void checkTransitionForPreviousDefinition(State state, SemanticTransition st) {
      TransitionTuple thisTuple = new TransitionTuple(state.name, st.event, st.nextState.name, st.actions);
      if (transitionTuples.containsKey(thisTuple.event)) {
        determineIfThePreviousDefinitionIsAnError(state, thisTuple);
      } else
        transitionTuples.put(thisTuple.event, thisTuple);
    }

    private void determineIfThePreviousDefinitionIsAnError(State state, TransitionTuple thisTuple) {
      TransitionTuple previousTuple = transitionTuples.get(thisTuple.event);
      if (!transitionsHaveSameOutcomes(thisTuple, previousTuple))
        checkForOverriddenTransition(state, thisTuple, previousTuple);
    }

    private void checkForOverriddenTransition(State state, TransitionTuple thisTuple, TransitionTuple previousTuple) {
      State definingState = ast.states.get(previousTuple.currentState);
      if (!isSuperStateOf(definingState, state)) {
        ast.errors.add(new AnalysisError(CONFLICTING_SUPERSTATES, concreteState.name + "|" + thisTuple.event));
      } else
        transitionTuples.put(thisTuple.event, thisTuple);
    }

    private boolean transitionsHaveSameOutcomes(TransitionTuple t1, TransitionTuple t2) {
      return
        Objects.equals(t1.nextState, t2.nextState) &&
          Objects.equals(t1.actions, t2.actions);
    }
  }

  private boolean isSuperStateOf(State possibleSuperState, State state) {
    if (state == possibleSuperState)
      return true;
    for (State superState : state.superStates)
      if (isSuperStateOf(possibleSuperState, superState))
        return true;
    return false;
  }
}
