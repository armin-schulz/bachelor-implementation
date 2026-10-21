package com.id.aggregation;

import com.id.custom.CategoryCounts;
import com.id.custom.ProposalResult;
import com.id.custom.Scores;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * mean scores and summed is-a categories per config over the evaluation units of one variant
 */
public final class VariantAggregate {

  public static final String OVERALL_HEADER = "variant\tconfig\tunits\tprecision\trecall\tf";
  public static final String CATEGORY_HEADER = "variant\tconfig"
     + "\tp concepts\tp eq\tp gt\tp lt\tp neq\tp ambiguous"
     + "\tr concepts\tr eq\tr gt\tr lt\tr neq\tr ambiguous";

  private static final CategoryCounts ZERO = new CategoryCounts(0, 0, 0, 0, 0);

  private final String variant;
  private final Map<String, ScoreMean> means = new LinkedHashMap<>();
  private final Map<String, CategoryCounts> precisionCategories = new LinkedHashMap<>();
  private final Map<String, CategoryCounts> recallCategories = new LinkedHashMap<>();

  /**
   * @param configs     all configs in output order
   * @param softConfigs configs with is-a categories in output order
   */
  public VariantAggregate(String variant, List<String> configs, List<String> softConfigs) {
    this.variant = variant;
    configs.forEach(config -> means.put(config, new ScoreMean()));
    softConfigs.forEach(config -> {
      precisionCategories.put(config, ZERO);
      recallCategories.put(config, ZERO);
    });
  }

  /**
   * adds one evaluation unit (one proposal with the results of all configs)
   */
  public void add(Map<String, ProposalResult> proposal) {
    for (ProposalResult result : proposal.values()) {
      means.get(result.config()).add(result.scores());
      if (precisionCategories.containsKey(result.config())) {
        precisionCategories.merge(result.config(), result.precisionCategories(), CategoryCounts::plus);
        recallCategories.merge(result.config(), result.recallCategories(), CategoryCounts::plus);
      }
    }
  }

  public List<String> overallLines() {
    List<String> lines = new ArrayList<>();
    means.forEach((config, mean) -> {
      Scores scores = mean.mean();
      lines.add(variant + "\t" + config + "\t" + mean.count()
         + "\t" + scores.precision() + "\t" + scores.recall() + "\t" + scores.f());
    });
    return lines;
  }

  /**
   * @return per config the number of concepts and the shares of categories and ambiguous assignments
   */
  public List<String> categoryLines() {
    List<String> lines = new ArrayList<>();
    for (String config : precisionCategories.keySet()) {
      lines.add(variant + "\t" + config + "\t" + shares(precisionCategories.get(config))
         + "\t" + shares(recallCategories.get(config)));
    }
    return lines;
  }

  private static String shares(CategoryCounts counts) {
    double total = counts.total();
    return counts.total() + "\t" + counts.eq() / total + "\t" + counts.gt() / total + "\t" + counts.lt() / total
       + "\t" + counts.neq() / total + "\t" + counts.ambiguous() / total;
  }
}
