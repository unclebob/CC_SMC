package smc.semanticAnalyzer;

import java.util.ArrayList;
import java.util.List;

public class AbstractSyntaxTree {
  private List<AnalysisError> errors = new ArrayList<>();

  public String toString() {
    return "";
  }

  public List<AnalysisError> getErrors() {
    return errors;
  }

  public void addError(AnalysisError analysisError) {
    errors.add(analysisError);
  }
}
