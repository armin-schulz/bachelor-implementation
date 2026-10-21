package com.id.aggregation;

import com.id.custom.Scores;

/**
 * arithmetic means of precision, recall and f, f is averaged and not recomputed from the means
 */
public final class ScoreMean {

  private int count;
  private double precision;
  private double recall;
  private double f;

  public void add(Scores scores) {
    count++;
    precision += scores.precision();
    recall += scores.recall();
    f += scores.f();
  }

  public int count() {
    return count;
  }

  /**
   * @throws IllegalStateException if nothing was added
   */
  public Scores mean() {
    if (count == 0) {
      throw new IllegalStateException("mean of no scores");
    }
    return new Scores(precision / count, recall / count, f / count);
  }
}
