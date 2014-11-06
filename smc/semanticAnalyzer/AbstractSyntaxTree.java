package smc.semanticAnalyzer;

import java.util.*;

public class AbstractSyntaxTree {
  public List<AnalysisError> errors = new ArrayList<>();
  public Map<String, State> states = new HashMap<>();
  public Set<String> events = new HashSet<>();
  public Set<String> actions = new HashSet<>();

  public String toString() {
    return "";
  }

  public void addError(AnalysisError analysisError) {
    errors.add(analysisError);
  }

  public static class State {
    public String name;

    public State(String name) {
      this.name = name;
    }

    public boolean equals(Object obj) {
      if (obj instanceof State) {
        State other = (State)obj;
        return Objects.equals(other.name, name);
      }
      else
        return false;
    }

    public String toString() {
      return String.format("state: %s", name);
    }
  }

  public static class AnalysisError {
    public enum ID {
      NO_ACTIONS,
      NO_FSM,
      NO_INITIAL,
      INVALID_HEADER,
      EXTRA_HEADER_IGNORED,
      UNDEFINED_STATE, UNDEFINED_SUPER_STATE,
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
      return String.format("Semantic Error: %s(%s)", id.name(), extra);
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
}
