package smc.semanticAnalyzer;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import smc.lexer.Lexer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;

import java.util.List;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsCollectionContaining.hasItems;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static smc.parser.FsmSyntax.Header;
import static smc.parser.ParserEvent.EOF;
import static smc.semanticAnalyzer.SemanticStateMachine.AnalysisError;
import static smc.semanticAnalyzer.SemanticStateMachine.AnalysisError.ID.*;

@RunWith(HierarchicalContextRunner.class)
public class SemanticAnalyzerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;

  @Before
  public void setUp() throws Exception {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
    analyzer = new SemanticAnalyzer();
  }

  private SemanticStateMachine produceAst(String s) {
    lexer.lex(s);
    parser.handleEvent(EOF, -1, -1);
    return analyzer.analyze(builder.getFsm());
  }

  private void assertSemanticResult(String s, String expected) {
    SemanticStateMachine semanticStateMachine = produceAst(s);
    assertEquals(expected, semanticStateMachine.toString());
  }

  public class SemanticErrors {
    public class HeaderErrors {
      @Test
      public void noHeaders() throws Exception {
        List<AnalysisError> errors = produceAst("{}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(NO_FSM),
          new AnalysisError(NO_INITIAL)));
      }

      @Test
      public void missingActions() throws Exception {
        List<AnalysisError> errors = produceAst("FSM:f Initial:i {}").errors;
        assertThat(errors, not(hasItems(
          new AnalysisError(NO_FSM),
          new AnalysisError(NO_INITIAL))));
      }

      @Test
      public void missingFsm() throws Exception {
        List<AnalysisError> errors = produceAst("actions:a Initial:i {}").errors;
        assertThat(errors, not(hasItems(
          new AnalysisError(NO_INITIAL))));
        assertThat(errors, hasItems(new AnalysisError(NO_FSM)));
      }

      @Test
      public void missingInitial() throws Exception {
        List<AnalysisError> errors = produceAst("Actions:a Fsm:f {}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(NO_FSM))));
        assertThat(errors, hasItems(new AnalysisError(NO_INITIAL)));
      }

      @Test
      public void nothingMissing() throws Exception {
        List<AnalysisError> errors = produceAst("Initial: f Actions:a Fsm:f {}").errors;
        assertThat(errors, not(hasItems(
          new AnalysisError(NO_INITIAL),
          new AnalysisError(NO_FSM))));
      }

      @Test
      public void unexpectedHeader() throws Exception {
        List<AnalysisError> errors = produceAst("X: x{s - - -}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(INVALID_HEADER, new Header("X", "x"))));
      }

      @Test
      public void duplicateHeader() throws Exception {
        List<AnalysisError> errors = produceAst("fsm:f fsm:x{s - - -}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(EXTRA_HEADER_IGNORED, new Header("fsm", "x"))));
      }

      @Test
      public void initialStateMustBeDefined() throws Exception {
        List<AnalysisError> errors = produceAst("initial: i {s - - -}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(UNDEFINED_STATE, "initial: i")));
      }
    } // Header Errors

    public class StateErrors {
      @Test
      public void nullNextStateIsNotUndefined() throws Exception {
        List<AnalysisError> errors = produceAst("{s - - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNDEFINED_STATE, null))));
      }

      @Test
      public void undefinedState() throws Exception {
        List<AnalysisError> errors = produceAst("{s - s2 -}").errors;
        assertThat(errors, hasItems(new AnalysisError(UNDEFINED_STATE, "s2")));
      }

      @Test
      public void noUndefinedStates() throws Exception {
        List<AnalysisError> errors = produceAst("{s - s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNDEFINED_STATE, "s2"))));
      }

      @Test
      public void undefinedSuperState() throws Exception {
        List<AnalysisError> errors = produceAst("{s:ss - - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(UNDEFINED_SUPER_STATE, "ss")));
      }

      @Test
      public void superStateDefined() throws Exception {
        List<AnalysisError> errors = produceAst("{ss - - - s:ss - - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNDEFINED_SUPER_STATE, "s2"))));
      }

      @Test
      public void unusedStates() throws Exception {
        List<AnalysisError> errors = produceAst("{s e n -}").errors;
        assertThat(errors, hasItems(new AnalysisError(UNUSED_STATE, "s")));
      }

      @Test
      public void noUnusedStates() throws Exception {
        List<AnalysisError> errors = produceAst("{s e s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "s"))));
      }

      @Test
      public void nextStateNullIsImplicitUse() throws Exception {
        List<AnalysisError> errors = produceAst("{s e - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "s"))));
      }

      @Test
      public void usedAsBaseIsValidUsage() throws Exception {
        List<AnalysisError> errors = produceAst("{b e n - s:b e2 s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "b"))));
      }

      @Test
      public void usedAsInitialIsValidUsage() throws Exception {
        List<AnalysisError> errors = produceAst("initial: b {b e n -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "b"))));
      }

      @Test
      public void errorIfSuperStatesHaveConflictingTransitions() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "FSM: f Actions: act Initial: s" +
            "{" +
            "  (ss1) e1 s1 -" +
            "  (ss2) e1 s2 -" +
            "  s :ss1 :ss2 e2 s3 a" +
            "  s2 e s -" +
            "  s1 e s -" +
            "  s3 e s -" +
            "}").errors;
        assertThat(errors, hasItems(new AnalysisError(CONFLICTING_SUPERSTATES, "s|e1")));
      }

      @Test
      public void noErrorForOverriddenTransition() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "FSM: f Actions: act Initial: s" +
            "{" +
            "  (ss1) e1 s1 -" +
            "  s :ss1 e1 s3 a" +
            "  s1 e s -" +
            "  s3 e s -" +
            "}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(CONFLICTING_SUPERSTATES, "s|e1"))));
      }

      @Test
      public void noErrorIfSuperStatesHaveIdenticalTransitions() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "FSM: f Actions: act Initial: s" +
            "{" +
            "  (ss1) e1 s1 ax" +
            "  (ss2) e1 s1 ax" +
            "  s :ss1 :ss2 e2 s3 a" +
            "  s1 e s -" +
            "  s3 e s -" +
            "}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(CONFLICTING_SUPERSTATES, "s|e1"))));
      }

      @Test
      public void errorIfSuperstatesHaveDifferentActionsInSameTransitions() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "FSM: f Actions: act Initial: s" +
            "{" +
            "  (ss1) e1 s1 a1" +
            "  (ss2) e1 s1 a2" +
            "  s :ss1 :ss2 e2 s3 a" +
            "  s1 e s -" +
            "  s3 e s -" +
            "}").errors;
        assertThat(errors, hasItems(new AnalysisError(CONFLICTING_SUPERSTATES, "s|e1")));

      }
    } // State Errors

    public class TransitionErrors {
      @Test
      public void duplicateTransitions() throws Exception {
        List<AnalysisError> errors = produceAst("{s e - - s e - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(DUPLICATE_TRANSITION, "s(e)")));
      }

      @Test
      public void noDuplicateTransitions() throws Exception {
        List<AnalysisError> errors = produceAst("{s e - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(DUPLICATE_TRANSITION, "s(e)"))));
      }

      @Test
      public void abstractStatesCantBeTargets() throws Exception {
        List<AnalysisError> errors = produceAst("{(as) e - - s e as -}").errors;
        assertThat(errors, hasItems(new AnalysisError(ABSTRACT_STATE_USED_AS_NEXT_STATE, "s(e)->as")));
      }

      @Test
      public void abstractStatesCanBeUsedAsSuperStates() throws Exception {
        List<AnalysisError> errors = produceAst("{(as) e - - s:as e s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(ABSTRACT_STATE_USED_AS_NEXT_STATE, "s(e)->s"))));
      }

      @Test
      public void entryAndExitActionsNotMultiplyDefined() throws Exception {
        List<AnalysisError> errors = produceAst(
          "" +
            "{" +
            "  s - - - " +
            "  s - - -" +
            "  es - - -" +
            "  es <x - - - " +
            "  es <x - - -" +
            "  xs >x - - -" +
            "  xs >{x} - - -" +
            "}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "s"))));
        assertThat(errors, not(hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "es"))));
        assertThat(errors, not(hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "xs"))));
      }

      @Test
      public void errorIfStateHasMultipleEntryActionDefinitions() throws Exception {
        List<AnalysisError> errors = produceAst("{s - - - ds <x - - - ds <y - - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "s"))));
        assertThat(errors, hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds")));
      }

      @Test
      public void errorIfStateHasMultipleExitActionDefinitions() throws Exception {
        List<AnalysisError> errors = produceAst("{ds >x - - - ds >y - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds")));
      }

      @Test
      public void errorIfStateHasMultiplyDefinedEntryAndExitActions() throws Exception {
        List<AnalysisError> errors = produceAst("{ds >x - - - ds <y - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds")));
      }
    } // Transition Errors
  }// Semantic Errors.

  public class Warnings {
    @Test
    public void warnIfStateUsedAsBothAbstractAndConcrete() throws Exception {
      List<AnalysisError> errors = produceAst("{(ias) e - - ias e - - (cas) e - -}").warnings;
      assertThat(errors, not(hasItems(new AnalysisError(INCONSISTENT_ABSTRACTION, "cas"))));
      assertThat(errors, hasItems(new AnalysisError(INCONSISTENT_ABSTRACTION, "ias")));
    }
  } // Warnings

  public class Lists {
    @Test
    public void oneState() throws Exception {
      SemanticStateMachine ast = produceAst("{s - - -}");
      assertThat(ast.states.values(), contains(new SemanticStateMachine.SemanticState("s")));
    }

    @Test
    public void manyStates() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 - - - s2 - - - s3 - - -}");
      assertThat(ast.states.values(), hasItems(
        new SemanticStateMachine.SemanticState("s1"),
        new SemanticStateMachine.SemanticState("s2"),
        new SemanticStateMachine.SemanticState("s3")));
    }

    @Test
    public void statesAreKeyedByName() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 - - - s2 - - - s3 - - -}");
      assertThat(ast.states.get("s1"), equalTo(new SemanticStateMachine.SemanticState("s1")));
      assertThat(ast.states.get("s2"), equalTo(new SemanticStateMachine.SemanticState("s2")));
      assertThat(ast.states.get("s3"), equalTo(new SemanticStateMachine.SemanticState("s3")));
    }

    @Test
    public void manyEvents() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 e1 - - s2 e2 - - s3 e3 - -}");
      assertThat(ast.events, hasItems("e1", "e2", "e3"));
      assertThat(ast.events, hasSize(3));
    }

    @Test
    public void manyEventsButNoDuplicates() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 e1 - - s2 e2 - - s3 e1 - -}");
      assertThat(ast.events, hasItems("e1", "e2"));
      assertThat(ast.events, hasSize(2));
    }

    @Test
    public void noNullEvents() throws Exception {
      SemanticStateMachine ast = produceAst("{(s1) - - -}");
      assertThat(ast.events, hasSize(0));
    }

    @Test
    public void manyActionsButNoDuplicates() throws Exception {
      SemanticStateMachine ast = produceAst("{s1 e1 - {a1 a2} s2 e2 - {a3 a1}}");
      assertThat(ast.actions, hasItems("a1", "a2", "a3"));
      assertThat(ast.actions, hasSize(3));
    }

    @Test
    public void entryAndExitActionsAreCountedAsActions() throws Exception {
      SemanticStateMachine ast = produceAst("{s <ea >xa - - a}");
      assertThat(ast.actions, hasItems("ea", "xa"));
    }
  } // Lists

  public class Logic {
    private String addHeader(String s) {
      return "initial: s fsm:f actions:a " + s;
    }

    private void assertSyntaxToAst(String syntax, String ast) {
      String states = produceAst(addHeader(syntax)).statesToString();
      assertThat(states, equalTo(ast));
    }

    @Test
    public void oneTransition() throws Exception {
      assertSyntaxToAst("{s e s a}",
        "" +
          "{\n" +
          "  s {\n" +
          "    e s {a}\n" +
          "  }\n" +
          "}\n");
    }

    @Test
    public void twoTransitionsAreAggregated() throws Exception {
      assertSyntaxToAst("{s e1 s a s e2 s a}",
        "" +
          "{\n" +
          "  s {\n" +
          "    e1 s {a}\n" +
          "    e2 s {a}\n" +
          "  }\n" +
          "}\n");
    }

    @Test
    public void superStatesAreAggregated() throws Exception {
      assertSyntaxToAst("{s:b1 e1 s a s:b2 e2 s a (b1) e s - (b2) e s -}",
        "" +
          "{\n" +
          "  (b1) {\n" +
          "    e s {}\n" +
          "  }\n" +
          "\n" +
          "  (b2) {\n" +
          "    e s {}\n" +
          "  }\n" +
          "\n" +
          "  s :b1 :b2 {\n" +
          "    e1 s {a}\n" +
          "    e2 s {a}\n" +
          "  }\n" +
          "}\n");
    }

    @Test
    public void nullNextStateRefersToSelf() throws Exception {
      assertSyntaxToAst("{s e - a}",
        "" +
          "{\n" +
          "  s {\n" +
          "    e s {a}\n" +
          "  }\n" +
          "}\n"
      );
    }

    @Test
    public void actionsRemainInOrder() throws Exception {
      assertSyntaxToAst("{s e s {the quick brown fox jumped over the lazy dogs back}}",
        "" +
          "{\n" +
          "  s {\n" +
          "    e s {the quick brown fox jumped over the lazy dogs back}\n" +
          "  }\n" +
          "}\n");
    }

    @Test
    public void entryAndExitActionsRemainInOrder() throws Exception {
      assertSyntaxToAst("{s <{d o} <g >{c a} >t e s a}",
        "" +
          "{\n" +
          "  s <d <o <g >c >a >t {\n" +
          "    e s {a}\n" +
          "  }\n" +
          "}\n");
    }
  } //Logic

  public class AcceptanceTests {
    @Test
    public void subwayTurnstileOne() throws Exception {
      SemanticStateMachine ast = produceAst(
        "" +
          "Actions: Turnstile\n" +
          "FSM: OneCoinTurnstile\n" +
          "Initial: Locked\n" +
          "{\n" +
          "  Locked\tCoin\tUnlocked\t{alarmOff unlock}\n" +
          "  Locked \tPass\tLocked\t\talarmOn\n" +
          "  Unlocked\tCoin\tUnlocked\tthankyou\n" +
          "  Unlocked\tPass\tLocked\t\tlock\n" +
          "}");
      assertThat(ast.toString(), equalTo(
        "" +
          "Actions: Turnstile\n" +
          "FSM: OneCoinTurnstile\n" +
          "Initial: Locked{\n" +
          "  Locked {\n" +
          "    Coin Unlocked {alarmOff unlock}\n" +
          "    Pass Locked {alarmOn}\n" +
          "  }\n" +
          "\n" +
          "  Unlocked {\n" +
          "    Coin Unlocked {thankyou}\n" +
          "    Pass Locked {lock}\n" +
          "  }\n" +
          "}\n"));
    }

    @Test
    public void subwayTurnstileTwo() throws Exception {
      SemanticStateMachine ast = produceAst(
        "" +
          "Actions: Turnstile\n" +
          "FSM: TwoCoinTurnstile\n" +
          "Initial: Locked\n" +
          "{\n" +
          "\tLocked {\n" +
          "\t\tPass\tAlarming\talarmOn\n" +
          "\t\tCoin\tFirstCoin\t-\n" +
          "\t\tReset\tLocked\t{lock alarmOff}\n" +
          "\t}\n" +
          "\t\n" +
          "\tAlarming\tReset\tLocked {lock alarmOff}\n" +
          "\t\n" +
          "\tFirstCoin {\n" +
          "\t\tPass\tAlarming\t-\n" +
          "\t\tCoin\tUnlocked\tunlock\n" +
          "\t\tReset\tLocked {lock alarmOff}\n" +
          "\t}\n" +
          "\t\n" +
          "\tUnlocked {\n" +
          "\t\tPass\tLocked\tlock\n" +
          "\t\tCoin\t-\t\tthankyou\n" +
          "\t\tReset\tLocked {lock alarmOff}\n" +
          "\t}\n" +
          "}"
      );
      assertThat(ast.toString(), equalTo(
        "" +
          "Actions: Turnstile\n" +
          "FSM: TwoCoinTurnstile\n" +
          "Initial: Locked{\n" +
          "  Alarming {\n" +
          "    Reset Locked {lock alarmOff}\n" +
          "  }\n" +
          "\n" +
          "  FirstCoin {\n" +
          "    Pass Alarming {}\n" +
          "    Coin Unlocked {unlock}\n" +
          "    Reset Locked {lock alarmOff}\n" +
          "  }\n" +
          "\n" +
          "  Locked {\n" +
          "    Pass Alarming {alarmOn}\n" +
          "    Coin FirstCoin {}\n" +
          "    Reset Locked {lock alarmOff}\n" +
          "  }\n" +
          "\n" +
          "  Unlocked {\n" +
          "    Pass Locked {lock}\n" +
          "    Coin Unlocked {thankyou}\n" +
          "    Reset Locked {lock alarmOff}\n" +
          "  }\n" +
          "}\n"));
    }

    @Test
    public void subwayTurnstileThree() throws Exception {
      SemanticStateMachine ast = produceAst(
        "" +
          "Actions: Turnstile\n" +
          "FSM: TwoCoinTurnstile\n" +
          "Initial: Locked\n" +
          "{\n" +
          "    (Base)\tReset\tLocked\tlock\n" +
          "\n" +
          "\tLocked : Base {\n" +
          "\t\tPass\tAlarming\t-\n" +
          "\t\tCoin\tFirstCoin\t-\n" +
          "\t}\n" +
          "\t\n" +
          "\tAlarming : Base\t<alarmOn >alarmOff -\t-\t-\n" +
          "\t\n" +
          "\tFirstCoin : Base {\n" +
          "\t\tPass\tAlarming\t-\n" +
          "\t\tCoin\tUnlocked\tunlock\n" +
          "\t}\n" +
          "\t\n" +
          "\tUnlocked : Base {\n" +
          "\t\tPass\tLocked\tlock\n" +
          "\t\tCoin\t-\t\tthankyou\n" +
          "\t}\n" +
          "}"
      );
      assertThat(ast.toString(), equalTo(
        "" +
          "Actions: Turnstile\n" +
          "FSM: TwoCoinTurnstile\n" +
          "Initial: Locked{\n" +
          "  Alarming :Base <alarmOn >alarmOff {\n" +
          "    null Alarming {}\n" +
          "  }\n" +
          "\n" +
          "  (Base) {\n" +
          "    Reset Locked {lock}\n" +
          "  }\n" +
          "\n" +
          "  FirstCoin :Base {\n" +
          "    Pass Alarming {}\n" +
          "    Coin Unlocked {unlock}\n" +
          "  }\n" +
          "\n" +
          "  Locked :Base {\n" +
          "    Pass Alarming {}\n" +
          "    Coin FirstCoin {}\n" +
          "  }\n" +
          "\n" +
          "  Unlocked :Base {\n" +
          "    Pass Locked {lock}\n" +
          "    Coin Unlocked {thankyou}\n" +
          "  }\n" +
          "}\n"));
    }

  }
}
