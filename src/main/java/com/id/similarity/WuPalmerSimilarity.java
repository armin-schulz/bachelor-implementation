package com.id.similarity;

import com.id.hierarchy.Hierarchy;

public final class WuPalmerSimilarity implements Similarity {

  private final Hierarchy hierarchy;

  public WuPalmerSimilarity(Hierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }

  @Override
  public String name() {
    return "WP";
  }

  @Override
  public double of(long a, long b) {
    if (a == b) {
      return 1;
    }
    return 2 * ((double) hierarchy.depth(hierarchy.lca(a, b)) / (hierarchy.depth(a) + hierarchy.depth(b)));
  }
}
