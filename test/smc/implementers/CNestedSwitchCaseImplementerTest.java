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

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static smc.Utilities.compressWhiteSpace;
import static smc.parser.ParserEvent.EOF;

public class CNestedSwitchCaseImplementerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;
  private Optimizer optimizer;
  private NSCGenerator generator;
  private CNestedSwitchCaseImplementer implementer;

  private void assertWhiteSpaceEquivalent(String generated, String expected) {
    assertThat(compressWhiteSpace(generated), equalTo(compressWhiteSpace(expected)));
  }

  @BeforeEach
  public void setUp() {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
    analyzer = new SemanticAnalyzer();
    optimizer = new Optimizer();
    generator = new NSCGenerator();
    implementer = new CNestedSwitchCaseImplementer(new HashMap<>());
  }

  private OptimizedStateMachine produceStateMachine(String fsmSyntax) {
    lexer.lex(fsmSyntax);
    parser.handleEvent(EOF, -1, -1);
    SemanticStateMachine ast = analyzer.analyze(builder.getFsm());
    return optimizer.optimize(ast);
  }


  @Test
  public void noAction_shouldBeError() {
    OptimizedStateMachine sm = produceStateMachine("""
            Initial: I
            Fsm: fsm
            {\
              I E I A\
            }""");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    assertThat(implementer.getErrors().size(), is(1));
    assertThat(implementer.getErrors().getFirst(), is(CNestedSwitchCaseImplementer.Error.NO_ACTION));
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
    assertWhiteSpaceEquivalent(implementer.getFsmHeader(), """
            #ifndef FSM_H
            #define FSM_H
            struct acts;
            struct fsm;
            struct fsm *make_fsm(struct acts*);
            void fsm_E(struct fsm*);
            #endif
            """);

    assertWhiteSpaceEquivalent(implementer.getFsmImplementation(), """
            #include <stdlib.h>
            #include "acts.h"
            #include "fsm.h"
            enum Event {E};
            enum State {I};
            struct fsm {
              enum State state;
              struct acts *actions;
            };
            struct fsm *make_fsm(struct acts* actions) {
              struct fsm *fsm = malloc(sizeof(struct fsm));
              fsm->actions = actions;
              fsm->state = I;
              return fsm;
            }
            static void setState(struct fsm *fsm, enum State state) {
              fsm->state = state;
            }
            static void A(struct fsm *fsm) {
              fsm->actions->A();
            }
            static void processEvent(enum State state, enum Event event, struct fsm *fsm, char *event_name) {
              switch (state) {
                case I:
                  switch (event) {
                    case E:
                      setState(fsm, I);
                      A(fsm);
                      break;
                    default:
                      (fsm->actions->unexpected_transition)("I", event_name);
                      break;
                  }
                  break;
              }
            }
            void fsm_E(struct fsm* fsm) {
              processEvent(fsm->state, E, fsm, "E");
            }
            """);
  }
}
