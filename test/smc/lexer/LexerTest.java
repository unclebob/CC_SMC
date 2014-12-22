package smc.lexer;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;

@RunWith(HierarchicalContextRunner.class)
public class LexerTest implements TokenCollector {
  String tokens = "";
  Lexer lexer;
  private boolean firstToken = true;

  @Before
  public void setUp() throws Exception {
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

  public class SingleTokenTests {
    @Test
    public void findsOpenBrace() throws Exception {
      assertLexResult("{", "OB");
    }

    @Test
    public void findsClosedBrace() throws Exception {
      assertLexResult("}", "CB");
    }

    @Test
    public void findsOpenParen() throws Exception {
      assertLexResult("(", "OP");
    }

    @Test
    public void findsClosedParen() throws Exception {
      assertLexResult(")", "CP");
    }

    @Test
    public void findsOpenAngle() throws Exception {
      assertLexResult("<", "OA");
    }

    @Test
    public void findsClosedAngle() throws Exception {
      assertLexResult(">", "CA");
    }

    @Test
    public void findsDash() throws Exception {
      assertLexResult("-", "D");
    }

    @Test
    public void findStarAsDash() throws Exception {
      assertLexResult("*", "D");
    }

    @Test
    public void findsColon() throws Exception {
      assertLexResult(":", "C");
    }

    @Test
    public void findsSimpleName() throws Exception {
      assertLexResult("name", "#name#");
    }

    @Test
    public void findComplexName() throws Exception {
      assertLexResult("Room_222", "#Room_222#");
    }

    @Test
    public void error() throws Exception {
      assertLexResult(".", "E1/1");
    }

    @Test
    public void nothingButWhiteSpace() throws Exception {
      assertLexResult(" ", "");
    }

    @Test
    public void whiteSpaceBefore() throws Exception {
      assertLexResult("  \t\n  -", "D");
    }
  }

  public class CommentTests {
    @Test
    public void commentAfterToken() throws Exception {
      assertLexResult("-//comment\n", "D");
    }

    @Test
    public void commentLines() throws Exception {
      assertLexResult("//comment 1\n-//comment2\n//comment2\n-//comment4;", "D,D");
    }
  }

  public class MultipleTokenTests {
    @Test
    public void simpleSequence() throws Exception {
      assertLexResult("{}", "OB,CB");
    }

    @Test
    public void complexSequence() throws Exception {
      assertLexResult("FSM:fsm{this}", "#FSM#,C,#fsm#,OB,#this#,CB");
    }

    @Test
    public void allTokens() throws Exception {
      assertLexResult("{}()<>-: name .", "OB,CB,OP,CP,OA,CA,D,C,#name#,E1/15");
    }

    @Test
    public void multipleLines() throws Exception {
      assertLexResult("FSM:fsm.\n{bob-.}", "#FSM#,C,#fsm#,E1/8,OB,#bob#,D,E2/6,CB");
    }
  }

}
