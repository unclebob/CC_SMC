package smc.optimizer;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import smc.OptimizedStateMachine;
import smc.lexer.Lexer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.SemanticAnalyzer;
import smc.semanticAnalyzer.SemanticStateMachine;

import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static smc.Utilities.compressWhiteSpace;
import static smc.parser.ParserEvent.EOF;

@RunWith(HierarchicalContextRunner.class)
public class OptimizerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;
  private Optimizer optimizer;
  private OptimizedStateMachine optimizedStateMachine;

  @Before
  public void setUp() throws Exception {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
    analyzer = new SemanticAnalyzer();
    optimizer = new Optimizer();
  }

  private OptimizedStateMachine produceStateMachineWithHeader(String s) {
    String fsmSyntax = "fsm:f initial:i actions:a " + s;
    return produceStateMachine(fsmSyntax);
  }

  private OptimizedStateMachine produceStateMachine(String fsmSyntax) {
    lexer.lex(fsmSyntax);
    parser.handleEvent(EOF, -1, -1);
    SemanticStateMachine ast = analyzer.analyze(builder.getFsm());
    return optimizer.optimize(ast);
  }

  private void assertOptimization(String syntax, String stateMachine) {
    optimizedStateMachine = produceStateMachineWithHeader(syntax);
    assertThat(
      compressWhiteSpace(optimizedStateMachine.transitionsToString()),
      equalTo(compressWhiteSpace(stateMachine)));
  }

  public class BasicOptimizerFunctions {
    @Test
    public void header() throws Exception {
      OptimizedStateMachine sm = produceStateMachineWithHeader("{i e i -}");
      assertThat(sm.header.fsm, equalTo("f"));
      assertThat(sm.header.initial, equalTo("i"));
      assertThat(sm.header.actions, equalTo("a"));
    }

    @Test
    public void statesArePreserved() throws Exception {
      OptimizedStateMachine sm = produceStateMachineWithHeader("{i e s - s e i -}");
      assertThat(sm.states, contains("i", "s"));
    }

    @Test
    public void abstractStatesAreRemoved() throws Exception {
      OptimizedStateMachine sm = produceStateMachineWithHeader("{(b) - - - i:b e i -}");
      assertThat(sm.states, not(hasItems("b")));
    }

    @Test
    public void eventsArePreserved() throws Exception {
      OptimizedStateMachine sm = produceStateMachineWithHeader("{i e1 s - s e2 i -}");
      assertThat(sm.events, contains("e1", "e2"));
    }

    @Test
    public void actionsArePreserved() throws Exception {
      OptimizedStateMachine sm = produceStateMachineWithHeader("{i e1 s a1 s e2 i a2}");
      assertThat(sm.actions, contains("a1", "a2"));
    }

    @Test
    public void simpleStateMachine() throws Exception {
      assertOptimization(
        "" +
          "{i e i a1}",

        "" +
          "i {\n" +
          "  e i {a1}\n" +
          "}\n");
        assertThat(optimizedStateMachine.transitions, hasSize(1));

    }
  } // Basic Optimizer Functions

  public class EntryAndExitActions {
    @Test
    public void entryFunctionsAdded() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  i e s a1" +
          "  i e2 s a2" +
          "  s <n1 <n2 e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {n1 n2 a1}\n" +
          "  e2 s {n1 n2 a2}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n");
    }

    @Test
    public void exitFunctionsAdded() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  i >x2 >x1 e s a1" +
          "  i e2 s a2" +
          "  s e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {x2 x1 a1}\n" +
          "  e2 s {x2 x1 a2}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n");
    }

    @Test
    public void firstSuperStateEntryAndExitActionsAreAdded() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  (ib) >ibx1 >ibx2 - - -" +
          "  (sb) <sbn1 <sbn2 - - -" +
          "  i:ib >x e s a" +
          "  s:sb <n e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {x ibx1 ibx2 sbn1 sbn2 n a}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n");
    }

    @Test
    public void multipleSuperStateEntryAndExitActionsAreAdded() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  (ib1) >ib1x - - -" +
          "  (ib2) : ib1 >ib2x - - -" +
          "  (sb1) <sb1n- - -" +
          "  (sb2) :sb1 <sb2n- - -" +
          "  i:ib2 >x e s a" +
          "  s:sb2 <n e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {x ib2x ib1x sb1n sb2n n a}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n");
    }

    @Test
    public void diamondSuperStateEntryAndExitActionsAreAdded() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  (ib1) >ib1x - - -" +
          "  (ib2) : ib1 >ib2x - - -" +
          "  (ib3) : ib1 >ib3x - - -" +
          "  (sb1) <sb1n - - -" +
          "  (sb2) :sb1 <sb2n - - -" +
          "  (sb3) :sb1 <sb3n - - -" +
          "  i:ib2 :ib3 >x e s a" +
          "  s :sb2 :sb3 <n e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {x ib3x ib2x ib1x sb1n sb2n sb3n n a}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n");
    }
  } // Entry and Exit Actions

  public class superStateTransitions {
    @Test
    public void simpleInheritanceOfTransitions() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  (b) be s ba" +
          "  i:b e s a" +
          "  s e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {a}\n" +
          "  be s {ba}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n"
      );
    }

    @Test
    public void deepInheritanceOfTransitions() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  (b1) {" +
          "    b1e1 s b1a1" +
          "    b1e2 s b1a2" +
          "  }" +
          "  (b2):b1 b2e s b2a" +
          "  i:b2 e s a" +
          "  s e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {a}\n" +
          "  b2e s {b2a}\n" +
          "  b1e1 s {b1a1}\n" +
          "  b1e2 s {b1a2}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n"
      );
    }

    @Test
    public void multipleInheritanceOfTransitions() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  (b1) b1e s b1a" +
          "  (b2) b2e s b2a" +
          "  i:b1 :b2 e s a" +
          "  s e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {a}\n" +
          "  b2e s {b2a}\n" +
          "  b1e s {b1a}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n"
      );
    }

    @Test
    public void diamondInheritanceOfTransitions() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  (b) be s ba" +
          "  (b1):b b1e s b1a" +
          "  (b2):b b2e s b2a" +
          "  i:b1 :b2 e s a" +
          "  s e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {a}\n" +
          "  b2e s {b2a}\n" +
          "  b1e s {b1a}\n" +
          "  be s {ba}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n"
      );
    }

    @Test
    public void overridingTransitions() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  (b) e s2 a2" +
          "  i:b e s a" +
          "  s e i -" +
          "  s2 e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {a}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n" +
          "s2 {\n" +
          "  e i {}\n" +
          "}\n"
      );
    }

    @Test
    public void eliminationOfDuplicateTransitions() throws Exception {
      assertOptimization(
        "" +
          "{" +
          "  (b) e s a" +
          "  i:b e s a" +
          "  s e i -" +
          "}",
        "" +
          "i {\n" +
          "  e s {a}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n"
      );

    }
  }// Super State Transitions

  public class AcceptanceTests {
    @Test
    public void turnstyle3() throws Exception {
      OptimizedStateMachine sm = produceStateMachine(
        "" +
          "Actions: Turnstile\n" +
          "FSM: TwoCoinTurnstile\n" +
          "Initial: Locked\n" +
          "{" +
          "    (Base)  Reset  Locked  lock" +
          "" +
          "  Locked : Base {" +
          "    Pass  Alarming  -" +
          "    Coin  FirstCoin -" +
          "  }" +
          "" +
          "  Alarming : Base <alarmOn >alarmOff -  -  -" +
          "" +
          "  FirstCoin : Base {" +
          "    Pass  Alarming  -" +
          "    Coin  Unlocked  unlock" +
          "  }" +
          "" +
          "  Unlocked : Base {" +
          "    Pass  Locked  lock" +
          "    Coin  -       thankyou" +
          "}");
      assertThat(sm.toString(), equalTo(
        "" +
          "Initial: Locked\n" +
          "Fsm: TwoCoinTurnstile\n" +
          "Actions:Turnstile\n" +
          "{\n" +
          "  Alarming {\n" +
          "    Reset Locked {alarmOff lock}\n" +
          "  }\n" +
          "  FirstCoin {\n" +
          "    Pass Alarming {alarmOn}\n" +
          "    Coin Unlocked {unlock}\n" +
          "    Reset Locked {lock}\n" +
          "  }\n" +
          "  Locked {\n" +
          "    Pass Alarming {alarmOn}\n" +
          "    Coin FirstCoin {}\n" +
          "    Reset Locked {lock}\n" +
          "  }\n" +
          "  Unlocked {\n" +
          "    Pass Locked {lock}\n" +
          "    Coin Unlocked {thankyou}\n" +
          "    Reset Locked {lock}\n" +
          "  }\n" +
          "}\n"));
    }
  } // Acceptance Tests
}
