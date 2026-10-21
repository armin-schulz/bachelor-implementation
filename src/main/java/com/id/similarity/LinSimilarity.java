package com.id.similarity;

import com.id.ic.InformationContent;

public final class LinSimilarity implements Similarity {

  private final String name;
  private final InformationContent ic;

  public LinSimilarity(InformationContent ic) {
    this.name = "Lin-" + ic.name();
    this.ic = ic;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public double of(long a, long b) {
    if (a == b) {
      return 1;
    }
    return 2 * (ic.ofMica(a, b) / (ic.of(a) + ic.of(b)));
  }
}
