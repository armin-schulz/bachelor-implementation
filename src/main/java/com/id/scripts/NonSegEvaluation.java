package com.id.scripts;

import com.id.aggregation.Configs;
import com.id.aggregation.Description;
import com.id.aggregation.TextResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.id.aggregation.ResultFiles.readTexts;
import static com.id.aggregation.ResultFiles.write;

/**
 * compares f of the main config between texts without and with non segmentable parts, one value
 * per text: variant A the mean over all proposals, variant B the selected proposal
 */
public class NonSegEvaluation {

  private static final Logger LOGGER = LoggerFactory.getLogger(NonSegEvaluation.class);

  private static final String HEADER = "variant\tnon seg\t" + Description.HEADER;

  private NonSegEvaluation() {
  }

  public static void main(String[] args) {
    Path resultsPath = Paths.get(args[0]);
    Path outputDir = Paths.get(args[1]);
    try {
      Files.createDirectories(outputDir);
      Values a = new Values();
      Values b = new Values();
      readTexts(resultsPath, text -> {
        a.add(text.nonSeg(), meanF(text));
        b.add(text.nonSeg(), text.best(Configs.MAIN).get(Configs.MAIN).scores().f());
      });
      List<String> lines = new ArrayList<>();
      lines.addAll(a.lines("A"));
      lines.addAll(b.lines("B"));
      write(outputDir, "non_seg", HEADER, lines);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  private static double meanF(TextResult text) {
    return text.proposals().stream().map(proposal -> proposal.get(Configs.MAIN))
       .mapToDouble(result -> result.scores().f()).average().orElseThrow();
  }

  /**
   * values per group, false: without, true: with non segmentable parts
   */
  private static final class Values {

    private final Map<Boolean, List<Double>> groups = Map.of(false, new ArrayList<>(), true, new ArrayList<>());

    void add(boolean nonSeg, double value) {
      groups.get(nonSeg).add(value);
    }

    /**
     * @throws IllegalStateException if a group is empty
     */
    List<String> lines(String variant) {
      List<String> lines = new ArrayList<>();
      for (boolean nonSeg : List.of(false, true)) {
        double[] values = groups.get(nonSeg).stream().mapToDouble(Double::doubleValue).toArray();
        if (values.length == 0) {
          throw new IllegalStateException("no texts with non seg=" + nonSeg + " in variant " + variant);
        }
        lines.add(variant + "\t" + nonSeg + "\t" + Description.of(values).toFields());
      }
      return lines;
    }
  }
}
