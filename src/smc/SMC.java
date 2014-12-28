package smc;

import com.cleancoder.args.Args;
import com.cleancoder.args.ArgsException;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;
import smc.implementers.CNestedSwitchCaseImplementer;
import smc.implementers.CppNestedSwitchCaseImplementer;
import smc.implementers.JavaNestedSwitchCaseImplementer;
import smc.lexer.Lexer;
import smc.optimizer.Optimizer;
import smc.parser.FsmSyntax;
import smc.parser.Parser;
import smc.parser.SyntaxBuilder;
import smc.semanticAnalyzer.SemanticStateMachine;
import smc.semanticAnalyzer.SemanticAnalyzer;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Map;

import static smc.parser.ParserEvent.EOF;

public class SMC {
  public static void main(String[] args) throws IOException {
    String argSchema = "o*,l*,f&";

    try {
      Args argParser = new Args(argSchema, args);
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
    private String outputDirectory = null;
    private String language = "Java";
    Map<String, String> flags = new HashMap<>();
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

      if (syntaxErrorCount == 0) {
        OptimizedStateMachine optimizedStateMachine = optimize(fsm);
        CodeGenerator generator = null;
        if (language.equalsIgnoreCase("java"))
          generator = new JavaCodeGenerator(optimizedStateMachine);
        else if (language.equalsIgnoreCase("c++"))
          generator = new CppCodeGenerator(optimizedStateMachine);
        else if (language.equalsIgnoreCase("c"))
          generator = new CCodeGenerator(optimizedStateMachine);
        else {
          System.out.println("Unknown language: " + language);
          System.exit(0);
        }
        generator.generate();
      }
    }

    private void extractCommandLineArguments() {
      if (argParser.has('o'))
        outputDirectory = argParser.getString('o');
      if (argParser.has('l'))
        language = argParser.getString('l');
      if (argParser.has('f'))
        flags = argParser.getMap('f');
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

    private OptimizedStateMachine optimize(FsmSyntax fsm) {
      SemanticStateMachine ast = new SemanticAnalyzer().analyze(fsm);
      return new Optimizer().optimize(ast);
    }

    public abstract class CodeGenerator {
      protected OptimizedStateMachine optimizedStateMachine;

      public CodeGenerator(OptimizedStateMachine optimizedStateMachine) {
        this.optimizedStateMachine = optimizedStateMachine;
      }

      protected Path getOutputPath(String outputFileName) {
        Path outputPath;
        if (outputDirectory == null)
          outputPath = FileSystems.getDefault().getPath(outputFileName);
        else
          outputPath = FileSystems.getDefault().getPath(outputDirectory, outputFileName);
        return outputPath;
      }

      protected void generate() throws IOException{
        NSCGenerator nscGenerator = new NSCGenerator();
        nscGenerator.generate(optimizedStateMachine).accept(getImplementer());
        writeFiles();
      }

      protected abstract NSCNodeVisitor getImplementer();

      protected abstract void writeFiles() throws IOException;
    }

    class JavaCodeGenerator extends CodeGenerator {
     private JavaNestedSwitchCaseImplementer implementer;

      public JavaCodeGenerator(OptimizedStateMachine optimizedStateMachine) {
        super(optimizedStateMachine);
        implementer = new JavaNestedSwitchCaseImplementer(flags);
      }

      protected NSCNodeVisitor getImplementer() {
        return implementer;
      }

      public void writeFiles() throws IOException {
        String outputFileName = optimizedStateMachine.header.fsm + ".java";
        Files.write(getOutputPath(outputFileName), implementer.getOutput().getBytes());
      }
    }

    class CppCodeGenerator extends CodeGenerator {

      private CppNestedSwitchCaseImplementer implementer;

      public CppCodeGenerator(OptimizedStateMachine optimizedStateMachine) {
        super(optimizedStateMachine);
        implementer = new CppNestedSwitchCaseImplementer(flags);
      }

      protected NSCNodeVisitor getImplementer() {
        return implementer;
      }

      public void writeFiles() throws IOException {
        String outputFileName = optimizedStateMachine.header.fsm + ".h";
        Files.write(getOutputPath(outputFileName), implementer.getOutput().getBytes());
      }
    }


    class CCodeGenerator extends CodeGenerator {
      private CNestedSwitchCaseImplementer implementer;

      public CCodeGenerator(OptimizedStateMachine optimizedStateMachine) {
        super(optimizedStateMachine);
        implementer = new CNestedSwitchCaseImplementer(flags);
      }

      protected NSCNodeVisitor getImplementer() {
        return implementer;
      }

      public void writeFiles() throws IOException {
        if (implementer.getErrors().size() > 0) {
          for (CNestedSwitchCaseImplementer.Error error : implementer.getErrors())
            System.out.println("Implementation error: " + error.name());
        } else {
          String fileName = optimizedStateMachine.header.fsm.toLowerCase();
          Files.write(getOutputPath(fileName + ".h"), implementer.getFsmHeader().getBytes());
          Files.write(getOutputPath(fileName + ".c"), implementer.getFsmImplementation().getBytes());
        }
      }
    }
  }
}
