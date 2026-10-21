package com.id.aggregation;

import com.id.custom.ProposalResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;

/**
 * reading the per proposal results of Calculation and writing aggregated tab separated files
 */
public final class ResultFiles {

  private static final Logger LOGGER = LoggerFactory.getLogger(ResultFiles.class);

  private ResultFiles() {
  }

  /**
   * passes the results of each text to consumer, rows of a text have to be consecutive as written by Calculation
   *
   * @throws IllegalStateException if the header is unexpected, the file is empty, the rows of a text are not
   *                               consecutive or a text is inconsistent, see TextResult.of
   */
  public static void readTexts(Path path, Consumer<TextResult> consumer) throws IOException {
    Set<String> finished = new HashSet<>();
    List<ProposalResult> rows = new ArrayList<>();
    try (BufferedReader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
      if (!ProposalResult.HEADER.equals(reader.readLine())) {
        throw new IllegalStateException("unexpected header in " + path);
      }
      String line;
      while ((line = reader.readLine()) != null) {
        ProposalResult row = ProposalResult.fromLine(line);
        if (!rows.isEmpty() && !rows.get(0).ident().equals(row.ident())) {
          finishText(rows, finished, consumer);
        }
        rows.add(row);
      }
    }
    if (rows.isEmpty()) {
      throw new IllegalStateException("no results in " + path);
    }
    finishText(rows, finished, consumer);
    LOGGER.info("Read {} texts", finished.size());
  }

  private static void finishText(List<ProposalResult> rows, Set<String> finished, Consumer<TextResult> consumer) {
    TextResult text = TextResult.of(rows, Configs.ALL);
    if (!finished.add(text.ident())) {
      throw new IllegalStateException("rows of ident=" + text.ident() + " are not consecutive");
    }
    consumer.accept(text);
    rows.clear();
  }

  /**
   * writes header and lines to dir/name.csv
   */
  public static void write(Path dir, String name, String header, List<String> lines) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(dir.resolve(name + ".csv"), StandardCharsets.UTF_8)) {
      writer.write(header);
      writeLines(writer, lines);
    }
    LOGGER.info("Written {}", name);
  }

  /**
   * writes each line after a line separator
   */
  public static void writeLines(BufferedWriter writer, List<String> lines) {
    try {
      for (String line : lines) {
        writer.newLine();
        writer.write(line);
      }
    } catch (IOException e) {
      throw new UncheckedIOException(e);
    }
  }
}
