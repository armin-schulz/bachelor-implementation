package com.id.aggregation;

import com.id.custom.ProposalResult;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * tau curves of one variant for pairs of config and score
 */
public final class TauCurves {

  public record Key(String config, String score) {
  }

  private final String variant;
  private final Map<Key, TauCurve> curves = new LinkedHashMap<>();

  public TauCurves(String variant, List<Key> keys) {
    this.variant = variant;
    keys.forEach(key -> curves.put(key, new TauCurve()));
  }

  /**
   * adds one evaluation unit (proposal with the results of all configs)
   */
  public void add(Map<String, ProposalResult> proposal) {
    curves.forEach((key, curve) -> curve.add(proposal.get(key.config()).scores().get(key.score())));
  }

  /**
   * @return per file name variant_config_score the lines of the curve
   */
  public Map<String, List<String>> lines() {
    Map<String, List<String>> lines = new LinkedHashMap<>();
    curves.forEach((key, curve) -> lines.put(variant + "_" + key.config() + "_" + key.score(), curve.lines()));
    return lines;
  }
}
