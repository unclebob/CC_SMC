package smc;

import com.cleancoder.args.Args;
import com.cleancoder.args.ArgsException;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.implementers.CNestedSwitchCaseImplementer;
import smc.implementers.JavaNestedSwitchCaseImplementer;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.FsmSyntax;
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

public class SMC {
  public static void main(String[] args) throws IOException {
    Args argParser;
    String argSchema = "a,p*,o*,l*";
    try {
      argParser = new Args(argSchema, args);
      new SmcCompiler(args, argParser).run();
    } catch (ArgsException e) {
      System.out.println("usage: " + argSchema + " file");
      System.out.println(e.errorMessage());
      System.exit(0);
    }
  }

  private static class SmcCompiler {
    private String[] args;
    private Args argParser;
    private String javaPackage = null;
    private String outputDirectory = null;
    private String language = "Java";
    private SyntaxBuilder syntaxBuilder;
    private Parser parser;
    private Lexer lexer;

    public SmcCompiler(String[] args, Args argParser) {
      this.args = args;
      this.argParser = argParser;
    }

    public void run() throws IOException {
      extractCommandLineArguments();

      FsmSyntax fsm = compile(getSourceCode());
      int syntaxErrorCount = reportSyntaxErrors(fsm);

      if (syntaxErrorCount == 0)
        generateCode(fsm);
    }

    private void extractCommandLineArguments() {
      if (argParser.has('p'))
        javaPackage = argParser.getString('p');
      if (argParser.has('o'))
        outputDirectory = argParser.getString('o');
      if (argParser.has('l'))
        language = argParser.getString('l');
    }

    private String getSourceCode() throws IOException {
      String sourceFileName = args[argParser.nextArgument()];
      return new String(Files.readAllBytes(Paths.get(sourceFileName)));
    }

    private FsmSyntax compile(String smContent) {
      syntaxBuilder = new SyntaxBuilder();
      parser = new Parser(syntaxBuilder);
      lexer = new Lexer(parser);
      lexer.lex(smContent);
      parser.handleEvent(EOF, -1, -1);

      return syntaxBuilder.getFsm();
    }

    private int reportSyntaxErrors(FsmSyntax fsm) {
      int syntaxErrorCount = fsm.errors.size();
      System.out.println(String.format("Compiled with %d syntax error%s.", syntaxErrorCount, (syntaxErrorCount == 1 ? "" : "s")));

      for (FsmSyntax.SyntaxError error : fsm.errors)
        System.out.println(error.toString());
      return syntaxErrorCount;
    }

    private void generateCode(FsmSyntax fsm) throws IOException {
      StateMachine stateMachine = optimize(fsm);

      NSCGenerator generator = new NSCGenerator();
      if (language.equalsIgnoreCase("java"))
        generateJava(stateMachine, generator);
       else if (language.equalsIgnoreCase("c"))
        generateC(stateMachine, generator);
    }

    private void generateC(StateMachine stateMachine, NSCGenerator generator) throws IOException {
      CNestedSwitchCaseImplementer implementer = new CNestedSwitchCaseImplementer();
      generator.generate(stateMachine).accept(implementer);
      if (implementer.getErrors().size() > 0) {
        for (CNestedSwitchCaseImplementer.Error error : implementer.getErrors())
          System.out.println("Implementation error: " + error.name());
      } else {
        String outputFilePrefix = stateMachine.header.fsm.toLowerCase();
        String headerFileName = outputFilePrefix + ".h";
        String implementationFileName = outputFilePrefix + ".c";

        Files.write(getOutputPath(headerFileName), implementer.getFsmHeader().getBytes());
        Files.write(getOutputPath(implementationFileName), implementer.getFsmImplementation().getBytes());
      }
    }

    private void generateJava(StateMachine stateMachine, NSCGenerator generator) throws IOException {
      JavaNestedSwitchCaseImplementer implementer = new JavaNestedSwitchCaseImplementer(javaPackage);
      generator.generate(stateMachine).accept(implementer);

      String outputFileName = stateMachine.header.fsm + ".java";

      Files.write(getOutputPath(outputFileName), implementer.getOutput().getBytes());
    }

    private StateMachine optimize(FsmSyntax fsm) {
      SemanticAnalyzer analyzer = new SemanticAnalyzer();
      Optimizer optimizer = new Optimizer();

      AbstractSyntaxTree ast = analyzer.analyze(fsm);
      return optimizer.optimize(ast);
    }

    private Path getOutputPath(String outputFileName) {
      Path outputPath;
      if (outputDirectory == null)
        outputPath = FileSystems.getDefault().getPath(outputFileName);
      else
        outputPath = FileSystems.getDefault().getPath(outputDirectory, outputFileName);
      return outputPath;
    }
  }
}
