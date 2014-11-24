import smc.StateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.implementers.JavaNestedSwitchCaseImplementer;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.AbstractSyntaxTree;
import smc.semanticAnalyzer.SemanticAnalyzer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static smc.parser.ParserEvent.EOF;

public class smc {
  public static void main(String[] args) throws IOException {
    String fileName = args[0];
    Path filePath = FileSystems.getDefault().getPath(fileName);
    String smContent = new String(Files.readAllBytes(Paths.get(fileName)));

    SyntaxBuilder builder = new SyntaxBuilder();
    Parser parser = new Parser(builder);
    Lexer lexer = new Lexer(parser);
    SemanticAnalyzer analyzer = new SemanticAnalyzer();
    Optimizer optimizer = new Optimizer();
    NSCGenerator generator = new NSCGenerator();

    lexer.lex(smContent);
    parser.handleEvent(EOF, -1, -1);
    AbstractSyntaxTree ast = analyzer.analyze(builder.getFsm());
    StateMachine stateMachine = optimizer.optimize(ast);

    JavaNestedSwitchCaseImplementer implementer = new JavaNestedSwitchCaseImplementer();
    generator.generate(stateMachine).accept(implementer);

    String outputFileName = stateMachine.header.fsm + ".java";
    Path outputPath = FileSystems.getDefault().getPath(outputFileName);
    Files.write(outputPath, implementer.getOutput().getBytes());
  }
}
