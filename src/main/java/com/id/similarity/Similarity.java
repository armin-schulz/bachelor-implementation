package com.id.similarity;

public sealed interface Similarity permits WuPalmerSimilarity, LinSimilarity, JiangConrathSimilarity {

  String name();
  double of(long a, long b);
}
