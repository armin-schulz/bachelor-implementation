package com.id.custom;

import com.id.hierarchy.Hierarchy;

/**
 * is-a categories of a pair of mapping and reference concept
 */
public enum IsaCategory {
  /**
   * mapping equals reference
   */
  EQ,
  /**
   * mapping is more general than reference
   */
  GT,
  /**
   * mapping is more specific than reference
   */
  LT,
  /**
   * mapping and reference are not hierarchically comparable
   */
  NEQ;

  public static IsaCategory of(Hierarchy hierarchy, long mapping, long reference) {
    if (mapping == reference) {
      return EQ;
    }
    if (hierarchy.isSubConceptOf(reference, mapping)) {
      return GT;
    }
    if (hierarchy.isSubConceptOf(mapping, reference)) {
      return LT;
    }
    return NEQ;
  }
}
