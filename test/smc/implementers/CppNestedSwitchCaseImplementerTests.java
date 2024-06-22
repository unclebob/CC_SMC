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
    public void noActions_shouldBeError() {
      OptimizedStateMachine sm = produceStateMachine("""
              Initial: I
              Fsm: fsm
              {\
                I E I A\
              }""");
      NSCNode generatedFsm = generator.generate(sm);
      generatedFsm.accept(implementer);
      assertThat(implementer.getErrors().size(), is(1));
      assertThat(implementer.getErrors().getFirst(), is(CppNestedSwitchCaseImplementer.Error.NO_ACTIONS));
    }
    @Test
    public void oneTransition() {
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
              #ifndef FSM_H
              #define FSM_H
              #include "acts.h"
              class fsm : public acts {
              public:
                fsm()
                : state(State_I)
                {}
                void E() {processEvent(Event_E, "E");}
              private:
                enum State {State_I};
                State state;
                void setState(State s) {state=s;}
                enum Event {Event_E};
                void processEvent(Event event, const char* eventName) {
                  switch (state) {
                    case State_I:
                      switch (event) {
                        case Event_E:
                          setState(State_I);
                          A();
                          break;
                        default:
                          unexpected_transition("I", eventName);
                          break;
                      }
                      break;
                  }
                }
              };
              #endif
              """);
    }
  } // no flags
}
