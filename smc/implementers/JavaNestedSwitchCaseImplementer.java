package smc.implementers;

import smc.generators.nestedSwitchCaseGenerator.NSCNode;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;

import java.util.List;

public class JavaNestedSwitchCaseImplementer implements NSCNodeVisitor {
  private String output = "";

  private String commaList(List<String> names) {
    String commaList = "";
    boolean first = true;
    for (String name : names) {
      commaList += (first ? "" : ",") + name;
      first = false;
    }
    return commaList;
  }

  public void visit(NSCNode.SwitchCaseNode switchCaseNode) {
    output += String.format("switch(%s) {\n", switchCaseNode.variableName);
    switchCaseNode.generateCases(this);
    output += "}\n";
  }

  public void visit(NSCNode.CaseNode caseNode) {
    output += String.format("case %s:\n", caseNode.caseName);
    caseNode.caseActionNode.accept(this);
    output += "break;\n";
  }

  public void visit(NSCNode.FunctionCallNode functionCallNode) {
    output += String.format("%s(", functionCallNode.functionName);
    if (functionCallNode.argument != null)
      functionCallNode.argument.accept(this);
    output += ");\n";
  }

  public void visit(NSCNode.EnumNode enumNode) {
    output += String.format("private enum %s {%s}\n", enumNode.name, commaList(enumNode.enumerators));

  }

  public void visit(NSCNode.StatePropertyNode statePropertyNode) {
    output += String.format("State state = State.%s;\n", statePropertyNode.initialState);
    output += "void setState(State s) {state = s;}\n";
  }

  public void visit(NSCNode.EventDelegatorsNode eventDelegatorsNode) {
    for (String event : eventDelegatorsNode.events)
      output += String.format("void %s() {handleEvent(Event.%s);}\n", event, event);
  }

  public void visit(NSCNode.FSMClassNode fsmClassNode) {
    output += String.format("public class %s implements %s {\n", fsmClassNode.className, fsmClassNode.actionsName);
    fsmClassNode.stateEnum.accept(this);
    fsmClassNode.eventEnum.accept(this);
    fsmClassNode.stateProperty.accept(this);
    fsmClassNode.delegators.accept(this);
    fsmClassNode.handleEvent.accept(this);
    output += "}\n";
  }

  public void visit(NSCNode.HandleEventNode handleEventNode) {
    output += "void handleEvent(Event event) {\n";
    handleEventNode.switchCase.accept(this);
    output += "}\n";
  }

  public void visit(NSCNode.EnumeratorNode enumeratorNode) {
    output += String.format("%s.%s", enumeratorNode.enumeration, enumeratorNode.enumerator);
  }

  public String getOutput() {
    return output;
  }
}
