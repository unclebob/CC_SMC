package smc.implementers;

import org.junit.Before;
import org.junit.Test;
import smc.OptimizedStateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.generators.nestedSwitchCaseGenerator.NSCNode;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.SemanticStateMachine;
import smc.semanticAnalyzer.SemanticAnalyzer;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static smc.Utilities.compressWhiteSpace;
import static smc.parser.ParserEvent.EOF;

public class JavaNestedSwitchCaseImplementerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;
  private Optimizer optimizer;
  private NSCGenerator generator;
  private Map<String, String> emptyFlags = new HashMap<>();

  @Before
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

  @Test
  public void oneTransitionWithPackageAndActions() throws Exception {
    Map<String, String> flags = new HashMap<>();
    flags.put("package", "thePackage");
    JavaNestedSwitchCaseImplementer implementer = new JavaNestedSwitchCaseImplementer(flags);
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
      "package thePackage;\n" +
      "public abstract class fsm implements acts {\n" +
      "public abstract void unhandledTransition(String state, String event);\n" +
      "  private enum State {I}\n" +
      "  private enum Event {E}\n" +
      "  private State state = State.I;\n" +
      "" +
      "  private void setState(State s) {state = s;}\n" +
      "  public void E() {handleEvent(Event.E);}\n" +
      "  private void handleEvent(Event event) {\n" +
      "    switch(state) {\n" +
      "      case I:\n" +
      "        switch(event) {\n" +
      "          case E:\n" +
      "            setState(State.I);\n" +
      "            A();\n" +
      "            break;\n" +
      "          default: unhandledTransition(state.name(), event.name()); break;\n" +
      "        }\n" +
      "        break;\n" +
      "    }\n" +
      "  }\n" +
      "}\n");
  }

  private void assertWhitespaceEquivalent(String generated, String expected) {
    assertThat(compressWhiteSpace(generated), equalTo(compressWhiteSpace(expected)));
  }

  @Test
  public void oneTransitionWithActionsButNoPackage() throws Exception {
    JavaNestedSwitchCaseImplementer implementer = new JavaNestedSwitchCaseImplementer(emptyFlags);
    OptimizedStateMachine sm = produceStateMachine("" +
        "Initial: I\n" +
        "Fsm: fsm\n" +
        "Actions: acts\n" +
        "{" +
        "  I E I A" +
        "}");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    assertThat(implementer.getOutput(), startsWith("" +
      "public abstract class fsm implements acts {\n"));
  }

  @Test
  public void oneTransitionWithNoActionsAndNoPackage() throws Exception {
    JavaNestedSwitchCaseImplementer implementer = new JavaNestedSwitchCaseImplementer(emptyFlags);
    OptimizedStateMachine sm = produceStateMachine("" +
        "Initial: I\n" +
        "Fsm: fsm\n" +
        "{" +
        "  I E I A" +
        "}");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    String output = implementer.getOutput();
    assertThat(output, startsWith("" +
      "public abstract class fsm {\n"));
    assertThat(output, containsString("protected abstract void A();\n"));
  }

}
