package com.id.aggregation;

import com.id.custom.ProposalResult;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * results of all proposals of one text
 *
 * @param proposals per proposal index the results by config
 */
public record TextResult(String ident, String code, boolean nonSeg, List<Map<String, ProposalResult>> proposals) {

  /**
   * @param rows    all result rows of one text
   * @param configs expected configs of every proposal, defines the order of the result maps
   * @throws IllegalStateException if a proposal misses a config, has duplicate or unknown configs, proposal
   *                               indices have gaps or text attributes are inconsistent
   */
  public static TextResult of(List<ProposalResult> rows, List<String> configs) {
    ProposalResult first = rows.get(0);
    List<Map<String, ProposalResult>> proposals = new ArrayList<>();
    for (ProposalResult row : rows) {
      if (!row.ident().equals(first.ident()) || !row.code().equals(first.code()) || row.nonSeg() != first.nonSeg()
         || row.referenceSize() != first.referenceSize()) {
        throw new IllegalStateException("inconsistent text attributes for ident=" + first.ident());
      }
      if (!configs.contains(row.config())) {
        throw new IllegalStateException("unknown config " + row.config() + " for ident=" + first.ident());
      }
      while (proposals.size() <= row.proposalIndex()) {
        proposals.add(new LinkedHashMap<>());
      }
      Map<String, ProposalResult> proposal = proposals.get(row.proposalIndex());
      if (proposal.put(row.config(), row) != null) {
        throw new IllegalStateException("duplicate config " + row.config() + " for ident=" + first.ident()
           + " proposal=" + row.proposalIndex());
      }
    }
    for (int i = 0; i < proposals.size(); i++) {
      Map<String, ProposalResult> proposal = proposals.get(i);
      if (proposal.size() != configs.size()) {
        throw new IllegalStateException("missing configs for ident=" + first.ident() + " proposal=" + i);
      }
      int size = proposal.values().iterator().next().proposalSize();
      if (proposal.values().stream().anyMatch(row -> row.proposalSize() != size)) {
        throw new IllegalStateException("inconsistent proposal size for ident=" + first.ident() + " proposal=" + i);
      }
      Map<String, ProposalResult> ordered = new LinkedHashMap<>();
      configs.forEach(config -> ordered.put(config, proposal.get(config)));
      proposals.set(i, ordered);
    }
    return new TextResult(first.ident(), first.code(), first.nonSeg(), List.copyOf(proposals));
  }

  /**
   * variant B: proposal with the highest f of config, ties to fewer concepts, then to the lower proposal index
   */
  public Map<String, ProposalResult> best(String config) {
    Map<String, ProposalResult> best = null;
    for (Map<String, ProposalResult> proposal : proposals) {
      if (best == null || isBetter(proposal.get(config), best.get(config))) {
        best = proposal;
      }
    }
    return best;
  }

  private static boolean isBetter(ProposalResult candidate, ProposalResult best) {
    double f = candidate.scores().f();
    double bestF = best.scores().f();
    return f > bestF || (f == bestF && candidate.proposalSize() < best.proposalSize());
  }
}
