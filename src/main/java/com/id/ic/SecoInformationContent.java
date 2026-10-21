package com.id.ic;

import com.id.hierarchy.Hierarchy;

public final class SecoInformationContent extends InformationContent {

  public SecoInformationContent(Hierarchy hierarchy) {
    super("Seco", hierarchy, compute(hierarchy));
  }
  /**
   * @return ics of concepts in order of internal of concepts in hierarchy
   */
  private static double[] compute(Hierarchy hierarchy) {
    int nbrConcepts = hierarchy.numberOfConcepts();
    double logNbrConcepts = Math.log(nbrConcepts);
    double[] values = new double[nbrConcepts];
    for (int i = 0; i < nbrConcepts; i++) {
      long c = hierarchy.concept(i).id();
      values[i] = 1.0 - Math.log(hierarchy.numberOfSubConcepts(c) + 1) / logNbrConcepts;
    }
    return values;
  }
}
