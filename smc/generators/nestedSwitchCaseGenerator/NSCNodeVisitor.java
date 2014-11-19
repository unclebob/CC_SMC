package smc.generators.nestedSwitchCaseGenerator;

import static smc.generators.nestedSwitchCaseGenerator.NSCNode.*;

public interface NSCNodeVisitor {
  void visit(SwitchCaseNode switchCaseNode);
  void visit(CaseNode caseNode);
  void visit(FunctionCallNode functionCallNode);
  void visit(EnumNode enumNode);
  void visit(StatePropertyNode statePropertyNode);
  void visit(EventDelegatorsNode eventDelegatorsNode);
  void visit(FSMClassNode fsmClassNode);
  void visit(HandleEventNode handleEventNode);
  void visit(EnumeratorNode enumeratorNode);
}
