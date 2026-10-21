package com.id.custom;

import java.util.List;

public record Scores(double precision, double recall, double f) {

  public static final List<String> NAMES = List.of("precision", "recall", "f");

  /**
   * @param name one of NAMES
   */
  public double get(String name) {
    return switch (name) {
      case "precision" -> precision;
      case "recall" -> recall;
      case "f" -> f;
      default -> throw new IllegalArgumentException("unknown score " + name);
    };
  }

  /**
   * @return scores with f as harmonic mean, f set to 0 if precision and recall are 0
   */
  public static Scores of(double precision, double recall) {
    double f = precision + recall == 0 ? 0 : 2 * precision * recall / (precision + recall);
    return new Scores(precision, recall, f);
  }
}
