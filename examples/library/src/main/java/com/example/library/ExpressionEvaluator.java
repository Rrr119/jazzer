package com.example.library;

import java.util.Objects;

/**
 * A small, dependency-free integer expression evaluator.
 *
 * <p>The public API intentionally consists of a single method so that it can be used both by
 * regular callers and by fuzz targets without adapters.
 */
public final class ExpressionEvaluator {
  private static final int MAX_EXPRESSION_LENGTH = 4096;
  private static final int MAX_NESTING_DEPTH = 128;

  private ExpressionEvaluator() {}

  /**
   * Evaluates an integer expression.
   *
   * <p>Supported operators are {@code +}, {@code -}, {@code *}, {@code /}, {@code %}, and
   * parentheses. Whitespace between tokens is ignored.
   *
   * @param expression the expression to evaluate
   * @return the exact integer result
   * @throws NullPointerException if {@code expression} is {@code null}
   * @throws IllegalArgumentException if the expression is malformed, too large, too deeply nested,
   *     divides by zero, or overflows an {@code long}
   */
  public static long evaluate(String expression) {
    Objects.requireNonNull(expression, "expression");
    if (expression.length() > MAX_EXPRESSION_LENGTH) {
      throw new IllegalArgumentException(
          "Expression is longer than " + MAX_EXPRESSION_LENGTH + " characters");
    }

    Parser parser = new Parser(expression);
    long result = parser.parseAdditive(0);
    parser.skipWhitespace();
    if (!parser.atEnd()) {
      throw parser.invalid("Unexpected trailing input");
    }
    return result;
  }

  private static final class Parser {
    private final String expression;
    private int index;

    private Parser(String expression) {
      this.expression = expression;
    }

    private long parseAdditive(int depth) {
      long value = parseMultiplicative(depth);
      while (true) {
        skipWhitespace();
        if (consume('+')) {
          value = addExact(value, parseMultiplicative(depth));
        } else if (consume('-')) {
          value = subtractExact(value, parseMultiplicative(depth));
        } else {
          return value;
        }
      }
    }

    private long parseMultiplicative(int depth) {
      long value = parseUnary(depth);
      while (true) {
        skipWhitespace();
        if (consume('*')) {
          value = multiplyExact(value, parseUnary(depth));
        } else if (consume('/')) {
          value = divideExact(value, parseUnary(depth));
        } else if (consume('%')) {
          value = moduloExact(value, parseUnary(depth));
        } else {
          return value;
        }
      }
    }

    private long parseUnary(int depth) {
      if (depth >= MAX_NESTING_DEPTH) {
        throw invalid("Expression nesting is too deep");
      }
      skipWhitespace();
      if (consume('+')) {
        return parseUnary(depth + 1);
      }
      if (consume('-')) {
        long value = parseUnary(depth + 1);
        if (value == Long.MIN_VALUE) {
          throw invalid("Unary minus overflows a long");
        }
        return -value;
      }
      return parsePrimary(depth);
    }

    private long parsePrimary(int depth) {
      skipWhitespace();
      if (consume('(')) {
        if (depth >= MAX_NESTING_DEPTH) {
          throw invalid("Expression nesting is too deep");
        }
        long value = parseAdditive(depth + 1);
        skipWhitespace();
        if (!consume(')')) {
          throw invalid("Expected ')'");
        }
        return value;
      }
      return parseNumber();
    }

    private long parseNumber() {
      skipWhitespace();
      int start = index;
      while (!atEnd() && isAsciiDigit(expression.charAt(index))) {
        index++;
      }
      if (start == index) {
        throw invalid("Expected a number or '('");
      }

      long value = 0;
      for (int i = start; i < index; i++) {
        int digit = expression.charAt(i) - '0';
        try {
          value = Math.addExact(Math.multiplyExact(value, 10L), digit);
        } catch (ArithmeticException exception) {
          throw invalid("Integer literal overflows a long");
        }
      }
      return value;
    }

    private static boolean isAsciiDigit(char value) {
      return value >= '0' && value <= '9';
    }

    private void skipWhitespace() {
      while (!atEnd() && Character.isWhitespace(expression.charAt(index))) {
        index++;
      }
    }

    private boolean consume(char expected) {
      if (!atEnd() && expression.charAt(index) == expected) {
        index++;
        return true;
      }
      return false;
    }

    private boolean atEnd() {
      return index == expression.length();
    }

    private IllegalArgumentException invalid(String message) {
      return new IllegalArgumentException(message + " at index " + index);
    }
  }

  private static long addExact(long left, long right) {
    try {
      return Math.addExact(left, right);
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("Addition overflows a long");
    }
  }

  private static long subtractExact(long left, long right) {
    try {
      return Math.subtractExact(left, right);
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("Subtraction overflows a long");
    }
  }

  private static long multiplyExact(long left, long right) {
    try {
      return Math.multiplyExact(left, right);
    } catch (ArithmeticException exception) {
      throw new IllegalArgumentException("Multiplication overflows a long");
    }
  }

  private static long divideExact(long left, long right) {
    if (right == 0) {
      throw new IllegalArgumentException("Division by zero");
    }
    if (left == Long.MIN_VALUE && right == -1) {
      throw new IllegalArgumentException("Division overflows a long");
    }
    return left / right;
  }

  private static long moduloExact(long left, long right) {
    if (right == 0) {
      throw new IllegalArgumentException("Modulo by zero");
    }
    return left % right;
  }
}
