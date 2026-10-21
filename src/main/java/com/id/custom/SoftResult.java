package com.id.custom;

/**
 * soft scores with is-a categories of best matching concepts from precision and recall side
 */
public record SoftResult(Scores scores, CategoryCounts precisionCategories, CategoryCounts recallCategories) {
}
