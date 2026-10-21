package com.id.scripts;

import com.id.hierarchy.Concept;
import com.id.sct.SctRelease;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.format.DateTimeFormatter;

public class ExportSCTIds {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportSCTIds.class);

  private ExportSCTIds() {}

  public static void main(String[] args) {
    String sctReleasePath = args[0];
    String outputPath = args[1];
    try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(outputPath))) {
      SctRelease sctRelease = SctRelease.fromReleaseDirectory(sctReleasePath);
      writer.write("active sct ids for " + sctRelease.releaseDate().format(DateTimeFormatter.ofPattern("dd-MM-yyyy")));
      for (Concept concept : sctRelease.getConcepts()) {
        writer.newLine();
        writer.write(String.valueOf(concept.id()));
      }
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }

  }
}
