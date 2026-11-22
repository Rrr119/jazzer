package com.example.library;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.Test;

class ExpressionEvaluatorTest {
  @Test
  void publicApiStaysStable() throws NoSuchMethodException {
    Method evaluate = ExpressionEvaluator.class.getMethod("evaluate", String.class);

    assertTrue(Modifier.isPublic(evaluate.getModifiers()));
    assertTrue(Modifier.isStatic(evaluate.getModifiers()));
    assertEquals(long.class, evaluate.getReturnType());
    assertEquals(1, evaluate.getParameterCount());
    assertTrue(Modifier.isFinal(ExpressionEvaluator.class.getModifiers()));
  }

  @Test
  void evaluatesWithOperatorPrecedence() {
    assertEquals(7L, ExpressionEvaluator.evaluate("1 + 2 * 3"));
    assertEquals(9L, ExpressionEvaluator.evaluate("1 + 2 * 3 + 2"));
    assertEquals(-3L, ExpressionEvaluator.evaluate("1 - 2 * 2"));
  }

  @Test
  void evaluatesParenthesesAndUnaryOperators() {
    assertEquals(9L, ExpressionEvaluator.evaluate("(1 + 2) * 3"));
    assertEquals(-5L, ExpressionEvaluator.evaluate("-(2 + 3)"));
    assertEquals(5L, ExpressionEvaluator.evaluate("-(-5)"));
    assertEquals(8L, ExpressionEvaluator.evaluate("2 * (3 + 1)"));
  }

  @Test
  void handlesWhitespaceDivisionAndModulo() {
    assertEquals(6L, ExpressionEvaluator.evaluate(" 20 / 3 "));
    assertEquals(2L, ExpressionEvaluator.evaluate("20 % 3"));
    assertEquals(1L, ExpressionEvaluator.evaluate("\t10\n%\r3 "));
    assertEquals(-3L, ExpressionEvaluator.evaluate("-7 / 2"));
  }

  @Test
  void rejectsMalformedInputAndOverflow() {
    assertThrows(NullPointerException.class, () -> ExpressionEvaluator.evaluate(null));
    assertThrows(IllegalArgumentException.class, () -> ExpressionEvaluator.evaluate(""));
    assertThrows(IllegalArgumentException.class, () -> ExpressionEvaluator.evaluate("1 +"));
    assertThrows(IllegalArgumentException.class, () -> ExpressionEvaluator.evaluate("(1 + 2"));
    assertThrows(IllegalArgumentException.class, () -> ExpressionEvaluator.evaluate("1 2"));
    assertThrows(IllegalArgumentException.class, () -> ExpressionEvaluator.evaluate("1 / 0"));
    assertThrows(IllegalArgumentException.class, () -> ExpressionEvaluator.evaluate("1 % 0"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExpressionEvaluator.evaluate("9223372036854775807 + 1"));
    assertThrows(
        IllegalArgumentException.class,
        () -> ExpressionEvaluator.evaluate("(-9223372036854775807 - 1) / -1"));
  }
}
