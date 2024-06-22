package smc.semanticAnalyzer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static smc.parser.FsmSyntax.Header;
import static smc.parser.ParserEvent.EOF;
import static smc.semanticAnalyzer.SemanticStateMachine.AnalysisError;
import static smc.semanticAnalyzer.SemanticStateMachine.AnalysisError.ID.*;


public class SemanticAnalyzerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;

  @BeforeEach
  public void setUp() {
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

  @Nested
  public class SemanticErrors {
    @Nested
    public class HeaderErrors {
      @Test
      public void noHeaders() {
        List<AnalysisError> errors = produceAst("{}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(NO_FSM),
          new AnalysisError(NO_INITIAL)));
      }

      @Test
      public void missingActions() {
        List<AnalysisError> errors = produceAst("FSM:f Initial:i {}").errors;
        assertThat(errors, not(hasItems(
          new AnalysisError(NO_FSM),
          new AnalysisError(NO_INITIAL))));
      }

      @Test
      public void missingFsm() {
        List<AnalysisError> errors = produceAst("actions:a Initial:i {}").errors;
        assertThat(errors, not(hasItems(
          new AnalysisError(NO_INITIAL))));
        assertThat(errors, hasItems(new AnalysisError(NO_FSM)));
      }

      @Test
      public void missingInitial() {
        List<AnalysisError> errors = produceAst("Actions:a Fsm:f {}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(NO_FSM))));
        assertThat(errors, hasItems(new AnalysisError(NO_INITIAL)));
      }

      @Test
      public void nothingMissing() {
        List<AnalysisError> errors = produceAst("Initial: f Actions:a Fsm:f {}").errors;
        assertThat(errors, not(hasItems(
          new AnalysisError(NO_INITIAL),
          new AnalysisError(NO_FSM))));
      }

      @Test
      public void unexpectedHeader() {
        List<AnalysisError> errors = produceAst("X: x{s - - -}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(INVALID_HEADER, new Header("X", "x"))));
      }

      @Test
      public void duplicateHeader() {
        List<AnalysisError> errors = produceAst("fsm:f fsm:x{s - - -}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(EXTRA_HEADER_IGNORED, new Header("fsm", "x"))));
      }

      @Test
      public void initialStateMustBeDefined() {
        List<AnalysisError> errors = produceAst("initial: i {s - - -}").errors;
        assertThat(errors, hasItems(
          new AnalysisError(UNDEFINED_STATE, "initial: i")));
      }
    } // Header Errors

    @Nested
    public class StateErrors {
      @Test
      public void nullNextStateIsNotUndefined() {
        List<AnalysisError> errors = produceAst("{s - - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNDEFINED_STATE, null))));
      }

      @Test
      public void undefinedState() {
        List<AnalysisError> errors = produceAst("{s - s2 -}").errors;
        assertThat(errors, hasItems(new AnalysisError(UNDEFINED_STATE, "s2")));
      }

      @Test
      public void noUndefinedStates() {
        List<AnalysisError> errors = produceAst("{s - s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNDEFINED_STATE, "s2"))));
      }

      @Test
      public void undefinedSuperState() {
        List<AnalysisError> errors = produceAst("{s:ss - - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(UNDEFINED_SUPER_STATE, "ss")));
      }

      @Test
      public void superStateDefined() {
        List<AnalysisError> errors = produceAst("{ss - - - s:ss - - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNDEFINED_SUPER_STATE, "s2"))));
      }

      @Test
      public void unusedStates() {
        List<AnalysisError> errors = produceAst("{s e n -}").errors;
        assertThat(errors, hasItems(new AnalysisError(UNUSED_STATE, "s")));
      }

      @Test
      public void noUnusedStates() {
        List<AnalysisError> errors = produceAst("{s e s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "s"))));
      }

      @Test
      public void nextStateNullIsImplicitUse() {
        List<AnalysisError> errors = produceAst("{s e - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "s"))));
      }

      @Test
      public void usedAsBaseIsValidUsage() {
        List<AnalysisError> errors = produceAst("{b e n - s:b e2 s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "b"))));
      }

      @Test
      public void usedAsInitialIsValidUsage() {
        List<AnalysisError> errors = produceAst("initial: b {b e n -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(UNUSED_STATE, "b"))));
      }

      @Test
      public void errorIfSuperStatesHaveConflictingTransitions() {
        List<AnalysisError> errors = produceAst(
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
      public void noErrorForOverriddenTransition() {
        List<AnalysisError> errors = produceAst(
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
      public void noErrorIfSuperStatesHaveIdenticalTransitions() {
        List<AnalysisError> errors = produceAst(
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
      public void errorIfSuperstatesHaveDifferentActionsInSameTransitions() {
        List<AnalysisError> errors = produceAst(
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

    @Nested
    public class TransitionErrors {
      @Test
      public void duplicateTransitions() {
        List<AnalysisError> errors = produceAst("{s e - - s e - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(DUPLICATE_TRANSITION, "s(e)")));
      }

      @Test
      public void noDuplicateTransitions() {
        List<AnalysisError> errors = produceAst("{s e - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(DUPLICATE_TRANSITION, "s(e)"))));
      }

      @Test
      public void abstractStatesCantBeTargets() {
        List<AnalysisError> errors = produceAst("{(as) e - - s e as -}").errors;
        assertThat(errors, hasItems(new AnalysisError(ABSTRACT_STATE_USED_AS_NEXT_STATE, "s(e)->as")));
      }

      @Test
      public void abstractStatesCanBeUsedAsSuperStates() {
        List<AnalysisError> errors = produceAst("{(as) e - - s:as e s -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(ABSTRACT_STATE_USED_AS_NEXT_STATE, "s(e)->s"))));
      }

      @Test
      public void entryAndExitActionsNotMultiplyDefined() {
        List<AnalysisError> errors = produceAst(
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
      public void errorIfStateHasMultipleEntryActionDefinitions() {
        List<AnalysisError> errors = produceAst("{s - - - ds <x - - - ds <y - - -}").errors;
        assertThat(errors, not(hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "s"))));
        assertThat(errors, hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds")));
      }

      @Test
      public void errorIfStateHasMultipleExitActionDefinitions() {
        List<AnalysisError> errors = produceAst("{ds >x - - - ds >y - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds")));
      }

      @Test
      public void errorIfStateHasMultiplyDefinedEntryAndExitActions() {
        List<AnalysisError> errors = produceAst("{ds >x - - - ds <y - -}").errors;
        assertThat(errors, hasItems(new AnalysisError(STATE_ACTIONS_MULTIPLY_DEFINED, "ds")));
      }
    } // Transition Errors
  }// Semantic Errors.

  @Nested
  public class Warnings {
    @Test
    public void warnIfStateUsedAsBothAbstractAndConcrete() {
      List<AnalysisError> errors = produceAst("{(ias) e - - ias e - - (cas) e - -}").warnings;
      assertThat(errors, not(hasItems(new AnalysisError(INCONSISTENT_ABSTRACTION, "cas"))));
      assertThat(errors, hasItems(new AnalysisError(INCONSISTENT_ABSTRACTION, "ias")));
    }
  } // Warnings

  @Nested
  public class Lists {
    @Test
    public void oneState() {
      SemanticStateMachine ast = produceAst("{s - - -}");
      assertThat(ast.states.values(), contains(new SemanticStateMachine.SemanticState("s")));
    }

    @Test
    public void manyStates() {
      SemanticStateMachine ast = produceAst("{s1 - - - s2 - - - s3 - - -}");
      assertThat(ast.states.values(), hasItems(
        new SemanticStateMachine.SemanticState("s1"),
        new SemanticStateMachine.SemanticState("s2"),
        new SemanticStateMachine.SemanticState("s3")));
    }

    @Test
    public void statesAreKeyedByName() {
      SemanticStateMachine ast = produceAst("{s1 - - - s2 - - - s3 - - -}");
      assertThat(ast.states.get("s1"), equalTo(new SemanticStateMachine.SemanticState("s1")));
      assertThat(ast.states.get("s2"), equalTo(new SemanticStateMachine.SemanticState("s2")));
      assertThat(ast.states.get("s3"), equalTo(new SemanticStateMachine.SemanticState("s3")));
    }

    @Test
    public void manyEvents() {
      SemanticStateMachine ast = produceAst("{s1 e1 - - s2 e2 - - s3 e3 - -}");
      assertThat(ast.events, hasItems("e1", "e2", "e3"));
      assertThat(ast.events, hasSize(3));
    }

    @Test
    public void manyEventsButNoDuplicates() {
      SemanticStateMachine ast = produceAst("{s1 e1 - - s2 e2 - - s3 e1 - -}");
      assertThat(ast.events, hasItems("e1", "e2"));
      assertThat(ast.events, hasSize(2));
    }

    @Test
    public void noNullEvents() {
      SemanticStateMachine ast = produceAst("{(s1) - - -}");
      assertThat(ast.events, hasSize(0));
    }

    @Test
    public void manyActionsButNoDuplicates() {
      SemanticStateMachine ast = produceAst("{s1 e1 - {a1 a2} s2 e2 - {a3 a1}}");
      assertThat(ast.actions, hasItems("a1", "a2", "a3"));
      assertThat(ast.actions, hasSize(3));
    }

    @Test
    public void entryAndExitActionsAreCountedAsActions() {
      SemanticStateMachine ast = produceAst("{s <ea >xa - - a}");
      assertThat(ast.actions, hasItems("ea", "xa"));
    }
  } // Lists

  @Nested
  public class Logic {
    private String addHeader(String s) {
      return "initial: s fsm:f actions:a " + s;
    }

    private void assertSyntaxToAst(String syntax, String ast) {
      String states = produceAst(addHeader(syntax)).statesToString();
      assertThat(states, equalTo(ast));
    }

    @Test
    public void oneTransition() {
      assertSyntaxToAst("{s e s a}",
              """
                      {
                        s {
                          e s {a}
                        }
                      }
                      """);
    }

    @Test
    public void twoTransitionsAreAggregated() {
      assertSyntaxToAst("{s e1 s a s e2 s a}",
              """
                      {
                        s {
                          e1 s {a}
                          e2 s {a}
                        }
                      }
                      """);
    }

    @Test
    public void superStatesAreAggregated() {
      assertSyntaxToAst("{s:b1 e1 s a s:b2 e2 s a (b1) e s - (b2) e s -}",
              """
                      {
                        (b1) {
                          e s {}
                        }

                        (b2) {
                          e s {}
                        }

                        s :b1 :b2 {
                          e1 s {a}
                          e2 s {a}
                        }
                      }
                      """);
    }

    @Test
    public void nullNextStateRefersToSelf() {
      assertSyntaxToAst("{s e - a}",
              """
                      {
                        s {
                          e s {a}
                        }
                      }
                      """
      );
    }

    @Test
    public void actionsRemainInOrder() {
      assertSyntaxToAst("{s e s {the quick brown fox jumped over the lazy dogs back}}",
              """
                      {
                        s {
                          e s {the quick brown fox jumped over the lazy dogs back}
                        }
                      }
                      """);
    }

    @Test
    public void entryAndExitActionsRemainInOrder() {
      assertSyntaxToAst("{s <{d o} <g >{c a} >t e s a}",
              """
                      {
                        s <d <o <g >c >a >t {
                          e s {a}
                        }
                      }
                      """);
    }
  } //Logic

  @Nested
  public class AcceptanceTests {
    @Test
    public void subwayTurnstileOne() {
      SemanticStateMachine ast = produceAst(
              """
                      Actions: Turnstile
                      FSM: OneCoinTurnstile
                      Initial: Locked
                      {
                        Locked\tCoin\tUnlocked\t{alarmOff unlock}
                        Locked \tPass\tLocked\t\talarmOn
                        Unlocked\tCoin\tUnlocked\tthankyou
                        Unlocked\tPass\tLocked\t\tlock
                      }""");
      assertThat(ast.toString(), equalTo(
              """
                      Actions: Turnstile
                      FSM: OneCoinTurnstile
                      Initial: Locked{
                        Locked {
                          Coin Unlocked {alarmOff unlock}
                          Pass Locked {alarmOn}
                        }

                        Unlocked {
                          Coin Unlocked {thankyou}
                          Pass Locked {lock}
                        }
                      }
                      """));
    }

    @Test
    public void subwayTurnstileTwo() {
      SemanticStateMachine ast = produceAst(
              """
                      Actions: Turnstile
                      FSM: TwoCoinTurnstile
                      Initial: Locked
                      {
                      \tLocked {
                      \t\tPass\tAlarming\talarmOn
                      \t\tCoin\tFirstCoin\t-
                      \t\tReset\tLocked\t{lock alarmOff}
                      \t}
                      \t
                      \tAlarming\tReset\tLocked {lock alarmOff}
                      \t
                      \tFirstCoin {
                      \t\tPass\tAlarming\t-
                      \t\tCoin\tUnlocked\tunlock
                      \t\tReset\tLocked {lock alarmOff}
                      \t}
                      \t
                      \tUnlocked {
                      \t\tPass\tLocked\tlock
                      \t\tCoin\t-\t\tthankyou
                      \t\tReset\tLocked {lock alarmOff}
                      \t}
                      }"""
      );
      assertThat(ast.toString(), equalTo(
              """
                      Actions: Turnstile
                      FSM: TwoCoinTurnstile
                      Initial: Locked{
                        Alarming {
                          Reset Locked {lock alarmOff}
                        }

                        FirstCoin {
                          Pass Alarming {}
                          Coin Unlocked {unlock}
                          Reset Locked {lock alarmOff}
                        }

                        Locked {
                          Pass Alarming {alarmOn}
                          Coin FirstCoin {}
                          Reset Locked {lock alarmOff}
                        }

                        Unlocked {
                          Pass Locked {lock}
                          Coin Unlocked {thankyou}
                          Reset Locked {lock alarmOff}
                        }
                      }
                      """));
    }

    @Test
    public void subwayTurnstileThree() {
      SemanticStateMachine ast = produceAst(
              """
                      Actions: Turnstile
                      FSM: TwoCoinTurnstile
                      Initial: Locked
                      {
                          (Base)\tReset\tLocked\tlock

                      \tLocked : Base {
                      \t\tPass\tAlarming\t-
                      \t\tCoin\tFirstCoin\t-
                      \t}
                      \t
                      \tAlarming : Base\t<alarmOn >alarmOff -\t-\t-
                      \t
                      \tFirstCoin : Base {
                      \t\tPass\tAlarming\t-
                      \t\tCoin\tUnlocked\tunlock
                      \t}
                      \t
                      \tUnlocked : Base {
                      \t\tPass\tLocked\tlock
                      \t\tCoin\t-\t\tthankyou
                      \t}
                      }"""
      );
      assertThat(ast.toString(), equalTo(
              """
                      Actions: Turnstile
                      FSM: TwoCoinTurnstile
                      Initial: Locked{
                        Alarming :Base <alarmOn >alarmOff {
                          null Alarming {}
                        }

                        (Base) {
                          Reset Locked {lock}
                        }

                        FirstCoin :Base {
                          Pass Alarming {}
                          Coin Unlocked {unlock}
                        }

                        Locked :Base {
                          Pass Alarming {}
                          Coin FirstCoin {}
                        }

                        Unlocked :Base {
                          Pass Locked {lock}
                          Coin Unlocked {thankyou}
                        }
                      }
                      """));
    }

  }
}
