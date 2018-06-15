package smc.implementers;

import org.junit.Before;
import org.junit.Test;
import smc.OptimizedStateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.generators.nestedSwitchCaseGenerator.NSCNode;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.SemanticStateMachine;
import smc.semanticAnalyzer.SemanticAnalyzer;

import java.util.HashMap;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
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

  @Before
  public void setUp() throws Exception {
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
  public void noAction_shouldBeError() throws Exception {
    OptimizedStateMachine sm = produceStateMachine("" +
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
    OptimizedStateMachine sm = produceStateMachine("" +
      "Initial: I\n" +
      "Fsm: fsm\n" +
      "Actions: acts\n" +
      "{" +
      "  I E I A" +
      "}");
    NSCNode generatedFsm = generator.generate(sm);
    generatedFsm.accept(implementer);
    assertWhiteSpaceEquivalent(implementer.getFsmHeader(), "" +
      "#ifndef FSM_H\n" +
      "#define FSM_H\n" +
      "struct acts;\n" +
      "struct fsm;\n" +
      "struct fsm *make_fsm(struct acts*);\n" +
      "void fsm_E(struct fsm*);\n" +
      "#endif\n");

    assertWhiteSpaceEquivalent(implementer.getFsmImplementation(), "" +
      "#include <stdlib.h>\n" +
      "#include \"acts.h\"\n" +
      "#include \"fsm.h\"\n" +
      "" +
      "enum Event {E};\n" +
      "enum State {I};\n" +
      "" +
      "struct fsm {\n" +
      "  enum State state;\n" +
      "  struct acts *actions;\n" +
      "};\n" +
      "" +
      "struct fsm *make_fsm(struct acts* actions) {\n" +
      "  struct fsm *fsm = malloc(sizeof(struct fsm));\n" +
      "  fsm->actions = actions;\n" +
      "  fsm->state = I;\n" +
      "  return fsm;\n" +
      "}\n" +
      "" +
      "static void setState(struct fsm *fsm, enum State state) {\n" +
      "  fsm->state = state;\n" +
      "}\n" +
      "" +
      "static void A(struct fsm *fsm) {\n" +
      "  fsm->actions->A();\n" +
      "}\n" +
      "" +
      "static void processEvent(enum State state, enum Event event, struct fsm *fsm, char *event_name) {\n" +
      "  switch (state) {\n" +
      "    case I:\n" +
      "      switch (event) {\n" +
      "        case E:\n" +
      "          setState(fsm, I);\n" +
      "          A(fsm);\n" +
      "          break;\n" +
      "        default:\n" +
      "          (fsm->actions->unexpected_transition)(\"I\", event_name);\n" +
      "          break;\n" +
      "      }\n" +
      "      break;\n" +
      "  }\n" +
      "}\n" +
      "" +
      "void fsm_E(struct fsm* fsm) {\n" +
      "  processEvent(fsm->state, E, fsm, \"E\");\n" +
      "}\n");
  }
}
