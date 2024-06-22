package smc.parser;


import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import smc.lexer.Lexer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static smc.parser.ParserEvent.EOF;


public class ParserTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;

  @BeforeEach
  public void setUp() {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
  }

  private void assertParseResult(String s, String expected) {
    lexer.lex(s);
    parser.handleEvent(EOF, -1, -1);
    assertEquals(expected, builder.getFsm().toString());
  }

  private void assertParseError(String s, String expected) {
    lexer.lex(s);
    parser.handleEvent(EOF, -1, -1);
    assertEquals(expected, builder.getFsm().getError());
  }

  @Nested
  public class IncrementalTests {
    @Test
    public void parseOneHeader() {
      assertParseResult("N:V{}", "N:V\n.\n");
    }

    @Test
    public void parseManyHeaders() {
      assertParseResult("  N1 : V1\tN2 : V2\n{}", "N1:V1\nN2:V2\n.\n");
    }

    @Test
    public void noHeader() {
      assertParseResult(" {}", ".\n");
    }

    @Test
    public void simpleTransition() {
      assertParseResult("{ s e ns a }",
              """
                      {
                        s e ns a
                      }
                      .
                      """);
    }

    @Test
    public void transitionWithNullAction() {
      assertParseResult("{s e ns -}",
              """
                      {
                        s e ns {}
                      }
                      .
                      """);
    }

    @Test
    public void transitionWithManyActions() {
      assertParseResult("{s e ns {a1 a2}}",
              """
                      {
                        s e ns {a1 a2}
                      }
                      .
                      """);
    }

    @Test
    public void stateWithSubTransition() {
      assertParseResult("{s {e ns a}}",
              """
                      {
                        s e ns a
                      }
                      .
                      """);
    }

    @Test
    public void stateWithSeveralSubTransitions() {
      assertParseResult("{s {e1 ns a1 e2 ns a2}}",
              """
                      {
                        s {
                          e1 ns a1
                          e2 ns a2
                        }
                      }
                      .
                      """);
    }

    @Test
    public void manyTransitions() {
      assertParseResult("{s1 e1 s2 a1 s2 e2 s3 a2}",
              """
                      {
                        s1 e1 s2 a1
                        s2 e2 s3 a2
                      }
                      .
                      """);
    }

    @Test
    public void superState() {
      assertParseResult("{(ss) e s a}",
              """
                      {
                        (ss) e s a
                      }
                      .
                      """);
    }

    @Test
    public void entryAction() {
      assertParseResult("{s <ea e ns a}",
              """
                      {
                        s <ea e ns a
                      }
                      .
                      """);
    }

    @Test
    public void exitAction() {
      assertParseResult("{s >xa e ns a}",
              """
                      {
                        s >xa e ns a
                      }
                      .
                      """);
    }

    @Test
    public void derivedState() {
      assertParseResult("{s:ss e ns a}",
              """
                      {
                        s:ss e ns a
                      }
                      .
                      """);
    }

    @Test
    public void allStateAdornments() {
      assertParseResult("{(s)<ea>xa:ss e ns a}",
              """
                      {
                        (s):ss <ea >xa e ns a
                      }
                      .
                      """);
    }

    @Test
    public void stateWithNoSubTransitions() {
      assertParseResult("{s {}}",
              """
                      {
                        s {
                        }
                      }
                      .
                      """);
    }

    @Test
    public void stateWithAllDashes() {
      assertParseResult("{s - - -}",
              """
                      {
                        s null null {}
                      }
                      .
                      """);
    }

    @Test
    public void multipleSuperStates() {
      assertParseResult("{s :x :y - - -}",
              """
                      {
                        s:x:y null null {}
                      }
                      .
                      """);
    }

    @Test
    public void multipleEntryActions() {
      assertParseResult("{s <x <y - - -}",
              """
                      {
                        s <x <y null null {}
                      }
                      .
                      """);
    }

    @Test
    public void multipleExitActions() {
      assertParseResult("{s >x >y - - -}",
              """
                      {
                        s >x >y null null {}
                      }
                      .
                      """);
    }

    @Test
    public void multipleEntryAndExitActionsWithBraces() {
      assertParseResult("{s <{u v} >{w x} - - -}",
              """
                      {
                        s <u <v >w >x null null {}
                      }
                      .
                      """);
    }
  }

  @Nested
  public class AcceptanceTests {
    @Test
    public void simpleOneCoinTurnstile() {
      assertParseResult(
              """
                      Actions: Turnstile
                      FSM: OneCoinTurnstile
                      Initial: Locked
                      {
                        Locked\tCoin\tUnlocked\t{alarmOff unlock}
                        Locked \tPass\tLocked\t\talarmOn
                        Unlocked\tCoin\tUnlocked\tthankyou
                        Unlocked\tPass\tLocked\t\tlock
                      }""",
              """
                      Actions:Turnstile
                      FSM:OneCoinTurnstile
                      Initial:Locked
                      {
                        Locked Coin Unlocked {alarmOff unlock}
                        Locked Pass Locked alarmOn
                        Unlocked Coin Unlocked thankyou
                        Unlocked Pass Locked lock
                      }
                      .
                      """
      );
    }

    @Test
    public void twoCoinTurnstileWithoutSuperState() {
      assertParseResult(
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
                      }""",
              """
                      Actions:Turnstile
                      FSM:TwoCoinTurnstile
                      Initial:Locked
                      {
                        Locked {
                          Pass Alarming alarmOn
                          Coin FirstCoin {}
                          Reset Locked {lock alarmOff}
                        }
                        Alarming Reset Locked {lock alarmOff}
                        FirstCoin {
                          Pass Alarming {}
                          Coin Unlocked unlock
                          Reset Locked {lock alarmOff}
                        }
                        Unlocked {
                          Pass Locked lock
                          Coin null thankyou
                          Reset Locked {lock alarmOff}
                        }
                      }
                      .
                      """
      );
    }

    @Test
    public void twoCoinTurnstileWithSuperState() {
      assertParseResult(
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
                      }""",
              """
                      Actions:Turnstile
                      FSM:TwoCoinTurnstile
                      Initial:Locked
                      {
                        (Base) Reset Locked lock
                        Locked:Base {
                          Pass Alarming {}
                          Coin FirstCoin {}
                        }
                        Alarming:Base <alarmOn >alarmOff null null {}
                        FirstCoin:Base {
                          Pass Alarming {}
                          Coin Unlocked unlock
                        }
                        Unlocked:Base {
                          Pass Locked lock
                          Coin null thankyou
                        }
                      }
                      .
                      """);
    }
  }

  @Nested
  public class ErrorTests {
    @Test
    public void parseNothing() {
      assertParseError("", "Syntax error: HEADER. HEADER|EOF. line -1, position -1.\n");
    }

    @Test
    public void headerWithNoColonOrValue() {
      assertParseError("A {s e ns a}",
        "Syntax error: HEADER. HEADER_COLON|OPEN_BRACE. line 1, position 2.\n");
    }

    @Test
    public void headerWithNoValue() {
      assertParseError("A: {s e ns a}",
        "Syntax error: HEADER. HEADER_VALUE|OPEN_BRACE. line 1, position 3.\n");
    }

    @Test
    public void transitionWayTooShort() {
      assertParseError("{s}",
        "Syntax error: STATE. STATE_MODIFIER|CLOSED_BRACE. line 1, position 2.\n");
    }

    @Test
    public void transitionTooShort() {
      assertParseError("{s e}",
        "Syntax error: TRANSITION. SINGLE_EVENT|CLOSED_BRACE. line 1, position 4.\n");
    }

    @Test
    public void transitionNoAction() {
      assertParseError("{s e ns}",
        "Syntax error: TRANSITION. SINGLE_NEXT_STATE|CLOSED_BRACE. line 1, position 7.\n");
    }

    @Test
    public void noClosingBrace() {
      assertParseError("{",
        "Syntax error: STATE. STATE_SPEC|EOF. line -1, position -1.\n");
    }

    @Test
    public void initialStateDash() {
      assertParseError("{- e ns a}",
        "Syntax error: STATE. STATE_SPEC|DASH. line 1, position 1.\n");
    }

    @Test
    public void lexicalError() {
      assertParseError("{.}",
        "Syntax error: SYNTAX. . line 1, position 2.\n");
    }
  }
}
