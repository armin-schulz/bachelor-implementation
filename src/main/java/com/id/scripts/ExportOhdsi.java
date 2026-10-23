package com.id.scripts;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Consumer;

public class ExportOhdsi {

  private static final Logger LOGGER = LoggerFactory.getLogger(ExportOhdsi.class);

  public static void main(String[] args) {
    Path ohdsiReleasePath = Path.of(args[0]);
    Path outputPath = Path.of(args[1]);
    try (BufferedWriter writer = Files.newBufferedWriter(outputPath, StandardCharsets.UTF_8)) {
      Map<String, Set<String>> ohdsi = loadReferences(ohdsiReleasePath);
      writer.write("icd code\tsct ids");
      for (Map.Entry<String, Set<String>> entry : ohdsi.entrySet()) {
        writer.newLine();
        writer.write(entry.getKey() + "\t" + String.join(" ", entry.getValue()));
      }
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  private ExportOhdsi() {
  }

  public static String normalizeIcd(String code) {
    if (code == null) {
      return "";
    }
    return code.toLowerCase(Locale.ROOT)
       .replaceAll("[^\\p{L}\\p{N}-]", "");
  }

  public static Map<String, Set<String>> loadReferences(Path vocabularyDirectory) throws IOException {
    Map<String, String> ohdsiToIcdCodes = new HashMap<>();
    Map<String, String> ohdsiToSnomedCodes = new HashMap<>();
    readCsv(vocabularyDirectory.resolve("CONCEPT.csv"), Set.of("concept_id", "vocabulary_id", "concept_code"),
       row -> {
         String vocabulary = row.get("vocabulary_id");
         if ("ICD10GM".equals(vocabulary)) {
           String code = row.get("concept_code");
           if (code.isEmpty()) {
             throw new IllegalArgumentException("empty normalized icd code for sct id " + row.get("concept_id"));
           }
           ohdsiToIcdCodes.put(row.get("concept_id"), code);

         } else if ("SNOMED".equals(vocabulary)) {
           String code = row.get("concept_code");
           if (code.isBlank()) {
             throw new IllegalArgumentException("empty icd code for sct id " + row.get("concept_id"));
           }
           ohdsiToSnomedCodes.put(row.get("concept_id"), code);
         }
       }
    );

    Map<String, Set<String>> references = new TreeMap<>();
    readCsv(vocabularyDirectory.resolve("CONCEPT_RELATIONSHIP.csv"), Set.of("concept_id_1", "concept_id_2",
          "relationship_id", "invalid_reason"), row -> {
         if (!"Maps to".equals(row.get("relationship_id")) || !row.get("invalid_reason").isBlank()) {
           return;
         }
         String icd = ohdsiToIcdCodes.get(row.get("concept_id_1"));
         String snomed = ohdsiToSnomedCodes.get(row.get("concept_id_2"));
         if (icd != null && snomed != null) {
           references.computeIfAbsent(
              icd, ignored -> new TreeSet<>()
           ).add(snomed);
         }
       }
    );
    return new TreeMap<>(references);
  }

  private record Row(Map<String, Integer> columns, String[] values, long lineNumber) {
    String get(String name) {
      Integer index = columns.get(name);
      if (index == null) {
        throw new IllegalArgumentException("unknown column " + name);
      }
      return index < values.length ? values[index] : "";
    }
  }

  private static void readCsv(Path file, Set<String> requiredColumns, Consumer<Row> handler)
     throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
      String header = reader.readLine();
      String[] names = header.split("\t", -1);
      Map<String, Integer> columns = new HashMap<>();
      for (int i = 0; i < names.length; i++) {
        columns.put(names[i].strip(), i);
      }
      for (String required : requiredColumns) {
        if (!columns.containsKey(required)) {
          throw new IOException("missing column " + required + " in " + file);
        }
      }
      String line;
      long lineNumber = 1;
      while ((line = reader.readLine()) != null) {
        lineNumber++;
        if (line.isEmpty()) {
          continue;
        }
        handler.accept(new Row(columns, line.split("\t", -1), lineNumber));
      }
    }
  }
}