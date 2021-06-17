package smc.generators.nestedSwitchCaseGenerator;

public interface NSCNodeVisitor {
  void visit(NSCNode.SwitchCaseNode switchCaseNode);
  void visit(NSCNode.CaseNode caseNode);
  void visit(NSCNode.FunctionCallNode functionCallNode);
  void visit(NSCNode.EnumNode enumNode);
  void visit(NSCNode.StatePropertyNode statePropertyNode);
  void visit(NSCNode.EventDelegatorsNode eventDelegatorsNode);
  void visit(NSCNode.FSMClassNode fsmClassNode);
  void visit(NSCNode.HandleEventNode handleEventNode);
  void visit(NSCNode.EnumeratorNode enumeratorNode);
  void visit(NSCNode.DefaultCaseNode defaultCaseNode);
}
