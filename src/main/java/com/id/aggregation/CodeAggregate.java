package com.id.aggregation;

import com.id.custom.ProposalResult;
import com.id.custom.Scores;
import com.id.icd.IcdChapters;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * mean scores per code and per chapter, chapter values are means of code values
 */
public final class CodeAggregate {

  public static final String CODE_HEADER = "config\tcode\tchapter\ttexts\tprecision\trecall\tf";
  public static final String CHAPTER_HEADER = "config\tchapter\tcodes\ttexts\tprecision\trecall\tf";
  public static final String DISTRIBUTION_HEADER = "config\tscore\t" + Description.HEADER;

  private final IcdChapters chapters;
  private final Map<String, TreeMap<String, ScoreMean>> codeMeans = new LinkedHashMap<>();

  /**
   * @param configs configs to aggregate in output order
   */
  public CodeAggregate(IcdChapters chapters, List<String> configs) {
    this.chapters = chapters;
    configs.forEach(config -> codeMeans.put(config, new TreeMap<>()));
  }

  /**
   * adds the results of the selected proposal of one text, configs not aggregated are ignored
   *
   * @throws IllegalArgumentException if the code has no chapter
   */
  public void add(Map<String, ProposalResult> proposal) {
    for (ProposalResult result : proposal.values()) {
      TreeMap<String, ScoreMean> means = codeMeans.get(result.config());
      if (means != null) {
        chapters.chapter(result.code());
        means.computeIfAbsent(result.code(), code -> new ScoreMean()).add(result.scores());
      }
    }
  }

  public List<String> codeLines() {
    List<String> lines = new ArrayList<>();
    codeMeans.forEach((config, means) -> means.forEach((code, mean) ->
       lines.add(config + "\t" + code + "\t" + chapters.chapter(code) + "\t" + mean.count()
          + "\t" + toFields(mean.mean()))));
    return lines;
  }

  /**
   * @return per config and chapter in order of the chapter file the number of codes and texts and the means of the
   * code values, chapters without codes are omitted
   */
  public List<String> chapterLines() {
    List<String> lines = new ArrayList<>();
    codeMeans.forEach((config, means) -> {
      for (IcdChapters.Chapter chapter : chapters.chapters()) {
        ScoreMean chapterMean = new ScoreMean();
        int texts = 0;
        for (Map.Entry<String, ScoreMean> entry : means.entrySet()) {
          if (chapters.chapter(entry.getKey()).equals(chapter.name())) {
            chapterMean.add(entry.getValue().mean());
            texts += entry.getValue().count();
          }
        }
        if (chapterMean.count() > 0) {
          lines.add(config + "\t" + chapter.name() + "\t" + chapterMean.count() + "\t" + texts
             + "\t" + toFields(chapterMean.mean()));
        }
      }
    });
    return lines;
  }

  /**
   * @return per config and score the distribution of the code values
   */
  public List<String> distributionLines() {
    List<String> lines = new ArrayList<>();
    codeMeans.forEach((config, means) -> Scores.NAMES.forEach(score -> {
      double[] values = means.values().stream().mapToDouble(mean -> mean.mean().get(score)).toArray();
      lines.add(config + "\t" + score + "\t" + Description.of(values).toFields());
    }));
    return lines;
  }

  private static String toFields(Scores scores) {
    return scores.precision() + "\t" + scores.recall() + "\t" + scores.f();
  }
}
