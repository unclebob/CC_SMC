package smc.implementers;

import static java.lang.String.format;
import static smc.Utilities.commaList;

import java.util.Map;

import smc.generators.nestedSwitchCaseGenerator.NSCNode;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;

public class JavaNestedSwitchCaseImplementer implements NSCNodeVisitor {
  private String output = "";
  @SuppressWarnings("unused")
  private Map<String, String> flags;
  private String javaPackage = null;

  public JavaNestedSwitchCaseImplementer(Map<String, String> flags) {
    this.flags = flags;
    if (flags.containsKey("package"))
      javaPackage = flags.get("package");
  }

  public void visit(NSCNode.SwitchCaseNode switchCaseNode) {
    output += format("switch(%s) {\n", switchCaseNode.variableName);
    switchCaseNode.generateCases(this);
    output += "}\n";
  }

  public void visit(NSCNode.CaseNode caseNode) {
    output += format("case %s:\n", caseNode.caseName);
    caseNode.caseActionNode.accept(this);
    output += "break;\n";
  }

  public void visit(NSCNode.FunctionCallNode functionCallNode) {
    output += format("%s(", functionCallNode.functionName);
    if (functionCallNode.argument != null)
      functionCallNode.argument.accept(this);
    output += ");\n";
  }

  public void visit(NSCNode.EnumNode enumNode) {
    output += format("private enum %s {%s}\n", enumNode.name, commaList(enumNode.enumerators));
  }

  public void visit(NSCNode.StatePropertyNode statePropertyNode) {
    output += format("private State state = State.%s;\n", statePropertyNode.initialState);
    output += "private void setState(State s) {state = s;}\n";
  }

  public void visit(NSCNode.EventDelegatorsNode eventDelegatorsNode) {
    for (String event : eventDelegatorsNode.events)
      output += format("public void %s() {handleEvent(Event.%s);}\n", event, event);
  }

  public void visit(NSCNode.FSMClassNode fsmClassNode) {
    if (javaPackage != null)
      output += "package " + javaPackage + ";\n";

    String actionsName = fsmClassNode.actionsName;
    if (actionsName == null)
      output += format("public abstract class %s {\n", fsmClassNode.className);
    else
      output += format("public abstract class %s implements %s {\n",
          fsmClassNode.className, actionsName);

    output += "public abstract void unhandledTransition(String state, String event);\n";
    fsmClassNode.stateEnum.accept(this);
    fsmClassNode.eventEnum.accept(this);
    fsmClassNode.stateProperty.accept(this);
    fsmClassNode.delegators.accept(this);
    fsmClassNode.handleEvent.accept(this);
    if (actionsName == null) {
      for (String action : fsmClassNode.actions)
        output += format("protected abstract void %s();\n", action);
    }
    output += "}\n";
  }

  public void visit(NSCNode.HandleEventNode handleEventNode) {
    output += "private void handleEvent(Event event) {\n";
    handleEventNode.switchCase.accept(this);
    output += "}\n";
  }

  public void visit(NSCNode.EnumeratorNode enumeratorNode) {
    output += format("%s.%s", enumeratorNode.enumeration, enumeratorNode.enumerator);
  }

  public void visit(NSCNode.DefaultCaseNode defaultCaseNode) {
    output += "default: unhandledTransition(state.name(), event.name()); break;\n";
  }

  public String getOutput() {
    return output;
  }
}
