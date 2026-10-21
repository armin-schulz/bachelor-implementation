package com.id.ic;

import com.id.hierarchy.Hierarchy;

public final class SanchezInformationContent extends InformationContent {

  public SanchezInformationContent(Hierarchy hierarchy) {
    super("Sanchez", hierarchy, compute(hierarchy));
  }

  /**
   * @return normed ics of concepts in order of internal of concepts in hierarchy
   */
  private static double[] compute(Hierarchy hierarchy) {
    final double leafCountPlusOne = hierarchy.numberOfLeaves() + 1.0;
    final double normalizationFactor = Math.log(leafCountPlusOne);
    double[] values = new double[hierarchy.numberOfConcepts()];
    for (int i = 0; i < hierarchy.numberOfConcepts(); i++) {
      long c = hierarchy.concept(i).id();
      int nbrLeaves = hierarchy.numberOfLeafSubConcepts(c);
      int nbrSuperConcepts = hierarchy.numberOfSuperConcepts(c);
      double numerator = ((double) nbrLeaves / (nbrSuperConcepts + 1)) + 1;
      double ic = -Math.log(numerator / leafCountPlusOne);
      double normIc = ic / normalizationFactor;
      values[i] = normIc;
    }
    return values;
  }
}
