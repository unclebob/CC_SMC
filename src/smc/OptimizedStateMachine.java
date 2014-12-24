package smc;

import java.util.ArrayList;
import java.util.List;

// This is the final output of the finite state machine parser.
// Code generators will use this format as their input.

public class OptimizedStateMachine {
  public List<String> states = new ArrayList<>();
  public List<String> events = new ArrayList<>();
  public List<String> actions = new ArrayList<>();
  public Header header;
  public List<Transition> transitions = new ArrayList<>();

  public String transitionsToString() {
    String result = "";
    for (Transition t : transitions)
      result += t;
    return result;
  }

  public String toString() {
    String transitionsString = transitionsToString().replaceAll("\n", "\n  ");
    transitionsString = transitionsString.substring(0, transitionsString.length()-2);
    return String.format("Initial: %s\nFsm: %s\nActions:%s\n{\n  %s}\n",
      header.initial, header.fsm, header.actions, transitionsString);
  }

  public static class Header {
    public String initial;
    public String fsm;
    public String actions;
  }

  public static class Transition {
    public String toString() {
      String result = String.format("%s {\n", currentState);
      for (SubTransition st : subTransitions)
        result += st.toString();
      result += "}\n";
      return result;
    }

    public String currentState;
    public List<SubTransition> subTransitions = new ArrayList<>();
  }

  public static class SubTransition {
    public String toString() {
      return String.format("  %s %s {%s}\n", event, nextState, actionsToString());
    }

    private String actionsToString() {
      String result = "";
      if (actions.size() == 0)
        return result;
      for (String action : actions)
        result += action + " ";
      return result.substring(0, result.length()-1);
    }

    public String event;
    public String nextState;
    public List<String> actions = new ArrayList<>();
  }
}
