package com.id.aggregation;

import java.util.List;
import java.util.stream.Stream;

/**
 * names of the configs as written by Calculation, soft config names are the names of the similarities
 */
public final class Configs {

  public static final String EXACT = "exact";
  public static final String MAIN = "Lin-Sanchez";
  public static final List<String> SOFT = List.of(MAIN, "Lin-Seco", "JC-Sanchez", "JC-Seco", "WP");
  public static final List<String> ALL = Stream.concat(Stream.of(EXACT), SOFT.stream()).toList();

  private Configs() {
  }
}
