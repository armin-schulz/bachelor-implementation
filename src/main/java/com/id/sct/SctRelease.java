package com.id.sct;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.id.hierarchy.Concept;
import com.id.hierarchy.IsaRelation;
import com.id.hierarchy.SctConcept;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Stream;

public record SctRelease(LocalDate releaseDate, Map<Long, Concept> concepts, List<IsaRelation> isaRelations) {

  private static final int COL_IS_ACTIVE = 2;

  private static final int COL_CONCEPT_ID = 0;

  private static final int COL_RELATION_IS_ACTIVE = 2;
  private static final int COL_SOURCE_ID = 4;
  private static final int COL_DESTINATION_ID = 5;
  private static final int COL_RELATION_TYPE = 7;
  private static final long TYPE_ID_ISA_RELATION = 116680003;

  private static final int COL_DESCRIPTION_ID = 0;
  private static final int COL_DESCRIPTIONS_CONCEPT_ID = 4;
  private static final int COL_DESCRIPTION_TYPE = 6;
  private static final int COL_DESCRIPTION_TERM = 7;
  private static final long TYPE_ID_SYNONYM = 900000000000013009L;


  private static final int COL_REFSET = 4;
  private static final int COL_REF_COMPONENT_ID = 5;
  private static final int COL_ACCEPTABILTY_ID = 6;
  private static final long TYPE_ID_PREFERRED_DESCRIPTION = 900000000000548007L;

  private static final Pattern PATTERN_CONCEPTS =
     Pattern.compile("sct2_Concept_Snapshot_GermanyEdition_\\d{8}\\.txt");
  private static final Pattern PATTERN_DESCRIPTION =
     Pattern.compile("sct2_Description_Snapshot_GermanyEdition_\\d{8}\\.txt");
  private static final Pattern PATTERN_LANGUAGE =
     Pattern.compile("der2_cRefset_LanguageSnapshot_GermanyEdition_\\d{8}\\.txt");
  private static final Pattern PATTERN_RELATION =
     Pattern.compile("sct2_Relationship_Snapshot_GermanyEdition_\\d{8}\\.txt");
  private static final Pattern PATTERN_INFO =
     Pattern.compile("release_package_information.json");


  public static SctRelease fromReleaseDirectory(String releasePath) throws IOException {
    SctPahts files = SctPahts.fromRelease(releasePath);
    Map<Long, Concept> concepts = extractConcepts(files.concepts, files.descriptions, files.language);
    List<IsaRelation> isaRelations = getIsaRelations(files.relations, concepts);
    try (FileReader reader = new FileReader(files.infoPath.toFile())) {
      JsonObject infoRootNode = JsonParser.parseReader(reader).getAsJsonObject();
      String releaseDateString = infoRootNode.get("effectiveTime").getAsString();
      LocalDate releaseDate = LocalDate.parse(releaseDateString, DateTimeFormatter.BASIC_ISO_DATE);
      return new SctRelease(releaseDate, concepts, isaRelations);
    }
  }

  private record TermText(long termId, String text) {
  }

  private record SctPahts(Path concepts, Path descriptions, Path language, Path relations, Path infoPath) {

    private static SctPahts fromRelease(String releasePath) throws IOException {
      Path concepts = null;
      Path descriptions = null;
      Path language = null;
      Path relations = null;
      Path infoPath = null;
      try (Stream<Path> paths = Files.walk(Path.of(releasePath))) {
        for (Path p : paths.filter(Files::isRegularFile).toList()) {
          String filename = p.getFileName().toString();

          if (PATTERN_CONCEPTS.matcher(filename).matches()) {
            if (concepts != null) {
             throw new IllegalArgumentException("Duplicate concept file: " + PATTERN_CONCEPTS);
            }
            concepts = p;
          } else if (PATTERN_DESCRIPTION.matcher(filename).matches()) {
            if (descriptions != null) {
             throw new IllegalArgumentException("Duplicate descriptions file: " + PATTERN_DESCRIPTION);
            }
            descriptions = p;
          } else if (PATTERN_LANGUAGE.matcher(filename).matches()) {
            if (language != null) {
              throw new IllegalArgumentException("Duplicate language file: " + PATTERN_LANGUAGE);
            }
            language = p;
          } else if (PATTERN_RELATION.matcher(filename).matches()) {
            if (relations != null) {
              throw new IllegalArgumentException("Duplicate relation file: " + PATTERN_RELATION);
            }
            relations = p;
          } else if (PATTERN_INFO.matcher(filename).matches()) {
            if (infoPath != null) {
              throw new IllegalArgumentException("Duplicate info file: " + PATTERN_INFO);
            }
            infoPath = p;
          }
        }
      }
      if (concepts == null) {
        throw new IOException("missing concepts file in " + releasePath);
      }
      if (descriptions == null) {
        throw new IOException("missing descriptions file in " + releasePath);
      }
      if (language == null) {
        throw new IOException("missing language file in " + releasePath);
      }
      if (relations == null) {
        throw new IOException("missing relations in " + releasePath);
      }
      if (infoPath == null) {
        throw new IOException("missing info in " + releasePath);
      }
      return new SctPahts(concepts, descriptions, language, relations, infoPath);
    }
  }


  public Set<Concept> getConcepts() {
    return new HashSet<>(concepts.values());
  }

  private static Map<Long, Concept> extractConcepts(Path conceptsPath, Path descriptionPath, Path languagePath)
     throws IOException {
    Set<Long> conceptIds = getActiveConcepts(conceptsPath);
    Map<Long, Set<TermText>> conceptsDescriptions = getConceptsDescriptions(descriptionPath, conceptIds);
    return getWithPreferredTerms(languagePath, conceptsDescriptions);
  }

  private static Map<Long, Set<TermText>> getConceptsDescriptions(Path descriptionsPath, Set<Long> conceptIds)
     throws IOException {
    Map<Long, Set<TermText>> conceptToDescriptionIds = new HashMap<>();
    try (BufferedReader reader = Files.newBufferedReader(descriptionsPath)) {
      boolean isFirst = true;
      String line;
      while ((line = reader.readLine()) != null) {
        if (isFirst) {
          isFirst = false;
          continue;
        }
        String[] cs = line.split("\t", -1);
        if (isActive(cs[COL_IS_ACTIVE]) && TYPE_ID_SYNONYM == Long.parseLong(cs[COL_DESCRIPTION_TYPE])) {
          final long descriptionId = Long.parseLong(cs[COL_DESCRIPTION_ID]);
          final long conceptId = Long.parseLong(cs[COL_DESCRIPTIONS_CONCEPT_ID]);
          if (conceptIds.contains(conceptId)) {
            final String text = cs[COL_DESCRIPTION_TERM];
            conceptToDescriptionIds.computeIfAbsent(conceptId, k -> new HashSet<>()).add(new TermText(descriptionId, text));
          }
        }
      }
    }
    return conceptToDescriptionIds;
  }

  private static List<IsaRelation> getIsaRelations(Path relationsPath, Map<Long, Concept> concepts) throws IOException {
    List<IsaRelation> isaRelations = new ArrayList<>();
    try (BufferedReader br = Files.newBufferedReader(relationsPath)) {
      boolean isFirst = true;
      String line;
      while ((line = br.readLine()) != null) {
        if (isFirst) {
          isFirst = false;
          continue;
        }
        String[] cs = line.split("\t", -1);
        if (isActive(cs[COL_RELATION_IS_ACTIVE]) && isIsaRelation(cs[COL_RELATION_TYPE])) {
          final long sourceConceptId = Long.parseLong(cs[COL_SOURCE_ID]);
          final long targetConceptId = Long.parseLong(cs[COL_DESTINATION_ID]);
          isaRelations.add(new IsaRelation(
             Objects.requireNonNull(concepts.get(sourceConceptId), "missing concept id=" + sourceConceptId),
             Objects.requireNonNull(concepts.get(targetConceptId), "missing concept id=" + targetConceptId)));
        }
      }
    }
    return isaRelations;
  }

  private static boolean isIsaRelation(String type) {
    return TYPE_ID_ISA_RELATION == Long.parseLong(type);
  }

  private static Map<Long, Concept> getWithPreferredTerms(Path languagePath, Map<Long, Set<TermText>> conceptTexts) throws IOException {
    Map<Long, Set<LanguageRefset>> preferredDescriptionIds = new HashMap<>();
    try (BufferedReader reader = Files.newBufferedReader(languagePath)) {
      boolean isFirst = true;
      String line;
      while ((line = reader.readLine()) != null) {
        if (isFirst) {
          isFirst = false;
          continue;
        }
        String[] cs = line.split("\t", -1);
        if (isActive(cs[COL_IS_ACTIVE]) && TYPE_ID_PREFERRED_DESCRIPTION == Long.parseLong(cs[COL_ACCEPTABILTY_ID])) {
          final long componentId = Long.parseLong(cs[COL_REF_COMPONENT_ID]);
          final LanguageRefset language = LanguageRefset.fromId(cs[COL_REFSET]);
          if (language != null) {
            preferredDescriptionIds.computeIfAbsent(componentId, k -> EnumSet.noneOf(LanguageRefset.class)).add(language);
          }
        }
      }
    }
    Map<Long, Concept> concepts = new HashMap<>();
    for (Map.Entry<Long, Set<TermText>> entry : conceptTexts.entrySet()) {
      List<Term> preferredTerms = entry.getValue().stream()
         .flatMap(t -> preferredDescriptionIds.getOrDefault(t.termId(), EnumSet.noneOf(LanguageRefset.class)).stream()
            .map(l -> new Term(t.termId(), t.text(), l)))
         .toList();
      concepts.put(entry.getKey(), new SctConcept(entry.getKey(), selectTerm(entry.getKey(), preferredTerms)));
    }
    return concepts;
  }

  private static Term selectTerm(Long conceptId, List<Term> preferredTerms) {
    Map<LanguageRefset, List<Term>> languagedTerms = new EnumMap<>(LanguageRefset.class);
    for (Term term : preferredTerms) {
      languagedTerms.computeIfAbsent(term.language(), k -> new ArrayList<>()).add(term);
    }
    List<Term> terms = languagedTerms.get(LanguageRefset.GERMAN);
    terms = terms == null ? languagedTerms.get(LanguageRefset.ENGLISH_GB) : terms;
    terms = terms == null ? languagedTerms.get(LanguageRefset.ENGLISH_US) : terms;
    if (terms == null) {
      throw new IllegalArgumentException("No preferred term found for concept=" + conceptId);
    }
    return terms.get(0);
  }

  private static Set<Long> getActiveConcepts(Path conceptsPath) throws IOException {
    Set<Long> ids = new HashSet<>();
    try (BufferedReader reader = Files.newBufferedReader(conceptsPath)) {
      boolean isFirst = true;
      String line;
      while ((line = reader.readLine()) != null) {
        if (isFirst) {
          isFirst = false;
          continue;
        }
        String[] cs = line.split("\t", -1);
        if (isActive(cs[COL_IS_ACTIVE])) {
          final long id = Long.parseLong(cs[COL_CONCEPT_ID]);
          ids.add(id);
        }
      }
    }
    return ids;
  }

  private static boolean isActive(String s) {
    return switch (s) {
      case "0" -> false;
      case "1" -> true;
      default -> throw new IllegalStateException("Unexpected value for number for boolean: " + s);
    };
  }
}
