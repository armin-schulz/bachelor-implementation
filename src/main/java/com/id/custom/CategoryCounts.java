package com.id.custom;

/**
 * number of concepts per is-a category and number of concepts whose category depends on tie breaking
 */
public record CategoryCounts(int eq, int gt, int lt, int neq, int ambiguous) {

  public static final String EMPTY_FIELDS = "\t\t\t\t";
  public static final int FIELDS = 5;

  /**
   * @return counts from fields at offset, null if all fields are empty
   */
  public static CategoryCounts fromFields(String[] cs, int offset) {
    boolean empty = true;
    for (int i = offset; i < offset + FIELDS; i++) {
      empty &= cs[i].isEmpty();
    }
    if (empty) {
      return null;
    }
    return new CategoryCounts(Integer.parseInt(cs[offset]), Integer.parseInt(cs[offset + 1]),
       Integer.parseInt(cs[offset + 2]), Integer.parseInt(cs[offset + 3]), Integer.parseInt(cs[offset + 4]));
  }

  public int total() {
    return eq + gt + lt + neq;
  }

  public CategoryCounts plus(CategoryCounts other) {
    return new CategoryCounts(eq + other.eq, gt + other.gt, lt + other.lt, neq + other.neq,
       ambiguous + other.ambiguous);
  }

  /**
   * @return tab separated counts
   */
  public String toFields() {
    return eq + "\t" + gt + "\t" + lt + "\t" + neq + "\t" + ambiguous;
  }
}
