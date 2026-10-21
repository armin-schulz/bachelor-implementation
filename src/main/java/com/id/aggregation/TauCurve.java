package com.id.aggregation;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * tau curve psi(tau) = share of values >= tau, computed exactly from the sorted values
 */
public final class TauCurve {

  public static final String HEADER = "tau\tpsi\tcount";

  private double[] values = new double[1024];
  private int size;

  /**
   * @throws IllegalArgumentException if value is not in [0, 1]
   */
  public void add(double value) {
    if (!(value >= 0 && value <= 1)) {
      throw new IllegalArgumentException("value not in [0, 1]: " + value);
    }
    if (size == values.length) {
      values = Arrays.copyOf(values, 2 * size);
    }
    values[size++] = value;
  }

  /**
   * steps of the curve for pgfplots const plot mark right: psi of a point holds on the interval from the previous
   * tau (exclusive) to its tau (inclusive). Starts at (0, 1) and ends at (1, 0) unless a value is 0 or 1.
   * new BigDecimal(), else error
   *
   * @throws IllegalStateException if no values were added or the area under the curve differs from the mean,
   *                               compared exactly
   */
  public List<String> lines() {
    if (size == 0) {
      throw new IllegalStateException("tau curve of no values");
    }
    double[] sorted = Arrays.copyOf(values, size);
    Arrays.sort(sorted);
    List<String> lines = new ArrayList<>();
    if (sorted[0] > 0) {
      lines.add(line(0, size, size));
    }
    BigDecimal sum = BigDecimal.ZERO;
    BigDecimal area = BigDecimal.ZERO;
    double previous = 0;
    for (int i = 0; i < size; i++) {
      sum = sum.add(BigDecimal.valueOf(sorted[i]));
      if (i == 0 || sorted[i] != sorted[i - 1]) {
        int atLeast = size - i;
        lines.add(line(sorted[i], atLeast, size));
        area = area.add(BigDecimal.valueOf(sorted[i]).subtract(BigDecimal.valueOf(previous))
           .multiply(BigDecimal.valueOf(atLeast)));
        previous = sorted[i];
      }
    }
    if (previous < 1) {
      lines.add(line(1, 0, size));
    }
    if (area.compareTo(sum) != 0) {
      throw new IllegalStateException("area under tau curve " + area + " differs from sum of values " + sum);
    }
    return lines;
  }

  private static String line(double tau, int atLeast, int size) {
    return tau + "\t" + (double) atLeast / size + "\t" + atLeast;
  }
}
