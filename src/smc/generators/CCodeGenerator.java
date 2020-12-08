package smc.generators;

import smc.OptimizedStateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;
import smc.implementers.CNestedSwitchCaseImplementer;

import java.io.IOException;
import java.nio.file.Files;
import java.util.Map;

public class CCodeGenerator extends CodeGenerator {
  private CNestedSwitchCaseImplementer implementer;

  public CCodeGenerator(OptimizedStateMachine optimizedStateMachine,
                        String outputDirectory,
                        Map<String, String> flags) {
    super(optimizedStateMachine, outputDirectory, flags);
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
      String fileName = toLowerCamelCase(optimizedStateMachine.header.fsm);
      Files.write(getOutputPath(fileName + ".h"), implementer.getFsmHeader().getBytes());
      Files.write(getOutputPath(fileName + ".c"), implementer.getFsmImplementation().getBytes());
    }
  }

  static private String toLowerCamelCase(String s) {
    if (s.length() < 2) return s.toLowerCase();
    return s.substring(0, 1).toLowerCase() + s.substring(1);
  }
}
