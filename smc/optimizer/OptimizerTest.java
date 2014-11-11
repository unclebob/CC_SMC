package smc.optimizer;

import de.bechte.junit.runners.context.HierarchicalContextRunner;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import smc.StateMachine;
import smc.lexer.Lexer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.AbstractSyntaxTree;
import smc.semanticAnalyzer.SemanticAnalyzer;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;
import static smc.parser.ParserEvent.EOF;

@RunWith(HierarchicalContextRunner.class)
public class OptimizerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;
  private Optimizer optimizer;

  @Before
  public void setUp() throws Exception {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
    analyzer = new SemanticAnalyzer();
    optimizer = new Optimizer();
  }

  private StateMachine produceStateMachine(String s) {
    lexer.lex("fsm:f initial:i actions:a " + s);
    parser.handleEvent(EOF, -1, -1);
    AbstractSyntaxTree ast = analyzer.analyze(builder.getFsm());
    return optimizer.optimize(ast);
  }

  public class BasicOptimizerFunctions {
    @Test
    public void header() throws Exception {
      StateMachine sm = produceStateMachine("{i e i -}");
      assertThat(sm.header.fsm, equalTo("f"));
      assertThat(sm.header.initial, equalTo("i"));
      assertThat(sm.header.actions, equalTo("a"));
    }

    @Test
    public void statesArePreserved() throws Exception {
      StateMachine sm = produceStateMachine("{i e s - s e i -}");
      assertThat(sm.states, contains("i", "s"));
    }

    @Test
    public void eventsArePreserved() throws Exception {
      StateMachine sm = produceStateMachine("{i e1 s - s e2 i -}");
      assertThat(sm.events, contains("e1", "e2"));
    }

    @Test
    public void actionsArePreserved() throws Exception {
      StateMachine sm = produceStateMachine("{i e1 s a1 s e2 i a2}");
      assertThat(sm.actions, contains("a1", "a2"));
    }

    @Test
    public void simpleStateMachine() throws Exception {
      StateMachine sm = produceStateMachine("{i e i a1}");
      assertThat(sm.transitions, hasSize(1));
      assertThat(sm.transitions.get(0).toString(), equalTo(
        "i {\n" +
          "  e i {a1}\n" +
          "}"));

    }
  } // Basic Optimizer Functions

  public class EntryAndExitActions {
    @Test
    public void entryFunctionsAdded() throws Exception {
      StateMachine sm = produceStateMachine(
        "" +
          "{" +
          "  i e s a1" +
          "  i e2 s a2" +
          "  s <n1 <n2 e i -" +
          "}");
      assertThat(sm.transitionsToString(), equalTo(
        "" +
          "i {\n" +
          "  e s {n1 n2 a1}\n" +
          "  e2 s {n1 n2 a2}\n" +
          "}\n" +
          "s {\n" +
          "  e i {}\n" +
          "}\n"));
    }

    @Test
        public void exitFunctionsAdded() throws Exception {
          StateMachine sm = produceStateMachine(
            "" +
              "{" +
              "  i e s a1" +
              "  i e2 s a2" +
              "  s >x1 >x2 e i -" +
              "}");
          assertThat(sm.transitionsToString(), equalTo(
            "" +
              "i {\n" +
              "  e s {a1 x1 x2}\n" +
              "  e2 s {a2 x1 x2}\n" +
              "}\n" +
              "s {\n" +
              "  e i {}\n" +
              "}\n"));
        }
  } // Entry and Exit Actions

}
