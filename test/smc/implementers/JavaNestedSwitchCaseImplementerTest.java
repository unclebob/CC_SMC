package smc.implementers;


import org.junit.jupiter.api.BeforeEach;
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
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static smc.Utilities.compressWhiteSpace;
import static smc.parser.ParserEvent.EOF;

public class JavaNestedSwitchCaseImplementerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;
  private Optimizer optimizer;
  private NSCGenerator generator;
  private final Map<String, String> emptyFlags = new HashMap<>();

  @BeforeEach
  public void setUp() {
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
  public void oneTransitionWithPackageAndActions() {
    Map<String, String> flags = new HashMap<>();
    flags.put("package", "thePackage");
    JavaNestedSwitchCaseImplementer implementer = new JavaNestedSwitchCaseImplementer(flags);
    OptimizedStateMachine sm = produceStateMachine("""
            Initial: I
            Fsm: fsm
            Actions: acts
            {\
              I E I A\
            }""");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    assertWhitespaceEquivalent(implementer.getOutput(), """
            package thePackage;
            public abstract class fsm implements acts {
            public abstract void unhandledTransition(String state, String event);
              private enum State {I}
              private enum Event {E}
              private State state = State.I;
              private void setState(State s) {state = s;}
              public void E() {handleEvent(Event.E);}
              private void handleEvent(Event event) {
                switch(state) {
                  case I:
                    switch(event) {
                      case E:
                        setState(State.I);
                        A();
                        break;
                      default: unhandledTransition(state.name(), event.name()); break;
                    }
                    break;
                }
              }
            }
            """);
  }

  private void assertWhitespaceEquivalent(String generated, String expected) {
    assertThat(compressWhiteSpace(generated), equalTo(compressWhiteSpace(expected)));
  }

  @Test
  public void oneTransitionWithActionsButNoPackage() {
    JavaNestedSwitchCaseImplementer implementer = new JavaNestedSwitchCaseImplementer(emptyFlags);
    OptimizedStateMachine sm = produceStateMachine("""
            Initial: I
            Fsm: fsm
            Actions: acts
            {\
              I E I A\
            }""");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    assertThat(implementer.getOutput(), startsWith("public abstract class fsm implements acts {\n"));
  }

  @Test
  public void oneTransitionWithNoActionsAndNoPackage() {
    JavaNestedSwitchCaseImplementer implementer = new JavaNestedSwitchCaseImplementer(emptyFlags);
    OptimizedStateMachine sm = produceStateMachine("""
            Initial: I
            Fsm: fsm
            {\
              I E I A\
            }""");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    String output = implementer.getOutput();
    assertThat(output, startsWith("public abstract class fsm {\n"));
    assertThat(output, containsString("protected abstract void A();\n"));
  }

}
