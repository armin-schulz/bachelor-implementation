package com.id.custom;

import com.id.hierarchy.Hierarchy;
import com.id.similarity.Similarity;

import java.util.Arrays;
import java.util.List;

/**
 * exact and soft measures of a proposal against its reference, expects lists without duplicates
 */
public final class ProposalEvaluation {

  private final Hierarchy hierarchy;

  public ProposalEvaluation(Hierarchy hierarchy) {
    this.hierarchy = hierarchy;
  }

  /**
   * @return exact precision and recall
   */
  public Scores exact(List<Long> proposal, List<Long> reference) {
    long hits = proposal.stream().filter(reference::contains).count();
    return Scores.of((double) hits / proposal.size(), (double) hits / reference.size());
  }

  /**
   * @return soft precision as mean of best similarities per proposal concept,
   * soft recall as mean of best similarities per reference concept,
   * categories of each concept with its best matching concept
   */
  public SoftResult soft(List<Long> proposal, List<Long> reference, Similarity similarity) {
    double[][] values = new double[proposal.size()][reference.size()];
    for (int p = 0; p < proposal.size(); p++) {
      for (int r = 0; r < reference.size(); r++) {
        values[p][r] = similarity.of(proposal.get(p), reference.get(r));
      }
    }

    double[] bestForProposal = new double[proposal.size()];
    CategoryCounter precisionCategories = new CategoryCounter();
    for (int p = 0; p < proposal.size(); p++) {
      BestMatch best = bestMatch(proposal.get(p), reference, values[p], true);
      bestForProposal[p] = best.value();
      precisionCategories.add(best);
    }

    double[] bestForReference = new double[reference.size()];
    CategoryCounter recallCategories = new CategoryCounter();
    for (int r = 0; r < reference.size(); r++) {
      double[] column = new double[proposal.size()];
      for (int p = 0; p < proposal.size(); p++) {
        column[p] = values[p][r];
      }
      BestMatch best = bestMatch(reference.get(r), proposal, column, false);
      bestForReference[r] = best.value();
      recallCategories.add(best);
    }

    return new SoftResult(Scores.of(mean(bestForProposal), mean(bestForReference)),
       precisionCategories.toCounts(), recallCategories.toCounts());
  }

  /**
   * selects partner from candidates with exactly maximal similarity, prefers identical concept, then smallest sct id
   *
   * @param values             similarities of concept to each candidate
   * @param conceptIsMapping   whether concept is from proposal, candidates are from the other side
   */
  private BestMatch bestMatch(long concept, List<Long> candidates, double[] values, boolean conceptIsMapping) {
    double maxValue = Arrays.stream(values).max().orElseThrow();
    long partner = 0;
    boolean hasPartner = false;
    IsaCategory firstCategory = null;
    boolean ambiguous = false;
    for (int i = 0; i < values.length; i++) {
      if (values[i] != maxValue) {
        continue;
      }
      long candidate = candidates.get(i);
      IsaCategory category = category(concept, candidate, conceptIsMapping);
      if (firstCategory == null) {
        firstCategory = category;
      } else if (category != firstCategory) {
        ambiguous = true;
      }
      if (!hasPartner || (partner != concept && (candidate == concept || candidate < partner))) {
        partner = candidate;
        hasPartner = true;
      }
    }
    return new BestMatch(maxValue, category(concept, partner, conceptIsMapping), ambiguous);
  }

  private IsaCategory category(long concept, long partner, boolean conceptIsMapping) {
    return conceptIsMapping
       ? IsaCategory.of(hierarchy, concept, partner)
       : IsaCategory.of(hierarchy, partner, concept);
  }

  private static double mean(double[] values) {
    return Arrays.stream(values).average().orElseThrow();
  }

  private record BestMatch(double value, IsaCategory category, boolean ambiguous) {
  }

  private static final class CategoryCounter {
    private final int[] counts = new int[IsaCategory.values().length];
    private int ambiguous;

    private void add(BestMatch best) {
      counts[best.category().ordinal()]++;
      if (best.ambiguous()) {
        ambiguous++;
      }
    }

    private CategoryCounts toCounts() {
      return new CategoryCounts(counts[IsaCategory.EQ.ordinal()], counts[IsaCategory.GT.ordinal()],
         counts[IsaCategory.LT.ordinal()], counts[IsaCategory.NEQ.ordinal()], ambiguous);
    }
  }
}
