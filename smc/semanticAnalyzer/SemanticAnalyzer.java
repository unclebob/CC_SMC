package smc.semanticAnalyzer;

import smc.parser.FsmSyntax;

import static smc.parser.FsmSyntax.Header;
import static smc.semanticAnalyzer.AnalysisError.ID.EXTRA_HEADER_IGNORED;
import static smc.semanticAnalyzer.AnalysisError.ID.INVALID_HEADER;

public class SemanticAnalyzer {
  private AbstractSyntaxTree ast;
  private Header fsmHeader = Header.NullHeader();
  private Header actionsHeader = new Header();
  private Header initialHeader = new Header();

  public AbstractSyntaxTree analyze(FsmSyntax fsm) {
    ast = new AbstractSyntaxTree();
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
    if (isNullHeader(actionsHeader))
      ast.addError(new AnalysisError(AnalysisError.ID.NO_ACTIONS));
    if (isNullHeader(fsmHeader))
      ast.addError(new AnalysisError(AnalysisError.ID.NO_FSM));
    if (isNullHeader(initialHeader))
      ast.addError(new AnalysisError(AnalysisError.ID.NO_INITIAL));
    return ast;
  }

  private boolean isNullHeader(Header header) {
    return header.name == null;
  }

  private void setHeader(Header targetHeader, Header header) {
    if (isNullHeader(targetHeader)) {
      targetHeader.name = header.name;
      targetHeader.value = header.value;
    }
    else
      ast.addError(new AnalysisError(EXTRA_HEADER_IGNORED, header));
  }

  private boolean isNamed(Header header, String headerName) {
    return header.name.equalsIgnoreCase(headerName);
  }
}
