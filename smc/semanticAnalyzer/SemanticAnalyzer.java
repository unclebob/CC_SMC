package smc.semanticAnalyzer;

import smc.parser.FsmSyntax;

import static smc.parser.FsmSyntax.*;
import static smc.semanticAnalyzer.AbstractSyntaxTree.*;
import static smc.semanticAnalyzer.AbstractSyntaxTree.State;
import static smc.semanticAnalyzer.AbstractSyntaxTree.AnalysisError.ID.*;

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
    return ast;
  }

  private void checkUndefinedStates(FsmSyntax fsm) {
    for (Transition t : fsm.logic) {
      for (String superState : t.state.superStates)
        checkUndefinedState(superState, UNDEFINED_SUPER_STATE);

      for (SubTransition st : t.subTransitions)
        checkUndefinedState(st.nextState, UNDEFINED_STATE);
    }
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
    }
    else
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
