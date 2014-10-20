package smc;

import java.util.ArrayList;
import java.util.List;

public class FsmSyntax {
  public List<Header> headers = new ArrayList<>();
  public List<Transition> logic = new ArrayList<>();
  public List<SyntaxError> errors = new ArrayList<>();
  public boolean done = false;

  public static class Header {
    public String name;
    public String value;
  }

  public static class Transition {
    public StateSpec state;
    public List<SubTransition> subTransitions = new ArrayList<>();
  }

  public static class StateSpec {
    String name;
    String superState;
    String entryAction;
    String exitAction;
    boolean abstractState;
  }

  public static class SubTransition {
    String event;
    String nextState;
    List<String> actions = new ArrayList<>();

    public SubTransition(String event) {
      this.event = event;
    }
  }

  public static class SyntaxError {
    Type type;
    String msg;
    int lineNumber;
    int position;

    public SyntaxError(Type type, String msg, int lineNumber, int position) {
      this.type = type;
      this.msg = msg;
      this.lineNumber = lineNumber;
      this.position = position;
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
    String formattedHeaders = "";
    for (Header header : headers)
      formattedHeaders += formatHeader(header);
    return formattedHeaders;
  }

  private String formatHeader(Header header) {
    return String.format("%s:%s\n", header.name, header.value);
  }

  private String formatLogic() {
    if (logic.size() > 0)
      return String.format("{\n%s}\n", formatTransitions());
    else
      return "";
  }

  private String formatTransitions() {
    String transitions = "";
    for (Transition transition : logic)
      transitions += formatTransition(transition);
    return transitions;
  }

  private String formatTransition(Transition transition) {
    return
      String.format("  %s %s\n",
        formatStateName(transition.state),
        formatSubTransitions(transition));
  }

  private String formatStateName(StateSpec stateSpec) {
    String stateName = String.format(stateSpec.abstractState ? "(%s)" : "%s", stateSpec.name);
    if (stateSpec.superState != null)
      stateName += ":" + stateSpec.superState;
    if (stateSpec.entryAction != null)
      stateName += " <" + stateSpec.entryAction;
    if (stateSpec.exitAction != null)
      stateName += " >" + stateSpec.exitAction;
    return stateName;
  }

  private String formatSubTransitions(Transition transition) {
    if (transition.subTransitions.size() == 1)
      return formatSubTransition(transition.subTransitions.get(0));
    else {
      String formattedSubTransitions = "{\n";

      for (SubTransition subtransition : transition.subTransitions)
        formattedSubTransitions += "    " + formatSubTransition(subtransition) + "\n";

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
    if (subtransition.actions.size() == 1)
      return subtransition.actions.get(0);
    else {
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
    if (errors.size() > 0)
      return formatError(errors.get(0));
    else
      return "";
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
