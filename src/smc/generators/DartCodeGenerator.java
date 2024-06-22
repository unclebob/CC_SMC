package smc.generators;

import smc.OptimizedStateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;
import smc.implementers.DartNestedSwitchCaseImplementer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class DartCodeGenerator extends CodeGenerator {
  private DartNestedSwitchCaseImplementer implementer;

  public DartCodeGenerator(OptimizedStateMachine optimizedStateMachine,
                          String outputDirectory,
                          Map<String, String> flags) {
    super(optimizedStateMachine, outputDirectory, flags);
    implementer = new DartNestedSwitchCaseImplementer(flags);
  }

  protected NSCNodeVisitor getImplementer() {
    return implementer;
  }

  public void writeFiles() throws IOException {
    String outputFileName = optimizedStateMachine.header.fsm + ".dart";
    Files.write(getOutputPath(outputFileName), implementer.getOutput().getBytes());
  }
}
