package smc.implementers;

import smc.Utilities;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static smc.generators.nestedSwitchCaseGenerator.NSCNode.*;

public class GoNestedSwitchCaseImplementer implements NSCNodeVisitor {
  private String fsmName;
  private String actionsName;
  private String output = "";
  private List<String> actions = new ArrayList<>();
  private List<Error> errors = new ArrayList<>();
  private List<String> states = new ArrayList<>();
  private Map<String, String> flags;

  public GoNestedSwitchCaseImplementer(Map<String, String> flags) {
    this.flags = flags;
  }

  public void visit(SwitchCaseNode switchCaseNode) {
    output += String.format("\tswitch %s {\n", switchCaseNode.variableName);
    switchCaseNode.generateCases(this);
    output += "}\n";
  }

  public void visit(CaseNode caseNode) {
    output += String.format("\tcase %s%s:\n", caseNode.switchName.toLowerCase(), caseNode.caseName);
    caseNode.caseActionNode.accept(this);
    output += "\n\n";
  }

  public void visit(FunctionCallNode functionCallNode) {
    output += String.format("%s(", functionCallNode.functionName);
    if (functionCallNode.argument != null) {
      functionCallNode.argument.accept(this);
    }
    output += ")\n";
  }

  public void visit(EnumNode enumNode) {
    output += String.format(
      "const (\n%s)\n\n",
      Utilities.iotaList(
        enumNode.name.toLowerCase() + "T",
        Utilities.addPrefix(enumNode.name.toLowerCase(), enumNode.enumerators)));
  }

  public void visit(StatePropertyNode statePropertyNode) {
    output += "state"+statePropertyNode.initialState;
  }

  public void visit(EventDelegatorsNode eventDelegatorsNode) {
    for (String event : eventDelegatorsNode.events) {
      output += String.format("func (f *%s) %s() { f.processEvent(event%s, \"%s\") }\n", fsmName, event, event, event);
    }
  }

  public void visit(FSMClassNode fsmClassNode) {
    if (fsmClassNode.actionsName == null) {
      errors.add(Error.NO_ACTIONS);
      return;
    }

    fsmName = fsmClassNode.className;
    actionsName = fsmClassNode.actionsName;
    actions = fsmClassNode.actions;
    states = fsmClassNode.states;

    output += String.format(
      "// Package %s is an auto-generated Finite State Machine.\n" +
      "// DO NOT EDIT.\n" +
      "package %s\n\n" +
      "// %s is the Finite State Machine.\n" +
      "type %s struct {\n" +
      "\tactions %s\n" +
      "\tstate stateT\n" +
      "}\n\n" +
      "// New returns a new %s.\n" +
      "func New(actions %s) *%s {\n" +
      "\t return &%s{actions: actions, state: ",
      fsmName.toLowerCase(), fsmName.toLowerCase(), fsmName, fsmName,
      actionsName, fsmName, actionsName, fsmName, fsmName);
    fsmClassNode.stateProperty.accept(this);
    output += "}\n}\n\n";

    fsmClassNode.delegators.accept(this);

    output += "type stateT int\n";
    fsmClassNode.stateEnum.accept(this);
    output += "type eventT int\n";
    fsmClassNode.eventEnum.accept(this);
    fsmClassNode.handleEvent.accept(this);
  }

  public void visit(HandleEventNode handleEventNode) {
    output += String.format(
      "func (f *%s) processEvent(event eventT, eventName string) {\n" +
      "\tstate := f.state\n" +
      "\tsetState := func(s stateT) { f.state = s; state = s }\n",
      fsmName);

    for (String action : actions) {
      output += String.format(
        "\t%s := func() { f.actions.%s() }\n",
        action, Utilities.capitalize(action));
    }
    output += "\n";

    for (String state : states) {
      output += String.format(
        "\tconst State%s = state%s\n",
        state, Utilities.capitalize(state));
    }
    output += "\n";

    handleEventNode.switchCase.accept(this);
    output += "}\n\n";
  }

  public void visit(EnumeratorNode enumeratorNode) {
    output += enumeratorNode.enumeration + enumeratorNode.enumerator;
  }

  public void visit(DefaultCaseNode defaultCaseNode) {
    output += String.format(
      "" +
      "\tdefault:\n" +
      "\t\tf.actions.UnexpectedTransition(\"%s\", eventName);\n\n",
      defaultCaseNode.state);
  }

  public String getOutput() {
    return output;
  }

  public List<Error> getErrors() {
    return errors;
  }

  public enum Error {NO_ACTIONS}
}
