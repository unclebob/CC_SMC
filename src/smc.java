import smcsrc.smc.StateMachine;
import smcsrc.smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smcsrc.smc.implementers.JavaNestedSwitchCaseImplementer;
import smcsrc.smc.lexer.Lexer;
import smcsrc.smc.optimizer.Optimizer;
import smcsrc.smc.parser.Parser;
import smcsrc.smc.parser.SyntaxBuilder;
import smcsrc.smc.semanticAnalyzer.AbstractSyntaxTree;
import smcsrc.smc.semanticAnalyzer.SemanticAnalyzer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static smcsrc.smc.parser.ParserEvent.EOF;

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
