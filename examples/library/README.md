# Fuzzing a Java library with Jazzer

This example shows the smallest complete integration for a Java library:

- `ExpressionEvaluator` is the library under test and exposes the stable public method
  `ExpressionEvaluator.evaluate(String)`.
- `ExpressionEvaluatorFuzzer` is the Jazzer harness. It consumes input through the existing
  `FuzzedDataProvider` API and only catches the documented exception for invalid expressions.
- `ExpressionEvaluatorTest` contains five library-level JUnit tests, including an API-shape test
  that protects the public method signature.
- The fuzz target emits both a human-readable coverage report and a JaCoCo `.exec` dump. Its
  `fuzzerTearDown` method verifies the library appears in the report and that the dump is non-empty.

Run the library tests:

```shell
bazel test //examples/library:ExpressionEvaluatorTest
```

Run the coverage-guided fuzz target:

```shell
bazel test //examples/library:ExpressionEvaluatorFuzzer
```

The fuzz target writes `coverage.txt` and `coverage.exec`. When run under `bazel test`, copies are
also published to `bazel-testlogs/examples/library/ExpressionEvaluatorFuzzer/test.outputs/`.

The initial corpus in `src/test/resources/seeds` contains valid expressions so that the
fuzzer starts with full parser paths rather than having to discover the grammar from random bytes.
