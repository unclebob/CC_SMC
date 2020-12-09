package smc.generators.nestedSwitchCaseGenerator;

import smc.OptimizedStateMachine;

public class NSCGenerator {
  private NSCNode.EnumNode stateEnumNode;
  private NSCNode.EnumNode eventEnumNode;
  private NSCNode.EventDelegatorsNode eventDelegatorsNode;
  private NSCNode.StatePropertyNode statePropertyNode;
  private NSCNode.HandleEventNode handleEventNode;
  private NSCNode.SwitchCaseNode stateSwitch;

  public NSCNode generate(OptimizedStateMachine sm) {
    eventDelegatorsNode = new NSCNode.EventDelegatorsNode(sm.events);
    statePropertyNode = new NSCNode.StatePropertyNode(sm.header.initial);
    stateEnumNode = new NSCNode.EnumNode("State", sm.states);
    eventEnumNode = new NSCNode.EnumNode("Event", sm.events);
    stateSwitch = new NSCNode.SwitchCaseNode("state");
    addStateCases(sm);
    handleEventNode = new NSCNode.HandleEventNode(stateSwitch);
    return makeFsmNode(sm);
  }

  private NSCNode.FSMClassNode makeFsmNode(OptimizedStateMachine sm) {
    NSCNode.FSMClassNode fsm = new NSCNode.FSMClassNode();
    fsm.className = sm.header.fsm;
    fsm.actionsName = sm.header.actions;
    fsm.stateEnum = stateEnumNode;
    fsm.eventEnum = eventEnumNode;
    fsm.delegators = eventDelegatorsNode;
    fsm.stateProperty = statePropertyNode;
    fsm.handleEvent = handleEventNode;
    fsm.actions = sm.actions;
    return fsm;
  }

  private void addStateCases(OptimizedStateMachine sm) {
    for (OptimizedStateMachine.Transition t : sm.transitions)
      addStateCase(stateSwitch, t);
  }

  private void addStateCase(NSCNode.SwitchCaseNode stateSwitch, OptimizedStateMachine.Transition t) {
    NSCNode.CaseNode stateCaseNode = new NSCNode.CaseNode("State", t.currentState);
    addEventCases(stateCaseNode, t);
    stateSwitch.caseNodes.add(stateCaseNode);
  }

  private void addEventCases(NSCNode.CaseNode stateCaseNode, OptimizedStateMachine.Transition t) {
    NSCNode.SwitchCaseNode eventSwitch = new NSCNode.SwitchCaseNode("event");
    stateCaseNode.caseActionNode = eventSwitch;
    for (OptimizedStateMachine.SubTransition st : t.subTransitions)
      addEventCase(eventSwitch, st);
    eventSwitch.caseNodes.add(new NSCNode.DefaultCaseNode(t.currentState));
  }

  private void addEventCase(NSCNode.SwitchCaseNode eventSwitch, OptimizedStateMachine.SubTransition st) {
    NSCNode.CaseNode eventCaseNode = new NSCNode.CaseNode("Event", st.event);
    addActions(st, eventCaseNode);
    eventSwitch.caseNodes.add(eventCaseNode);
  }

  private void addActions(OptimizedStateMachine.SubTransition st, NSCNode.CaseNode eventCaseNode) {
    NSCNode.CompositeNode actions = new NSCNode.CompositeNode();
    addSetStateNode(st.nextState, actions);
    for (String action : st.actions)
      actions.add(new NSCNode.FunctionCallNode(action));

    eventCaseNode.caseActionNode = actions;
  }

  private void addSetStateNode(String stateName, NSCNode.CompositeNode actions) {
    NSCNode.EnumeratorNode enumeratorNode = new NSCNode.EnumeratorNode("State", stateName);
    NSCNode.FunctionCallNode setStateNode = new NSCNode.FunctionCallNode("setState", enumeratorNode);
    actions.add(setStateNode);
  }
}
