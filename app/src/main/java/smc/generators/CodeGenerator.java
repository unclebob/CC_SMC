package smc.generators;

import smc.OptimizedStateMachine;
import smc.generators.nestedSwitchCaseGenerator.NSCGenerator;
import smc.generators.nestedSwitchCaseGenerator.NSCNodeVisitor;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

public abstract class CodeGenerator {
  protected final OptimizedStateMachine optimizedStateMachine;
  protected final String outputDirectory;
  protected final Map<String, String> flags;

  public CodeGenerator(OptimizedStateMachine optimizedStateMachine,
                       String outputDirectory,
                       Map<String, String> flags) {
    this.optimizedStateMachine = optimizedStateMachine;
    this.outputDirectory = outputDirectory;
    this.flags = flags;
  }

  protected Path getOutputPath(String outputFileName) {
    Path outputPath;
    if (outputDirectory == null)
      outputPath = FileSystems.getDefault().getPath(outputFileName);
    else
      outputPath = FileSystems.getDefault().getPath(outputDirectory, outputFileName);
    return outputPath;
  }

  public void generate() throws IOException {
    NSCGenerator nscGenerator = new NSCGenerator();
    nscGenerator.generate(optimizedStateMachine).accept(getImplementer());
    writeFiles();
  }

  protected abstract NSCNodeVisitor getImplementer();

  protected abstract void writeFiles() throws IOException;
}
