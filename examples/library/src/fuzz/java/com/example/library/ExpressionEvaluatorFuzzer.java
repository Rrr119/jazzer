package com.example.library;

import com.code_intelligence.jazzer.api.FuzzedDataProvider;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;

/**
 * Coverage-guided fuzz harness for {@link ExpressionEvaluator}.
 *
 * <p>The harness only relies on Jazzer's existing {@link FuzzedDataProvider} API. Boundaries
 * between valid and invalid inputs are not asserted against a reference implementation; malformed
 * expressions are expected to raise {@link IllegalArgumentException}.
 */
public final class ExpressionEvaluatorFuzzer {
  private static final String COVERAGE_REPORT_ENV = "COVERAGE_REPORT_FILE";
  private static final String COVERAGE_DUMP_ENV = "COVERAGE_DUMP_FILE";

  private ExpressionEvaluatorFuzzer() {}

  public static void fuzzerTestOneInput(FuzzedDataProvider data) {
    String expression = data.consumeRemainingAsString();
    try {
      ExpressionEvaluator.evaluate(expression);
    } catch (IllegalArgumentException expectedForInvalidInput) {
      // Invalid inputs are part of the fuzzing contract.
    }
  }

  public static void fuzzerTearDown() throws IOException {
    Path report = requiredFile(COVERAGE_REPORT_ENV);
    Path dump = requiredFile(COVERAGE_DUMP_ENV);

    List<String> lines = Files.readAllLines(report);
    String reportContents = String.join("\n", lines);
    assertPositiveCoverage(reportContents, "Branch coverage:");
    assertPositiveCoverage(reportContents, "Line coverage:");

    if (Files.size(dump) == 0) {
      throw new IllegalStateException("Coverage dump is empty: " + dump);
    }

    publishTestOutput(report);
    publishTestOutput(dump);
  }

  private static void assertPositiveCoverage(String report, String section) {
    int sectionStart = report.indexOf(section);
    String classPrefix = "ExpressionEvaluator.java: ";
    int classStart = sectionStart < 0 ? -1 : report.indexOf(classPrefix, sectionStart);
    if (classStart < 0) {
      throw new IllegalStateException("Coverage report does not contain the instrumented library");
    }

    int countsStart = classStart + classPrefix.length();
    int slash = report.indexOf('/', countsStart);
    if (slash < 0) {
      throw new IllegalStateException("Coverage report has an unexpected format");
    }
    long covered = Long.parseLong(report.substring(countsStart, slash));
    if (covered == 0) {
      throw new IllegalStateException("The library has no covered entries in: " + section);
    }
  }

  private static Path requiredFile(String environmentVariable) {
    String fileName = System.getenv(environmentVariable);
    if (fileName == null || fileName.isEmpty()) {
      throw new IllegalStateException("Missing environment variable " + environmentVariable);
    }
    Path path = Paths.get(fileName);
    if (!Files.isRegularFile(path) || path.toFile().length() == 0) {
      throw new IllegalStateException("Missing or empty coverage file: " + path);
    }
    return path;
  }

  private static void publishTestOutput(Path source) throws IOException {
    String outputDirectory = System.getenv("TEST_UNDECLARED_OUTPUTS_DIR");
    if (outputDirectory == null || outputDirectory.isEmpty()) {
      return;
    }
    Path destination = Paths.get(outputDirectory).resolve(source.getFileName());
    Files.createDirectories(destination.getParent());
    Files.copy(source, destination, StandardCopyOption.REPLACE_EXISTING);
  }
}
