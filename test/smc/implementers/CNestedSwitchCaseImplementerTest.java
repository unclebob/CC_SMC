package smc.implementers;

import org.junit.Before;
import org.junit.Test;
import smc.StateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.generators.nestedSwitchCaseGenerator.NSCNode;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.AbstractSyntaxTree;
import smc.semanticAnalyzer.SemanticAnalyzer;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static smc.parser.ParserEvent.EOF;

public class CNestedSwitchCaseImplementerTest {
  private Lexer lexer;
  private Parser parser;
  private SyntaxBuilder builder;
  private SemanticAnalyzer analyzer;
  private Optimizer optimizer;
  private NSCGenerator generator;

  @Before
  public void setUp() throws Exception {
    builder = new SyntaxBuilder();
    parser = new Parser(builder);
    lexer = new Lexer(parser);
    analyzer = new SemanticAnalyzer();
    optimizer = new Optimizer();
    generator = new NSCGenerator();
  }

  private StateMachine produceStateMachine(String fsmSyntax) {
    lexer.lex(fsmSyntax);
    parser.handleEvent(EOF, -1, -1);
    AbstractSyntaxTree ast = analyzer.analyze(builder.getFsm());
    return optimizer.optimize(ast);
  }

  @Test
  public void noAction_shouldBeError() throws Exception {
    CNestedSwitchCaseImplementer implementer = new CNestedSwitchCaseImplementer();
    StateMachine sm = produceStateMachine("" +
      "Initial: I\n" +
      "Fsm: fsm\n" +
      "{" +
      "  I E I A" +
      "}");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    assertThat(implementer.getErrors().size(), is(1));
    assertThat(implementer.getErrors().get(0), is(CNestedSwitchCaseImplementer.Error.NO_ACTION));
  }


  @Test
  public void oneTransition() throws Exception {
    CNestedSwitchCaseImplementer implementer = new CNestedSwitchCaseImplementer();
    StateMachine sm = produceStateMachine("" +
      "Initial: I\n" +
      "Fsm: fsm\n" +
      "Actions: acts\n" +
      "{" +
      "  I E I A" +
      "}");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    assertThat(implementer.getFsmHeader(), equalTo("" +
      "#ifndef FSM_H\n" +
      "#define FSM_H\n" +
      "\n" +
      "struct acts;\n" +
      "struct fsm;\n" +
      "struct fsm *make_fsm(struct acts*);\n" +
      "void fsm_E(struct fsm*);\n" +
      "#endif\n"));

    assertThat(implementer.getFsmImplementation(), equalTo("" +
      "#include \"acts.h\"\n" +
      "#include \"fsm.h\"\n" +
      "\n" +
      "enum Event {E};\n" +
      "enum State {I};\n" +
      "\n" +
      "struct fsm {\n" +
      "\tenum State state;\n" +
      "\tstruct acts *actions;\n" +
      "};\n" +
      "\n" +
      "struct fsm *make_fsm(struct acts* actions) {\n" +
      "\tstruct fsm *fsm = malloc(sizeof(struct fsm));\n" +
      "\tfsm->actions = actions;\n" +
      "\tfsm->state = I;\n" +
      "\treturn fsm;\n" +
      "}\n" +
      "\n" +
      "static void setState(struct fsm *fsm, enum State state) {\n" +
      "\tfsm->state = state;\n" +
      "}\n" +
      "\n" +
      "static void A(struct turnstile *fsm) {\n" +
      "\tfsm->actions->A();\n" +
      "}\n" +
      "\n" +
      "static void processEvent(enum State state, enum Event event, struct fsm *fsm, char *event_name) {\n" +
      "switch (state) {\n" +
      "case I:\n" +
      "switch (event) {\n" +
      "case E:\n" +
      "setState(fsm, I);\n" +
      "A(fsm);\n" +
      "break;\n" +
      "\n" +
      "default:\n" +
      "(fsm->actions->unexpected_transition)(\"I\", event);\n" +
      "break;\n" +
      "}\n" +
      "break;\n\n" +
      "}\n" +
      "}\n" +
      "\n" +
      "void fsm_E(struct fsm* fsm) {\n" +
      "\tprocessEvent(fsm, E, fsm, \"E\");\n" +
      "}\n"));
  }
}
