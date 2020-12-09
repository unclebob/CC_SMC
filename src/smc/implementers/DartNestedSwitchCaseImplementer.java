package smc.implementers;

import smc.Utilities;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static smc.generators.nestedSwitchCaseGenerator.NSCNode.*;

public class DartNestedSwitchCaseImplementer implements NSCNodeVisitor {
  private String fsmName;
  private String actionsName;
  private String output = "";
  private List<Error> errors = new ArrayList<>();
  private Map<String, String> flags;

  public DartNestedSwitchCaseImplementer(Map<String, String> flags) {
    this.flags = flags;
  }

  public void visit(SwitchCaseNode switchCaseNode) {
    output += String.format("switch (%s) {\n", switchCaseNode.variableName);
    switchCaseNode.generateCases(this);
    output += "}\n";
  }

  public void visit(CaseNode caseNode) {
    output += String.format("case %s.%s:\n",caseNode.switchName, caseNode.caseName);
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
      "\nenum %s {%s}\n\n",
      enumNode.name,
      Utilities.commaList(enumNode.enumerators));
  }

  public void visit(StatePropertyNode statePropertyNode) {
    output += "State."+statePropertyNode.initialState;
  }

  public void visit(EventDelegatorsNode eventDelegatorsNode) {
    for (String event : eventDelegatorsNode.events) {
      output += String.format("\t%s() {_processEvent(Event.%s, \"%s\");}\n", event, event, event);
    }
  }

  public void visit(FSMClassNode fsmClassNode) {
    if (fsmClassNode.actionsName == null) {
      errors.add(Error.NO_ACTIONS);
      return;
    }

    fsmName = fsmClassNode.className;
    actionsName = fsmClassNode.actionsName;

    output += "import 'package:meta/meta.dart';\n\n";
    output += String.format("import '%s.dart';\n", actionsName);

    fsmClassNode.stateEnum.accept(this);
    fsmClassNode.eventEnum.accept(this);

    output += String.format("\n" +
      "abstract class %s extends %s {\n" +
      "\tState state;\n\n" +
      "\t%s({@required this.state = ", fsmName, actionsName,fsmName);
    fsmClassNode.stateProperty.accept(this);
    output += "})\n\t : assert(state != null);\n\n";

    fsmClassNode.delegators.accept(this);
    output += "\n\tsetState(State s) {state=s;}\n\n";

    fsmClassNode.handleEvent.accept(this);
    output += "}\n";
  }

  public void visit(HandleEventNode handleEventNode) {
    output += "\t_processEvent(final Event event, final String eventName) {\n";
    handleEventNode.switchCase.accept(this);
    output += "}\n\n";
  }

  public void visit(EnumeratorNode enumeratorNode) {
    output += enumeratorNode.enumeration + "." + enumeratorNode.enumerator;
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
