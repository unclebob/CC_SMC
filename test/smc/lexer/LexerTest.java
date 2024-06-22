package smc.lexer;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
public class LexerTest implements TokenCollector {
  String tokens = "";
  Lexer lexer;
  private boolean firstToken = true;

  @BeforeEach
  public void setUp() {
    lexer = new Lexer(this);
  }

  private void addToken(String token) {
    if (!firstToken)
      tokens += ",";
    tokens += token;
    firstToken = false;
  }

  private void assertLexResult(String input, String expected) {
    lexer.lex(input);
    assertEquals(expected, tokens);
  }

  public void openBrace(int line, int pos) {
    addToken("OB");
  }

  public void closedBrace(int line, int pos) {
    addToken("CB");
  }

  public void openParen(int line, int pos) {
    addToken("OP");
  }

  public void closedParen(int line, int pos) {
    addToken("CP");
  }

  public void openAngle(int line, int pos) {
    addToken("OA");
  }

  public void closedAngle(int line, int pos) {
    addToken("CA");
  }

  public void dash(int line, int pos) {
    addToken("D");
  }

  public void colon(int line, int pos) {
    addToken("C");
  }

  public void name(String name, int line, int pos) {
    addToken("#" + name + "#");
  }

  public void error(int line, int pos) {
    addToken("E" + line + "/" + pos);
  }

  @Nested
  public class SingleTokenTests {
    @Test
    public void findsOpenBrace() {
      assertLexResult("{", "OB");
    }

    @Test
    public void findsClosedBrace() {
      assertLexResult("}", "CB");
    }

    @Test
    public void findsOpenParen() {
      assertLexResult("(", "OP");
    }

    @Test
    public void findsClosedParen() {
      assertLexResult(")", "CP");
    }

    @Test
    public void findsOpenAngle() {
      assertLexResult("<", "OA");
    }

    @Test
    public void findsClosedAngle() {
      assertLexResult(">", "CA");
    }

    @Test
    public void findsDash() {
      assertLexResult("-", "D");
    }

    @Test
    public void findStarAsDash() {
      assertLexResult("*", "D");
    }

    @Test
    public void findsColon() {
      assertLexResult(":", "C");
    }

    @Test
    public void findsSimpleName() {
      assertLexResult("name", "#name#");
    }

    @Test
    public void findComplexName() {
      assertLexResult("Room_222", "#Room_222#");
    }

    @Test
    public void error() {
      assertLexResult(".", "E1/1");
    }

    @Test
    public void nothingButWhiteSpace() {
      assertLexResult(" ", "");
    }

    @Test
    public void whiteSpaceBeforeEach() {
      assertLexResult("  \t\n  -", "D");
    }
  }

  @Nested
  public class CommentTests {
    @Test
    public void commentAfterToken() {
      assertLexResult("-//comment\n", "D");
    }

    @Test
    public void commentLines() {
      assertLexResult("//comment 1\n-//comment2\n//comment2\n-//comment4;", "D,D");
    }
  }

  @Nested
  public class MultipleTokenTests {
    @Test
    public void simpleSequence() {
      assertLexResult("{}", "OB,CB");
    }

    @Test
    public void complexSequence() {
      assertLexResult("FSM:fsm{this}", "#FSM#,C,#fsm#,OB,#this#,CB");
    }

    @Test
    public void allTokens() {
      assertLexResult("{}()<>-: name .", "OB,CB,OP,CP,OA,CA,D,C,#name#,E1/15");
    }

    @Test
    public void multipleLines() {
      assertLexResult("FSM:fsm.\n{bob-.}", "#FSM#,C,#fsm#,E1/8,OB,#bob#,D,E2/6,CB");
    }
  }

}
