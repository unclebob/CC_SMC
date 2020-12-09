package smc.generators;

import smc.OptimizedStateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;
import smc.implementers.GoNestedSwitchCaseImplementer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class GoCodeGenerator extends CodeGenerator {
  private GoNestedSwitchCaseImplementer implementer;

  public GoCodeGenerator(OptimizedStateMachine optimizedStateMachine,
                          String outputDirectory,
                          Map<String, String> flags) {
    super(optimizedStateMachine, outputDirectory, flags);
    implementer = new GoNestedSwitchCaseImplementer(flags);
  }

  protected NSCNodeVisitor getImplementer() {
    return implementer;
  }

  public void writeFiles() throws IOException {
    String outputFileName = camelToSnake(optimizedStateMachine.header.fsm + ".go");
    Files.write(getOutputPath(outputFileName), implementer.getOutput().getBytes());
  }

  private static String camelToSnake(String s) {
    return s.replaceAll("([a-z])([A-Z]+)", "$1_$2").toLowerCase();
  }
}
