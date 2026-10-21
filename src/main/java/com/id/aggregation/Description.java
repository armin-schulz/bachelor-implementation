package com.id.aggregation;

import java.util.Arrays;

/**
 * descriptive statistics, std (n - 1), quartiles with linear interpolation between order statistics, whiskers
 * as the most extreme values within 1.5 interquartile ranges of the quartiles
 */
public record Description(int count, double mean, double std, double min, double lowerWhisker, double q1,
                          double median, double q3, double upperWhisker, double max) {

  public static final String HEADER = "count\tmean\tstd\tmin\tlower whisker\tq1\tmedian\tq3\tupper whisker\tmax";

  private static final double WHISKER_RANGE = 1.5;

  /**
   * @throws IllegalArgumentException if values are empty
   */
  public static Description of(double[] values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("description of no values");
    }
    double[] sorted = values.clone();
    Arrays.sort(sorted);
    int n = sorted.length;
    double mean = Arrays.stream(sorted).average().orElseThrow();
    double squares = Arrays.stream(sorted).map(v -> (v - mean) * (v - mean)).sum();
    double std = n > 1 ? Math.sqrt(squares / (n - 1)) : Double.NaN;
    double q1 = quantile(sorted, 0.25);
    double q3 = quantile(sorted, 0.75);
    double lowerLimit = q1 - WHISKER_RANGE * (q3 - q1);
    double upperLimit = q3 + WHISKER_RANGE * (q3 - q1);
    double lowerWhisker = Arrays.stream(sorted).filter(v -> v >= lowerLimit).min().orElseThrow();
    double upperWhisker = Arrays.stream(sorted).filter(v -> v <= upperLimit).max().orElseThrow();
    return new Description(n, mean, std, sorted[0], lowerWhisker, q1, quantile(sorted, 0.5), q3, upperWhisker,
       sorted[n - 1]);
  }

  private static double quantile(double[] sorted, double q) {
    double position = (sorted.length - 1) * q;
    int lower = (int) Math.floor(position);
    int upper = (int) Math.ceil(position);
    return sorted[lower] + (sorted[upper] - sorted[lower]) * (position - lower);
  }

  public String toFields() {
    return count + "\t" + mean + "\t" + std + "\t" + min + "\t" + lowerWhisker + "\t" + q1 + "\t" + median
       + "\t" + q3 + "\t" + upperWhisker + "\t" + max;
  }
}
