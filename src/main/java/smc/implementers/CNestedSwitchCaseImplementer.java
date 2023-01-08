package smc.implementers;

import smc.Utilities;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static smc.generators.nestedSwitchCaseGenerator.NSCNode.*;

public class CNestedSwitchCaseImplementer implements NSCNodeVisitor {
  private String fsmName;
  private String actionsName;
  private String fsmHeader = "";
  private String fsmImplementation = "";
  private List<Error> errors = new ArrayList<>();
  private Map<String, String> flags;

  public CNestedSwitchCaseImplementer(Map<String, String> flags) {
    this.flags = flags;
  }

  public void visit(SwitchCaseNode switchCaseNode) {
    fsmImplementation += String.format("switch (%s) {\n", switchCaseNode.variableName);
    switchCaseNode.generateCases(this);
    fsmImplementation += "}\n";
  }

  public void visit(CaseNode caseNode) {
    fsmImplementation += String.format("case %s:\n", caseNode.caseName);
    caseNode.caseActionNode.accept(this);
    fsmImplementation += "break;\n\n";
  }

  public void visit(FunctionCallNode functionCallNode) {
    fsmImplementation += String.format("%s(fsm", functionCallNode.functionName);
    if (functionCallNode.argument != null) {
      fsmImplementation += ", ";
      functionCallNode.argument.accept(this);
    }
    fsmImplementation += ");\n";
  }

  public void visit(EnumNode enumNode) {
    fsmImplementation += String.format("enum %s {%s};\n", enumNode.name, Utilities.commaList(enumNode.enumerators));
  }

  public void visit(StatePropertyNode statePropertyNode) {
    fsmImplementation +=
      String.format("struct %s *make_%s(struct %s* actions) {\n", fsmName, fsmName, actionsName) +
        String.format("\tstruct %s *fsm = malloc(sizeof(struct %s));\n", fsmName, fsmName) +
        String.format("\tfsm->actions = actions;\n") +
        String.format("\tfsm->state = %s;\n", statePropertyNode.initialState) +
        String.format("\treturn fsm;\n") + "}\n\n";

    fsmImplementation += String.format("" +
      "static void setState(struct %s *fsm, enum State state) {\n" +
      "\tfsm->state = state;\n" +
      "}\n\n", fsmName);
  }

  public void visit(EventDelegatorsNode eventDelegatorsNode) {
    for (String event : eventDelegatorsNode.events) {
      fsmHeader += String.format("void %s_%s(struct %s*);\n", fsmName, event, fsmName);

      fsmImplementation += String.format("" +
        "void %s_%s(struct %s* fsm) {\n" +
        "\tprocessEvent(fsm->state, %s, fsm, \"%s\");\n" +
        "}\n", fsmName, event, fsmName, event, event);
    }
  }

  public void visit(FSMClassNode fsmClassNode) {
    if (fsmClassNode.actionsName == null) {
      errors.add(Error.NO_ACTION);
      return;
    }
    actionsName = fsmClassNode.actionsName;
    fsmName = fsmClassNode.className;

    fsmImplementation += "#include <stdlib.h>\n";
    fsmImplementation += String.format("#include \"%s.h\"\n", actionsName);
    fsmImplementation += String.format("#include \"%s.h\"\n\n", fsmName);
    fsmClassNode.eventEnum.accept(this);
    fsmClassNode.stateEnum.accept(this);

    fsmImplementation += String.format("\n" +
      "struct %s {\n" +
      "\tenum State state;\n" +
      "\tstruct %s *actions;\n" +
      "};\n\n", fsmName, actionsName);

    fsmClassNode.stateProperty.accept(this);

    for (String action : fsmClassNode.actions) {
      fsmImplementation += String.format("" +
        "static void %s(struct %s *fsm) {\n" +
        "\tfsm->actions->%s();\n" +
        "}\n\n", action, fsmName, action);
    }
    fsmClassNode.handleEvent.accept(this);

    String includeGuard = fsmName.toUpperCase();
    fsmHeader += String.format("#ifndef %s_H\n#define %s_H\n\n", includeGuard, includeGuard);
    fsmHeader += String.format("struct %s;\n", actionsName);
    fsmHeader += String.format("struct %s;\n", fsmName);
    fsmHeader += String.format("struct %s *make_%s(struct %s*);\n", fsmName, fsmName, actionsName);
    fsmClassNode.delegators.accept(this);
    fsmHeader += "#endif\n";
  }

  public void visit(HandleEventNode handleEventNode) {
    fsmImplementation += String.format("" +
        "static void processEvent(enum State state, enum Event event, struct %s *fsm, char *event_name) {\n",
      fsmName);
    handleEventNode.switchCase.accept(this);
    fsmImplementation += "}\n\n";
  }

  public void visit(EnumeratorNode enumeratorNode) {
    fsmImplementation += enumeratorNode.enumerator;
  }

  public void visit(DefaultCaseNode defaultCaseNode) {
    fsmImplementation += String.format("" +
      "default:\n" +
      "(fsm->actions->unexpected_transition)(\"%s\", event_name);\n" +
      "break;\n", defaultCaseNode.state);
  }

  public String getFsmHeader() {
    return fsmHeader;
  }

  public String getFsmImplementation() {
    return fsmImplementation;
  }

  public List<Error> getErrors() {
    return errors;
  }

  public enum Error {NO_ACTION}
}
