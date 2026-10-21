package com.id.scripts;

import com.id.aggregation.CodeAggregate;
import com.id.aggregation.Configs;
import com.id.aggregation.TauCurve;
import com.id.aggregation.TauCurves;
import com.id.aggregation.VariantAggregate;
import com.id.custom.ProposalResult;
import com.id.custom.Scores;
import com.id.icd.IcdChapters;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static com.id.aggregation.ResultFiles.readTexts;
import static com.id.aggregation.ResultFiles.write;
import static com.id.aggregation.ResultFiles.writeLines;

/**
 * aggregates the per proposal results of Calculation
 */
public class Evaluation {

  private static final Logger LOGGER = LoggerFactory.getLogger(Evaluation.class);

  private static final List<String> DETAIL_CONFIGS = List.of(Configs.MAIN, Configs.EXACT);
  private static final String VARIANT_A = "A";
  private static final String VARIANT_B = "B";

  private Evaluation() {
  }

  public static void main(String[] args) {
    Path resultsPath = Paths.get(args[0]);
    Path outputDir = Paths.get(args[1]);
    try {
      Files.createDirectories(outputDir);
      IcdChapters chapters = IcdChapters.load();
      VariantAggregate variantA = new VariantAggregate(VARIANT_A, Configs.ALL, Configs.SOFT);
      VariantAggregate variantB = new VariantAggregate(VARIANT_B, Configs.ALL, Configs.SOFT);
      CodeAggregate codes = new CodeAggregate(chapters, DETAIL_CONFIGS);
      TauCurves tauA = new TauCurves(VARIANT_A, tauKeys());
      TauCurves tauB = new TauCurves(VARIANT_B, tauKeys());
      try (BufferedWriter bWriter =
              Files.newBufferedWriter(outputDir.resolve("variant_b.csv"), StandardCharsets.UTF_8)) {
        bWriter.write(ProposalResult.HEADER);
        readTexts(resultsPath, text -> {
          text.proposals().forEach(variantA::add);
          text.proposals().forEach(tauA::add);
          Map<String, ProposalResult> best = text.best(Configs.MAIN);
          variantB.add(best);
          tauB.add(best);
          codes.add(best);
          writeLines(bWriter, best.values().stream().map(ProposalResult::toLine).toList());
        });
      }
      write(outputDir, "overall", VariantAggregate.OVERALL_HEADER,
         concat(variantA.overallLines(), variantB.overallLines()));
      write(outputDir, "category_shares", VariantAggregate.CATEGORY_HEADER,
         concat(variantA.categoryLines(), variantB.categoryLines()));
      write(outputDir, "codes", CodeAggregate.CODE_HEADER, codes.codeLines());
      write(outputDir, "chapters", CodeAggregate.CHAPTER_HEADER, codes.chapterLines());
      write(outputDir, "code_distribution", CodeAggregate.DISTRIBUTION_HEADER, codes.distributionLines());
      Path tauDir = Files.createDirectories(outputDir.resolve("tau"));
      for (TauCurves curves : List.of(tauA, tauB)) {
        for (Map.Entry<String, List<String>> entry : curves.lines().entrySet()) {
          write(tauDir, entry.getKey(), TauCurve.HEADER, entry.getValue());
        }
      }
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  /**
   * @return precision, recall and f of exact and main config, f of the other soft configs
   */
  private static List<TauCurves.Key> tauKeys() {
    List<TauCurves.Key> keys = new ArrayList<>();
    for (String config : List.of(Configs.EXACT, Configs.MAIN)) {
      Scores.NAMES.forEach(s -> keys.add(new TauCurves.Key(config, s)));
    }
    Configs.SOFT.stream().filter(config -> !config.equals(Configs.MAIN))
       .forEach(config -> keys.add(new TauCurves.Key(config, "f")));
    return keys;
  }

  private static List<String> concat(List<String> a, List<String> b) {
    return Stream.concat(a.stream(), b.stream()).toList();
  }
}
