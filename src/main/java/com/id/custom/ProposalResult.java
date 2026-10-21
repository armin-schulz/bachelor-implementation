package com.id.custom;

/**
 * @param precisionCategories null for exact scores
 * @param recallCategories    null for exact scores
 */
public record ProposalResult(String ident, String code, boolean nonSeg, int proposalIndex, int proposalSize,
                             int referenceSize, String config, Scores scores,
                             CategoryCounts precisionCategories, CategoryCounts recallCategories) {

  public static final String HEADER =
     "ident\tcode\tnon seg\tproposal index\tproposal size\treference size\tconfig\tprecision\trecall\tf"
        + "\tp eq\tp gt\tp lt\tp neq\tp ambiguous\tr eq\tr gt\tr lt\tr neq\tr ambiguous";
  private static final int FIELDS = HEADER.split("\t").length;

  /**
   * inverse of toLine, scores are parsed exactly as written
   *
   * @throws IllegalArgumentException on malformed line
   */
  public static ProposalResult fromLine(String line) {
    String[] cs = line.split("\t", -1);
    if (cs.length != FIELDS) {
      throw new IllegalArgumentException("expected " + FIELDS + " fields: " + line);
    }
    return new ProposalResult(cs[0], cs[1], parseBoolean(cs[2]), Integer.parseInt(cs[3]),
       Integer.parseInt(cs[4]), Integer.parseInt(cs[5]), cs[6],
       new Scores(Double.parseDouble(cs[7]), Double.parseDouble(cs[8]), Double.parseDouble(cs[9])),
       CategoryCounts.fromFields(cs, 10), CategoryCounts.fromFields(cs, 10 + CategoryCounts.FIELDS));
  }

  private static boolean parseBoolean(String s) {
    if (!s.equals("true") && !s.equals("false")) {
      throw new IllegalArgumentException("not a boolean: " + s);
    }
    return s.equals("true");
  }

  /**
   * @return tab separated line, scores unrounded, empty category fields for exact scores
   */
  public String toLine() {
    return ident + "\t" + code + "\t" + nonSeg + "\t" + proposalIndex + "\t" + proposalSize
       + "\t" + referenceSize + "\t" + config
       + "\t" + scores.precision() + "\t" + scores.recall() + "\t" + scores.f()
       + "\t" + toFields(precisionCategories) + "\t" + toFields(recallCategories);
  }

  private static String toFields(CategoryCounts counts) {
    return counts == null ? CategoryCounts.EMPTY_FIELDS : counts.toFields();
  }
}
