package com.id.scripts;

import com.id.custom.MappingLine;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;

public class Exclusions {

  private static final Logger LOGGER = LoggerFactory.getLogger(Exclusions.class);

  public static void main(String[] args) {
    Path validSctIdsPath = Paths.get(args[0]);
    Path referencePath = Paths.get(args[1]);
    Path mappingPath = Paths.get(args[2]);
    Path filteredPath = Paths.get(args[3]);
    Path statsPath = Paths.get(args[4]);
    Path unmatchedPath = Paths.get(args[5]);

    try {
      Set<Long> validSctIds = new HashSet<>();
      applyToLines(validSctIdsPath, cs -> validSctIds.add(Long.parseLong(cs[0])), StandardCharsets.UTF_8);
      Map<String, Set<Long>> reference = new HashMap<>();
      applyToLines(referencePath,
          cs -> reference.computeIfAbsent(cs[0], k -> new HashSet<>()).addAll(parseIds(cs[1])),
          StandardCharsets.UTF_8);
      List<MappingLine> mappings = new ArrayList<>();
      applyToLines(mappingPath, cs -> {
        final String ident = cs[0];
        final boolean nonSeg = Boolean.parseBoolean(cs[1]);
        final String code = cs[2];
        List<List<Long>> proposals = new ArrayList<>();
        for (String p : cs[3].split("\\|", -1)) {
          List<Long> proposal = parseIds(p);
          if (!proposal.isEmpty()) {
            proposals.add(proposal);
          }
        }
        mappings.add(new MappingLine(ident, nonSeg, code, proposals));
      }, Charset.forName("windows-1252"));
      Stats stats = new Stats();
      Map<MappingLine, Set<Long>> filteredMappings = filterMappings(mappings, reference, validSctIds, stats);
      writeFiltered(filteredMappings, filteredPath);
      writeFilterStats(stats, mappings.size(), filteredMappings.size(), statsPath);
      writeUnmatchedCodes(stats.unmatchedCodes, unmatchedPath);
    } catch (IOException e) {
      LOGGER.error(e.getMessage(), e);
    }
  }

  private static void writeFiltered(Map<MappingLine, Set<Long>> filteredMappings, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write("ident\tnon seg\tcode\treference\tproposals");
      for (Map.Entry<MappingLine, Set<Long>> entry : filteredMappings.entrySet()) {
        writer.newLine();
        MappingLine mapping = entry.getKey();
        String referenceString = StringUtils.join(entry.getValue(), " ");
        String proposalsString = proposalsToString(mapping.proposals());
        writer.write(mapping.ident() + "\t" + mapping.nonSeg() + "\t" + mapping.code()
            + "\t" + referenceString + "\t" + proposalsString);
      }
    }
  }

  private static void writeFilterStats(Stats stats, int nbrMappings, int nbrFilteredMappings, Path path) throws IOException {
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write("stat\tremoved\tremaining");
      writer.newLine();
      int nbrRemainingMappings = nbrMappings;
      writer.write("number of mappings\t0\t" + nbrMappings);
      writer.newLine();
      nbrRemainingMappings -= stats.noIcdCode;
      writer.write("texts without icd code\t" + stats.noIcdCode + "\t" + nbrRemainingMappings);
      writer.newLine();
      nbrRemainingMappings -= stats.noProposals;
      writer.write("texts without mapping\t" + stats.noProposals + "\t" + nbrRemainingMappings);
      writer.newLine();
      nbrRemainingMappings -= stats.noReference;
      writer.write("texts without reference\t" + stats.noReference + "\t" + nbrRemainingMappings);
      writer.newLine();
      nbrRemainingMappings -= stats.invalidReference;
      writer.write("texts with invalid reference concept\t" + stats.invalidReference + "\t" + nbrRemainingMappings);
      writer.newLine();
      nbrRemainingMappings -= stats.rmMappingForRmProposal;
      writer.write("text without mapping (after removing invalid reference concepts)\t" + stats.rmMappingForRmProposal + "\t" + nbrRemainingMappings);
      writer.newLine();
      writer.write("number of filtered mappings\t0\t" + nbrRemainingMappings);
      writer.newLine();
      if (nbrRemainingMappings != nbrFilteredMappings) {
        LOGGER.error("Calculated remaining mapping={} != filtered mapping={}", nbrRemainingMappings, nbrFilteredMappings);
      }
    }
  }

  private static void writeUnmatchedCodes(List<String> unmatched, Path path) throws IOException {
    unmatched.sort(String::compareTo);
    List<String> uniqueUnmatched = unmatched.stream()
        .distinct()
        .sorted()
        .toList();
    try (BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8)) {
      writer.write("unmatched\tunique unmatched");
      writer.newLine();
      for (int i = 0; i < unmatched.size(); i++) {
        String line = unmatched.get(i) + "\t";
        if (i < uniqueUnmatched.size()) {
          line += uniqueUnmatched.get(i);
        }
        writer.write(line);
        writer.newLine();
      }
    }
  }

  private static Map<MappingLine, Set<Long>> filterMappings(List<MappingLine> mappings, Map<String, Set<Long>> references,
                                                            Set<Long> validSctIds, Stats stats) {
    Map<MappingLine, Set<Long>> filteredMappings = new HashMap<>(mappings.size());
    for (MappingLine m : mappings) {
      if (StringUtils.isEmpty(m.code())) {
        stats.noIcdCode++;
        continue;
      }
      if (m.proposals().isEmpty()) {
        stats.noProposals++;
        continue;
      }
      Set<Long> reference = references.get(m.code());
      if (reference == null) {
        stats.unmatchedCodes.add(m.code());
        stats.noReference++;
        continue;
      }
      if (reference.stream().anyMatch(i -> !validSctIds.contains(i))) {
        stats.invalidReference++;
        continue;
      }
      List<List<Long>> validProposals = new ArrayList<>(m.proposals().size());
      for (List<Long> proposal : m.proposals()) {
        if (validSctIds.containsAll(proposal)) {
          validProposals.add(proposal);
        } else {
          stats.rmProposals++;
        }
      }
      if (validProposals.isEmpty()) {
        stats.rmMappingForRmProposal++;
      } else {
        filteredMappings.put(new MappingLine(m.ident(), m.nonSeg(), m.code(), validProposals), reference);
      }
    }
    return filteredMappings;
  }

  /**
   * skips first line
   */
  private static void applyToLines(Path sctIdsPath, Consumer<String[]> lineConsumer, Charset encoding) throws IOException {
    try (BufferedReader reader = Files.newBufferedReader(sctIdsPath, encoding)) {
      boolean firstLine = true;
      String line;
      while ((line = reader.readLine()) != null) {
        if (firstLine) {
          firstLine = false;
          continue;
        }
        lineConsumer.accept(line.split("\t", -1));
      }
    }
  }

  public static List<Long> parseIds(String s) {
    List<Long> ids = new ArrayList<>();
    if (StringUtils.isBlank(s)) {
      return ids;
    }
    for (String id : s.strip().split("\\s+")) {
      ids.add(Long.parseLong(id));
    }
    return ids;
  }

  private static String proposalsToString(List<List<Long>> proposals) {
    List<String> strings = new ArrayList<>(proposals.size());
    for (List<Long> p : proposals) {
      strings.add(StringUtils.join(p, " "));
    }
    return StringUtils.join(strings, "|");
  }

  private static class Stats {

    private int noIcdCode = 0;
    private int noProposals = 0;
    private int noReference = 0;
    private int invalidReference = 0;
    private int rmProposals = 0;
    private int rmMappingForRmProposal = 0;
    private List<String> unmatchedCodes = new ArrayList<>();

  }
}
