package smc.implementers;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import smc.OptimizedStateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.generators.nestedSwitchCaseGenerator.NSCNode;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.SemanticAnalyzer;
import smc.semanticAnalyzer.SemanticStateMachine;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static smc.Utilities.compressWhiteSpace;
import static smc.parser.ParserEvent.EOF;

public class CppNestedSwitchCaseImplementerTests {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;
  private Optimizer optimizer;
  private NSCGenerator generator;
  private CppNestedSwitchCaseImplementer implementer;

  @BeforeEach
  public void setUp() throws Exception {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
    analyzer = new SemanticAnalyzer();
    optimizer = new Optimizer();
    generator = new NSCGenerator();
  }

  private OptimizedStateMachine produceStateMachine(String fsmSyntax) {
    lexer.lex(fsmSyntax);
    parser.handleEvent(EOF, -1, -1);
    SemanticStateMachine ast = analyzer.analyze(builder.getFsm());
    return optimizer.optimize(ast);
  }

  private void assertWhitespaceEquivalent(String generatedCode, String expected) {
    assertThat(compressWhiteSpace(generatedCode), equalTo(compressWhiteSpace(expected)));
  }

  @Nested
  public class TestsWithNoFlags {

    @BeforeEach
    public void setup() {
      implementer = new CppNestedSwitchCaseImplementer(new HashMap<>());
    }

    @Test
    public void noActions_shouldBeError() throws Exception {
      OptimizedStateMachine sm = produceStateMachine("" +
        "Initial: I\n" +
        "Fsm: fsm\n" +
        "{" +
        "  I E I A" +
        "}");
      NSCNode generatedFsm = generator.generate(sm);
      generatedFsm.accept(implementer);
      assertThat(implementer.getErrors().size(), is(1));
      assertThat(implementer.getErrors().get(0), is(CppNestedSwitchCaseImplementer.Error.NO_ACTIONS));
    }
    @Test
    public void oneTransition() throws Exception {
      OptimizedStateMachine sm = produceStateMachine("" +
        "Initial: I\n" +
        "Fsm: fsm\n" +
        "Actions: acts\n" +
        "{" +
        "  I E I A" +
        "}");
      NSCNode generatedFsm = generator.generate(sm);
      generatedFsm.accept(implementer);

      assertWhitespaceEquivalent(implementer.getOutput(), "" +
        "#ifndef FSM_H\n" +
        "#define FSM_H\n" +
        "#include \"acts.h\"\n" +
        "" +
        "class fsm : public acts {\n" +
        "public:\n" +
        "  fsm()\n" +
        "  : state(State_I)\n" +
        "  {}\n" +
        "" +
        "  void E() {processEvent(Event_E, \"E\");}\n" +
        "" +
        "private:\n" +
        "  enum State {State_I};\n" +
        "  State state;\n" +
        "" +
        "  void setState(State s) {state=s;}\n" +
        "" +
        "  enum Event {Event_E};\n" +
        "" +
        "  void processEvent(Event event, const char* eventName) {\n" +
        "    switch (state) {\n" +
        "      case State_I:\n" +
        "        switch (event) {\n" +
        "          case Event_E:\n" +
        "            setState(State_I);\n" +
        "            A();\n" +
        "            break;\n" +
        "" +
        "          default:\n" +
        "            unexpected_transition(\"I\", eventName);\n" +
        "            break;\n" +
        "        }\n" +
        "        break;\n" +
        "    }\n" +
        "  }\n" +
        "};\n" +
        "#endif\n");
    }
  } // no flags
}
