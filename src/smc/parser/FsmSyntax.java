package smc.parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class FsmSyntax {
  public final List<Header> headers = new ArrayList<>();
  public final List<Transition> logic = new ArrayList<>();
  public final List<SyntaxError> errors = new ArrayList<>();
  public boolean done = false;

  public static class Header {
    public String name;
    public String value;

    public static Header NullHeader() {
        return new Header(null, null);
    }

    public Header() {
    }

    public Header(String name, String value) {
      this.name = name;
      this.value = value;
    }

    public int hashCode() {
      return Objects.hash(name, value);
    }

    public boolean equals(Object obj) {
      if (obj instanceof Header other) {
          return Objects.equals(other.name, name) && Objects.equals(other.value, value);
      }
      return false;
    }

  }

  public static class Transition {
    public StateSpec state;
    public final List<SubTransition> subTransitions = new ArrayList<>();
  }

  public static class StateSpec {
    public String name;
    public final List<String> superStates = new ArrayList<>();
    public final List<String> entryActions = new ArrayList<>();
    public final List<String> exitActions = new ArrayList<>();
    public boolean abstractState;
  }

  public static class SubTransition {
    public final String event;
    public String nextState;
    public final List<String> actions = new ArrayList<>();

    public SubTransition(String event) {
      this.event = event;
    }
  }

  public record SyntaxError(FsmSyntax.SyntaxError.Type type, String msg, int lineNumber, int position) {

    public String toString() {
        return String.format("Syntax Error Line: %d, Position: %d.  (%s) %s", lineNumber, position, type.name(), msg);
      }

      public enum Type {HEADER, STATE, TRANSITION, TRANSITION_GROUP, END, SYNTAX}
    }

  public String toString() {
    return
      formatHeaders() +
        formatLogic() +
        (done ? ".\n" : "") +
        formatErrors();
  }

  private String formatHeaders() {
    StringBuilder formattedHeaders = new StringBuilder();
    for (Header header : headers) {
        formattedHeaders.append(formatHeader(header));
    }
    return formattedHeaders.toString();
  }

  private String formatHeader(Header header) {
    return String.format("%s:%s\n", header.name, header.value);
  }

  private String formatLogic() {
    String ret = "";
    if (!logic.isEmpty()) {
        ret = String.format("{\n%s}\n", formatTransitions());
    }
    return ret;
  }

  private String formatTransitions() {
    StringBuilder transitions = new StringBuilder();
    for (Transition transition : logic) {
        transitions.append(formatTransition(transition));
    }
    return transitions.toString();
  }

  private String formatTransition(Transition transition) {
    return
      String.format("  %s %s\n",
        formatStateName(transition.state),
        formatSubTransitions(transition));
  }

  private String formatStateName(StateSpec stateSpec) {
    StringBuilder stateName = new StringBuilder(String.format(stateSpec.abstractState ? "(%s)" : "%s", stateSpec.name));
    for (String superState : stateSpec.superStates) {
        stateName.append(":").append(superState);
    }
    for (String entryAction : stateSpec.entryActions) {
        stateName.append(" <").append(entryAction);
    }
    for (String exitAction : stateSpec.exitActions) {
        stateName.append(" >").append(exitAction);
    }
    return stateName.toString();
  }

  private String formatSubTransitions(Transition transition) {
    if (transition.subTransitions.size() == 1) {
        return formatSubTransition(transition.subTransitions.getFirst());
    } else {
      String formattedSubTransitions = "{\n";

      for (SubTransition subtransition : transition.subTransitions) {
          formattedSubTransitions += "    " + formatSubTransition(subtransition) + "\n";
      }

      return formattedSubTransitions + "  }";
    }
  }

  private String formatSubTransition(SubTransition subtransition) {
    return String.format(
      "%s %s %s",
      subtransition.event,
      subtransition.nextState,
      formatActions(subtransition));
  }

  private String formatActions(SubTransition subtransition) {
    if (subtransition.actions.size() == 1) {
        return subtransition.actions.getFirst();
    } else {
      String actions = "{";
      boolean first = true;
      for (String action : subtransition.actions) {
        actions += (first ? "" : " ") + action;
        first = false;
      }

      return actions + "}";
    }
  }

  private String formatErrors() {
    String ret = "";
    if (!errors.isEmpty()) {
        ret = formatError(errors.getFirst());
    }
    return ret;
  }

  private String formatError(SyntaxError error) {
    return String.format(
      "Syntax error: %s. %s. line %d, position %d.\n",
      error.type.toString(),
      error.msg,
      error.lineNumber,
      error.position);
  }

  public String getError() {
    return formatErrors();
  }

}
