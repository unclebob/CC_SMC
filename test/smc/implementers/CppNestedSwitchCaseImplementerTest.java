package smc.implementers;

import org.junit.Before;
import org.junit.Test;
import smc.StateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.generators.nestedSwitchCaseGenerator.NSCNode;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.AbstractSyntaxTree;
import smc.semanticAnalyzer.SemanticAnalyzer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static smc.parser.ParserEvent.EOF;

public class CppNestedSwitchCaseImplementerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;
  private Optimizer optimizer;
  private NSCGenerator generator;

  @Before
  public void setUp() throws Exception {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
    analyzer = new SemanticAnalyzer();
    optimizer = new Optimizer();
    generator = new NSCGenerator();
  }

  private StateMachine produceStateMachine(String fsmSyntax) {
    lexer.lex(fsmSyntax);
    parser.handleEvent(EOF, -1, -1);
    AbstractSyntaxTree ast = analyzer.analyze(builder.getFsm());
    return optimizer.optimize(ast);
  }

  @Test
  public void noAction_shouldBeError() throws Exception {
    CppNestedSwitchCaseImplementer implementer = new CppNestedSwitchCaseImplementer();
    StateMachine sm = produceStateMachine("" +
      "Initial: I\n" +
      "Fsm: fsm\n" +
      "{" +
      "  I E I A" +
      "}");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    assertThat(implementer.getErrors().size(), is(1));
    assertThat(implementer.getErrors().get(0), is(CppNestedSwitchCaseImplementer.Error.NO_ACTION));
  }

  @Test
  public void oneTransition() throws Exception {
    CppNestedSwitchCaseImplementer implementer = new CppNestedSwitchCaseImplementer();
    StateMachine sm = produceStateMachine("" +
      "Initial: I\n" +
      "Fsm: fsm\n" +
      "Actions: acts\n" +
      "{" +
      "  I E I A" +
      "}");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);

    assertThat(implementer.getOutput(), equalTo("" +
      "#ifndef FSM_H\n" +
      "#define FSM_H\n" +
      "\n" +
      "#include \"acts.h\"\n" +
      "\n" +
      "class fsm : public acts {\n" +
      "public:\n" +
      "\tfsm()\n" +
      "\t: state(State_I)\n" +
      "\t{}\n\n" +
      "\tvoid E() {processEvent(Event_E, \"E\");}\n" +
      "\n" +
      "private:\n" +
      "\tenum State {State_I};\n" +
      "\tState state;\n" +
      "\tvoid setState(State s) {state=s;}\n" +
      "\tenum Event {Event_E};\n" +
      "\tvoid processEvent(Event event, const char* eventName) {\n" +
      "switch (state) {\n" +
      "case State_I:\n" +
      "switch (event) {\n" +
      "case Event_E:\n" +
      "setState(State_I);\n" +
      "A();\n" +
      "break;\n" +
      "\n" +
      "default:\n" +
      "unexpected_transition(\"I\", eventName);\n" +
      "break;\n" +
      "}\n" +
      "break;\n\n" +
      "}\n" +
      "}\n\n" +
      "};\n" +
      "\n" +
      "#endif\n"));
  }
}
