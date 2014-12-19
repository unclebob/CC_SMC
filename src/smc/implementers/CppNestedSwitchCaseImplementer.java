package smc.implementers;

import smc.Utilities;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static smc.generators.nestedSwitchCaseGenerator.NSCNode.*;

public class CppNestedSwitchCaseImplementer implements NSCNodeVisitor {
  private String fsmName;
  private String actionsName;
  private String output = "";
  private List<Error> errors = new ArrayList<>();
  private Map<String, String> flags;

  public CppNestedSwitchCaseImplementer(Map<String, String> flags) {
    this.flags = flags;
  }

  public void visit(SwitchCaseNode switchCaseNode) {
    output += String.format("switch (%s) {\n", switchCaseNode.variableName);
    switchCaseNode.generateCases(this);
    output += "}\n";
  }

  public void visit(CaseNode caseNode) {
    output += String.format("case %s_%s:\n",caseNode.switchName, caseNode.caseName);
    caseNode.caseActionNode.accept(this);
    output += "break;\n\n";
  }

  public void visit(FunctionCallNode functionCallNode) {
    output += String.format("%s(", functionCallNode.functionName);
    if (functionCallNode.argument != null) {
      functionCallNode.argument.accept(this);
    }
    output += ");\n";
  }

  public void visit(EnumNode enumNode) {
    output += String.format(
      "\tenum %s {%s};\n",
      enumNode.name,
      Utilities.commaList(Utilities.addPrefix(enumNode.name + "_", enumNode.enumerators)));
  }

  public void visit(StatePropertyNode statePropertyNode) {
    output += "State_"+statePropertyNode.initialState;
  }

  public void visit(EventDelegatorsNode eventDelegatorsNode) {
    for (String event : eventDelegatorsNode.events) {
      output += String.format("\tvoid %s() {processEvent(Event_%s, \"%s\");}\n", event, event, event);
    }
  }

  public void visit(FSMClassNode fsmClassNode) {
    if (fsmClassNode.actionsName == null) {
      errors.add(Error.NO_ACTIONS);
      return;
    }

    fsmName = fsmClassNode.className;
    String includeGuard = fsmName.toUpperCase();
    output += String.format("#ifndef %s_H\n#define %s_H\n\n", includeGuard, includeGuard);

    actionsName = fsmClassNode.actionsName;
    output += String.format("#include \"%s.h\"\n", actionsName);

    output += String.format("\n" +
      "class %s : public %s {\n" +
      "public:\n" +
      "\t%s()\n\t: state(", fsmName, actionsName,fsmName);
    fsmClassNode.stateProperty.accept(this);
    output += ")\n\t{}\n\n";

    fsmClassNode.delegators.accept(this);
    output += "\nprivate:\n";
    fsmClassNode.stateEnum.accept(this);
    output += "\tState state;\n";
    output += "\tvoid setState(State s) {state=s;}\n";
    fsmClassNode.eventEnum.accept(this);
    fsmClassNode.handleEvent.accept(this);

    output += "};\n\n";
    output += "#endif\n";
  }

  public void visit(HandleEventNode handleEventNode) {
    output += "\tvoid processEvent(Event event, const char* eventName) {\n";
    handleEventNode.switchCase.accept(this);
    output += "}\n\n";
  }

  public void visit(EnumeratorNode enumeratorNode) {
    output += enumeratorNode.enumeration + "_" + enumeratorNode.enumerator;
  }

  public void visit(DefaultCaseNode defaultCaseNode) {
    output += String.format("" +
      "default:\n" +
      "unexpected_transition(\"%s\", eventName);\n" +
      "break;\n", defaultCaseNode.state);
  }

  public String getOutput() {
    return output;
  }

  public List<Error> getErrors() {
    return errors;
  }

  public enum Error {NO_ACTIONS}
}
