package com.id.similarity;

import com.id.ic.InformationContent;

public final class JiangConrathSimilarity implements Similarity {

  private final String name;
  private final InformationContent ic;

  public JiangConrathSimilarity(InformationContent ic) {
    this.name = "JC-" + ic.name();
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
    double distance = (ic.of(a) + ic.of(b)) - 2 * ic.ofMica(a, b);
    return 1 - (distance / 2);
  }
}
