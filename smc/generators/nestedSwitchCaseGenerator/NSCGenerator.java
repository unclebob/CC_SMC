package smc.generators.nestedSwitchCaseGenerator;

import smc.StateMachine;

import static smc.StateMachine.SubTransition;
import static smc.StateMachine.Transition;
import static smc.generators.nestedSwitchCaseGenerator.NSCNode.*;

public class NSCGenerator {

  private EnumNode stateEnumNode;
  private EnumNode eventEnumNode;
  private EventDelegatorsNode eventDelegatorsNode;
  private StatePropertyNode statePropertyNode;
  private HandleEventNode handleEventNode;
  private SwitchCaseNode stateSwitch;

  public NSCNode generate(StateMachine sm) {
    makeEventDelegators(sm);
    makeStatePropertyNode(sm);
    makeEnums(sm);
    makeStateSwitch();
    addStateCases(sm);
    makeHandleEventNode();
    return makeFsmNode(sm);
  }

  private void makeEventDelegators(StateMachine sm) {
    eventDelegatorsNode = new EventDelegatorsNode();
    eventDelegatorsNode.events = sm.events;
  }

  private void makeStatePropertyNode(StateMachine sm) {
    statePropertyNode = new StatePropertyNode();
    statePropertyNode.initialState = sm.header.initial;
  }

  private void makeEnums(StateMachine sm) {
    stateEnumNode = new EnumNode();
    stateEnumNode.name = "State";
    stateEnumNode.enumerators = sm.states;

    eventEnumNode = new EnumNode();
    eventEnumNode.name = "Event";
    eventEnumNode.enumerators = sm.events;
  }

  private void makeStateSwitch() {
    stateSwitch = new SwitchCaseNode();
    stateSwitch.variableName = "state";
  }

  private void makeHandleEventNode() {
    handleEventNode = new HandleEventNode();
    handleEventNode.switchCase = stateSwitch;
  }

  private FSMClassNode makeFsmNode(StateMachine sm) {
    FSMClassNode fsm = new FSMClassNode();
    fsm.className = sm.header.fsm;
    fsm.actionsName = sm.header.actions;
    fsm.stateEnum = stateEnumNode;
    fsm.eventEnum = eventEnumNode;
    fsm.delegators = eventDelegatorsNode;
    fsm.stateProperty = statePropertyNode;
    fsm.handleEvent = handleEventNode;
    return fsm;
  }

  private void addStateCases(StateMachine sm) {
    for (Transition t : sm.transitions)
      addStateCase(stateSwitch, t);
  }

  private void addStateCase(SwitchCaseNode stateSwitch, Transition t) {
    CaseNode stateCaseNode = new CaseNode();
    stateCaseNode.caseName = t.currentState;
    addEventCases(stateCaseNode, t);
    stateSwitch.caseNodes.add(stateCaseNode);
  }

  private void addEventCases(CaseNode stateCaseNode, Transition t) {
    SwitchCaseNode eventSwitch = new SwitchCaseNode();
    stateCaseNode.caseActionNode = eventSwitch;
    eventSwitch.variableName = "event";
    for (SubTransition st : t.subTransitions)
      addEventCase(stateCaseNode, eventSwitch, st);
  }

  private void addEventCase(CaseNode stateCaseNode, SwitchCaseNode eventSwitch, SubTransition st) {
    CaseNode eventCaseNode = new CaseNode();
    eventCaseNode.caseName = st.event;
    addActions(st, eventCaseNode);
    eventSwitch.caseNodes.add(eventCaseNode);
  }

  private void addActions(SubTransition st, CaseNode eventCaseNode) {
    CompositeNode actions = new CompositeNode();
    addSetStateNode(st.nextState, actions);
    for (String action : st.actions) {
      FunctionCallNode actionNode = new FunctionCallNode();
      actionNode.functionName = action;
      actions.add(actionNode);
    }
    eventCaseNode.caseActionNode = actions;
  }

  private void addSetStateNode(String stateName, CompositeNode actions) {
    FunctionCallNode setStateNode = new FunctionCallNode();
    setStateNode.functionName = "setState";
    NSCNode.EnumeratorNode enumeratorNode = new EnumeratorNode();
    enumeratorNode.enumeration = "State";
    enumeratorNode.enumerator = stateName;
    setStateNode.argument = enumeratorNode;
    actions.add(setStateNode);
  }
}
