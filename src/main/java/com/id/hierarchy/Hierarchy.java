package com.id.hierarchy;

public interface Hierarchy {

  /**
   * @return total number of concepts in hierarchy
   */
  int numberOfConcepts();

  /**
   * @return total number of leave concepts in hierarchy
   */
  int numberOfLeaves();

  /**
   * @return concept with internal id
   */
  Concept concept(int index);

  /**
   * @return internal index for sct id
   */
  int indexOfSctId(long sctId);

  /**
   * @return total number of super concepts of concept
   */
  int numberOfSuperConcepts(long sctId);

  /**
   * @return total number of sub concepts of concept
   */
  int numberOfSubConcepts(long sctId);

  /**
   * @return total number of leaf concept in sub concepts of concept
   */
  int numberOfLeafSubConcepts(long sctId);

  /**
   * false for identical concept ids
   */
  boolean isSubConceptOf(long sub, long sup);

  /**
   * @return longest path to root
   */
  int depth(long sctId);

  /**
   * finds sct id of common ancestor of maximum depth, including self. Break ties by smallest sct id
   */
  long lca(long a, long b);

  long commonAncestorMaximizing(long a, long b, double[] score);
}
