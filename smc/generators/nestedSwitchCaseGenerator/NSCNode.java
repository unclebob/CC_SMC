package smc.generators.nestedSwitchCaseGenerator;

import java.util.ArrayList;
import java.util.List;

public interface NSCNode {
  public void accept(NSCNodeVisitor visitor);

  public static class SwitchCaseNode implements NSCNode {
    public String variableName;
    public List<CaseNode> caseNodes = new ArrayList<>();

    public void accept(NSCNodeVisitor visitor) {
      visitor.visit(this);
    }

    public void generateCases(NSCNodeVisitor visitor) {
      for (CaseNode c : caseNodes) {
        c.accept(visitor);
      }
    }
  }

  public static class CaseNode implements NSCNode {
    public String caseName;
    public NSCNode caseActionNode;

    public void accept(NSCNodeVisitor visitor) {
      visitor.visit(this);
    }
  }

  public class FunctionCallNode implements NSCNode {
    public String functionName;
    public NSCNode argument;

    public void accept(NSCNodeVisitor visitor) {
      visitor.visit(this);
    }
  }

  public class CompositeNode implements NSCNode {
    private List<NSCNode> nodes = new ArrayList<>();

    public void accept(NSCNodeVisitor visitor) {
      for (NSCNode node : nodes)
        node.accept(visitor);
    }

    public void add(NSCNode node) {
      nodes.add(node);
    }
  }

  public class EnumNode implements NSCNode {
    public String name;
    public List<String> enumerators;

    public void accept(NSCNodeVisitor visitor) {
      visitor.visit(this);
    }
  }

  public class StatePropertyNode implements NSCNode {
    public String initialState;

    public void accept(NSCNodeVisitor visitor) {
      visitor.visit(this);
    }
  }

  public class EventDelegatorsNode implements NSCNode {
    public List<String> events;

    public void accept(NSCNodeVisitor visitor) {
      visitor.visit(this);
    }
  }

  public class FSMClassNode implements NSCNode {
    public EventDelegatorsNode delegators;
    public EnumNode eventEnum;
    public EnumNode stateEnum;
    public StatePropertyNode stateProperty;
    public HandleEventNode handleEvent;
    public String className;
    public String actionsName;

    public void accept(NSCNodeVisitor visitor) {
      visitor.visit(this);
    }
  }

  public class HandleEventNode implements NSCNode {
    public SwitchCaseNode switchCase;

    public void accept(NSCNodeVisitor visitor) {
      visitor.visit(this);
    }
  }

  public class EnumeratorNode implements NSCNode {
    public String enumeration;
    public String enumerator;

    public void accept(NSCNodeVisitor visitor) {
      visitor.visit(this);
    }
  }
}
