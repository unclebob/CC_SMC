package smc.semanticAnalyzer;

import smc.parser.FsmSyntax;

import java.util.*;

import static smc.parser.FsmSyntax.*;
import static smc.semanticAnalyzer.AbstractSyntaxTree.AnalysisError;
import static smc.semanticAnalyzer.AbstractSyntaxTree.AnalysisError.ID.*;
import static smc.semanticAnalyzer.AbstractSyntaxTree.SemanticTransition;
import static smc.semanticAnalyzer.AbstractSyntaxTree.State;

public class SemanticAnalyzer {
  private AbstractSyntaxTree ast;
  private Header fsmHeader = Header.NullHeader();
  private Header actionsHeader = new Header();
  private Header initialHeader = new Header();

  public AbstractSyntaxTree analyze(FsmSyntax fsm) {
    ast = new AbstractSyntaxTree();
    analyzeHeaders(fsm);
    createStateEventAndActionLists(fsm);
    checkUndefinedStates(fsm);
    checkForUnusedStates(fsm);
    checkForDuplicateTransitions(fsm);
    checkThatAbstractStatesAreNotTargets(fsm);
    checkForConcreteStatesWithNoEvents(fsm);
    checkForInconsistentAbstraction(fsm);
    checkForDisorganizedStateActions(fsm);

    if (ast.errors.size() == 0) {
      ast.initialState = ast.states.get(initialHeader.value);
      ast.actionClass = actionsHeader.value;
      ast.fsmName = fsmHeader.value;
      for (Transition t : fsm.logic) {
        State state = ast.states.get(t.state.name);
        state.entryActions.addAll(t.state.entryActions);
        state.exitActions.addAll(t.state.exitActions);
        state.abstractState |= t.state.abstractState;
        for (String superStateName : t.state.superStates)
          state.superStates.add(ast.states.get(superStateName));

        for (SubTransition st : t.subTransitions) {
          SemanticTransition semanticTransition = new SemanticTransition();
          semanticTransition.event = st.event;
          semanticTransition.nextState = st.nextState == null ? null : ast.states.get(st.nextState);
          semanticTransition.actions.addAll(st.actions);
          state.transitions.add(semanticTransition);
        }
      }
    }

    return ast;
  }

  private void checkForDisorganizedStateActions(FsmSyntax fsm) {
    Map<String, String> firstActionsForState = new HashMap<>();
    for (Transition t : fsm.logic) {
      List<String> actions = new ArrayList<>();
      actions.addAll(t.state.entryActions);
      actions.addAll(t.state.exitActions);
      if (firstActionsForState.containsKey(t.state.name)) {
        if (!firstActionsForState.get(t.state.name).equals(commaList(actions)))
          ast.warnings.add(new AnalysisError(STATE_ACTIONS_DISORGANIZED, t.state.name));
      } else
        firstActionsForState.put(t.state.name, commaList(actions));
    }
  }

  private String commaList(List<String> list) {
    String commaList = "";
    if (list.size() == 0)
      return "";
    for (String s : list)
      commaList += s + ",";
    return commaList.substring(0, commaList.length() - 1);

  }

  private void checkForInconsistentAbstraction(FsmSyntax fsm) {
    Set<String> abstractStates = new HashSet<>();
    for (Transition t : fsm.logic)
      if (t.state.abstractState)
        abstractStates.add(t.state.name);
    for (Transition t : fsm.logic)
      if (!t.state.abstractState && abstractStates.contains(t.state.name))
        ast.warnings.add(new AnalysisError(INCONSISTENT_ABSTRACTION, t.state.name));
  }

  private void checkForConcreteStatesWithNoEvents(FsmSyntax fsm) {
    for (Transition t : fsm.logic)
      if (t.state.abstractState == false)
        for (SubTransition st : t.subTransitions) {
          if (st.event == null)
            ast.errors.add(new AnalysisError(CONCRETE_STATE_WITH_NO_EVENT, t.state.name));
          if (st.nextState == null)
            ast.errors.add(new AnalysisError(CONCRETE_STATE_WITH_NO_NEXT_STATE, t.state.name));
        }
  }

  private void checkThatAbstractStatesAreNotTargets(FsmSyntax fsm) {
    Set<String> abstractStates = new HashSet<>();
    for (Transition t : fsm.logic)
      if (t.state.abstractState)
        abstractStates.add(t.state.name);
    for (Transition t : fsm.logic)
      for (SubTransition st : t.subTransitions)
        if (abstractStates.contains(st.nextState))
          ast.errors.add(
            new AnalysisError(
              ABSTRACT_STATE_USED_AS_NEXT_STATE,
              String.format("%s(%s)->%s", t.state.name, st.event, st.nextState)));
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

  private void checkForUnusedStates(FsmSyntax fsm) {
    Set<String> usedStates = new HashSet<>();
    usedStates.add(initialHeader.value);
    for (Transition t : fsm.logic) {
      for (String superState : t.state.superStates)
        usedStates.add(superState);
      for (SubTransition st : t.subTransitions)
        if (st.nextState != null)
          usedStates.add(st.nextState);
    }
    for (String definedState : ast.states.keySet())
      if (!usedStates.contains(definedState))
        ast.errors.add(new AnalysisError(UNUSED_STATE, definedState));
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

  private void checkUndefinedState(String referencedState, AnalysisError.ID errorCode) {
    if (referencedState != null && !ast.states.containsKey(referencedState)) {
      ast.errors.add(new AnalysisError(errorCode, referencedState));
    }
  }

  private void createStateEventAndActionLists(FsmSyntax fsm) {
    for (Transition t : fsm.logic) {
      State state = new State(t.state.name);
      ast.states.put(state.name, state);
      for (SubTransition st : t.subTransitions) {
        ast.events.add(st.event);
        for (String action : st.actions)
          ast.actions.add(action);
      }
    }
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
    if (isNullHeader(actionsHeader))
      ast.addError(new AnalysisError(AnalysisError.ID.NO_ACTIONS));
    if (isNullHeader(fsmHeader))
      ast.addError(new AnalysisError(AnalysisError.ID.NO_FSM));
    if (isNullHeader(initialHeader))
      ast.addError(new AnalysisError(AnalysisError.ID.NO_INITIAL));
  }

  private boolean isNullHeader(Header header) {
    return header.name == null;
  }
}
