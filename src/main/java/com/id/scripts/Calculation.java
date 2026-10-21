package com.id.scripts;

import com.id.aggregation.Configs;
import com.id.custom.CategoryCounts;
import com.id.custom.ProposalEvaluation;
import com.id.custom.ProposalResult;
import com.id.custom.RefMappingLine;
import com.id.custom.Scores;
import com.id.custom.SoftResult;
import com.id.hierarchy.Hierarchy;
import com.id.hierarchy.SctHierarchy;
import com.id.ic.InformationContent;
import com.id.ic.SanchezInformationContent;
import com.id.ic.SecoInformationContent;
import com.id.sct.SctRelease;
import com.id.similarity.JiangConrathSimilarity;
import com.id.similarity.LinSimilarity;
import com.id.similarity.Similarity;
import com.id.similarity.WuPalmerSimilarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import static com.id.scripts.Exclusions.parseIds;

public class Calculation {

  private static final Logger LOGGER = LoggerFactory.getLogger(Calculation.class);

  private Calculation() {
  }

  public static void main(String[] args) {
    Path inputPath = Paths.get(args[0]);
    String sctReleasePath = args[1];
    Path outputPath = Paths.get(args[2]);
    try {
      Hierarchy hierarchy = new SctHierarchy(SctRelease.fromReleaseDirectory(sctReleasePath));
      List<Similarity> similarities = getSimilarities(hierarchy);
      LOGGER.info("Loaded. Start processing...");
      calculateAndWrite(inputPath, outputPath, new ProposalEvaluation(hierarchy), similarities);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * writes results for each mapping line of input, output is only opened after hierarchy is loaded
   */
  private static void calculateAndWrite(Path inputPath, Path outputPath, ProposalEvaluation evaluation,
                                        List<Similarity> similarities) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(inputPath, StandardCharsets.UTF_8);
         BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
      reader.readLine();
      writer.write(ProposalResult.HEADER);
      int lineNbr = 0;
      String line;
      while ((line = reader.readLine()) != null) {
        if (++lineNbr % 10_000 == 0) {
          LOGGER.info("Processing line {}...", lineNbr);
        }
        for (ProposalResult result : process(mappingFromLine(line), evaluation, similarities)) {
          writer.newLine();
          writer.write(result.toLine());
        }
      }
    }
  }

  /**
   * @return exact and soft results for each proposal of mapping
   */
  private static List<ProposalResult> process(RefMappingLine mapping, ProposalEvaluation evaluation,
                                              List<Similarity> similarities) {
    List<ProposalResult> results = new ArrayList<>();
    List<Long> reference = mapping.reference();
    for (int i = 0; i < mapping.proposals().size(); i++) {
      List<Long> proposal = mapping.proposals().get(i);
      Scores exact = evaluation.exact(proposal, reference);
      results.add(result(mapping, i, Configs.EXACT, exact, null, null));
      for (Similarity similarity : similarities) {
        SoftResult soft = evaluation.soft(proposal, reference, similarity);
        validate(mapping, i, similarity, exact, soft);
        results.add(result(mapping, i, similarity.name(), soft.scores(),  soft.precisionCategories(),
           soft.recallCategories()));
      }
    }
    return results;
  }

  private static ProposalResult result(RefMappingLine mapping, int proposalIndex, String config, Scores scores,
                                       CategoryCounts precisionCategories, CategoryCounts recallCategories) {
    return new ProposalResult(mapping.ident(), mapping.code(), mapping.nonSeg(), proposalIndex,
       mapping.proposals().get(proposalIndex).size(), mapping.reference().size(), config, scores,
       precisionCategories, recallCategories);
  }

  /**
   * asserts category counts match set sizes and exact hits, soft scores are not below exact scores
   */
  private static void validate(RefMappingLine mapping, int proposalIndex, Similarity similarity, Scores exact,
                               SoftResult soft) {
    int proposalSize = mapping.proposals().get(proposalIndex).size();
    int referenceSize = mapping.reference().size();
    long hits = Math.round(exact.precision() * proposalSize);
    CategoryCounts p = soft.precisionCategories();
    CategoryCounts r = soft.recallCategories();
    Scores s = soft.scores();
    if (p.total() != proposalSize || r.total() != referenceSize
       || p.eq() != hits || r.eq() != hits
       || s.precision() < exact.precision() || s.recall() < exact.recall() || s.f() < exact.f()) {
      throw new IllegalStateException("inconsistent result for ident=" + mapping.ident()
         + " proposal=" + proposalIndex + " config=" + similarity.name()
         + " exact=" + exact + " soft=" + soft);
    }
  }

  /**
   * @throws IllegalStateException if the similarity names differ from the soft configs expected by Evaluation
   */
  private static List<Similarity> getSimilarities(Hierarchy hierarchy) {
    InformationContent seco = new SecoInformationContent(hierarchy);
    InformationContent sanchez = new SanchezInformationContent(hierarchy);
    List<Similarity> similarities = List.of(
       new LinSimilarity(sanchez),
       new LinSimilarity(seco),
       new JiangConrathSimilarity(sanchez),
       new JiangConrathSimilarity(seco),
       new WuPalmerSimilarity(hierarchy));
    List<String> names = similarities.stream().map(Similarity::name).toList();
    if (!names.equals(Configs.SOFT)) {
      throw new IllegalStateException("similarity names " + names + " differ from configs " + Configs.SOFT);
    }
    return similarities;
  }

  private static RefMappingLine mappingFromLine(String line) {
    String[] cs = line.split("\t", -1);
    final String ident = cs[0];
    final boolean nonSeg = Boolean.parseBoolean(cs[1]);
    final String code = cs[2];
    List<Long> reference = parseIds(cs[3]);
    List<List<Long>> proposals = new ArrayList<>();
    for (String proposalString : cs[4].split("\\|")) {
      proposals.add(parseIds(proposalString));
    }
    return new RefMappingLine(ident, nonSeg, code, proposals, reference);
  }
}
