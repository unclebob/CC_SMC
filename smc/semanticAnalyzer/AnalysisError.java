package smc.semanticAnalyzer;

import java.util.Objects;

public class AnalysisError {

  public enum ID {
    NO_ACTIONS,
    NO_FSM,
    NO_INITIAL,
    INVALID_HEADER, EXTRA_HEADER_IGNORED
  };

  private ID id;
  private Object extra;

  public AnalysisError(ID id) {
    this.id = id;
  }

  public AnalysisError(ID id, Object extra) {
    this(id);
    this.extra = extra;
  }

  public String toString() {
    return "Semantic Error: " + id.name();
  }

  public int hashCode() {
    return Objects.hash(id, extra);
  }

  public boolean equals(Object obj) {
    if (obj instanceof AnalysisError) {
      AnalysisError other = (AnalysisError)obj;
      return id == other.id && Objects.equals(extra, other.extra);
    }
    return false;
  }
}
