package com.id.ic;

import com.id.hierarchy.Hierarchy;

public sealed abstract class InformationContent permits SanchezInformationContent, SecoInformationContent {

  private final String name;

  private final Hierarchy hierarchy;
  /**
   * normed ics by their internal hierarchy index
   */
  private final double[] values;

  public String name() {
    return name;
  }

  protected InformationContent(String name, Hierarchy hierarchy, double[] values) {
    this.name = name;
    this.hierarchy = hierarchy;
    this.values = values;
  }

  /**
   * @return normed of
   */
  public final double of(long sctId) {
    return values[hierarchy.indexOfSctId(sctId)];
  }

  /**
   * @return sct id of common ancestor with highest of
   */
  public final long mica(long a, long b) {
    return hierarchy.commonAncestorMaximizing(a, b, values);
  }

  /**
   * @return common ancestor's with highest of of
   */
  public final double ofMica(long a, long b) {
    return of(mica(a, b));
  }
}
