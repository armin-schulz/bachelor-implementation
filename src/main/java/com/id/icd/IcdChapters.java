package com.id.icd;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Pattern;

/**
 * icd-10-gm chapters as ranges of three character categories, read from resource icd10gm_chapters.csv
 */
public final class IcdChapters {

  private static final String RESOURCE = "/icd10gm_chapters.csv";
  private static final Pattern CATEGORY = Pattern.compile("[a-z][0-9]{2}");
  private static final Pattern NORMALIZED_CODE = Pattern.compile("[a-z][0-9]{2}[0-9a-z-]*");

  /**
   * @param first first three character category of chapter, normalized
   * @param last  last three character category of chapter, normalized
   */
  public record Chapter(String name, String first, String last, String title) {
  }

  private final List<Chapter> chapters;

  private IcdChapters(List<Chapter> chapters) {
    this.chapters = List.copyOf(chapters);
  }

  /**
   * @throws IllegalStateException on malformed or overlapping ranges
   */
  public static IcdChapters load() throws IOException {
    InputStream stream = IcdChapters.class.getResourceAsStream(RESOURCE);
    if (stream == null) {
      throw new IOException("missing resource " + RESOURCE);
    }
    List<Chapter> chapters = new ArrayList<>();
    try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
      reader.readLine();
      String line;
      while ((line = reader.readLine()) != null) {
        String[] cs = line.split("\t", -1);
        if (cs.length != 4) {
          throw new IllegalStateException("malformed chapter line: " + line);
        }
        Chapter chapter = new Chapter(cs[0], cs[1], cs[2], cs[3]);
        if (!CATEGORY.matcher(chapter.first()).matches() || !CATEGORY.matcher(chapter.last()).matches()
           || chapter.first().compareTo(chapter.last()) > 0) {
          throw new IllegalStateException("malformed range of chapter " + chapter.name());
        }
        chapters.add(chapter);
      }
    }
    List<Chapter> sorted = new ArrayList<>(chapters);
    sorted.sort(Comparator.comparing(Chapter::first));
    for (int i = 1; i < sorted.size(); i++) {
      if (sorted.get(i).first().compareTo(sorted.get(i - 1).last()) <= 0) {
        throw new IllegalStateException("overlapping chapters " + sorted.get(i - 1).name()
           + " and " + sorted.get(i).name());
      }
    }
    return new IcdChapters(chapters);
  }

  /**
   * @return chapters in order of the resource file
   */
  public List<Chapter> chapters() {
    return chapters;
  }

  /**
   * @param code normalized like ExportOhdsi.normalizeIcd, e.g. e785
   * @return name of the chapter containing code, e.g. IV
   * @throws IllegalArgumentException if code is not normalized or belongs to no chapter
   */
  public String chapter(String code) {
    if (code == null || !NORMALIZED_CODE.matcher(code).matches()) {
      throw new IllegalArgumentException("not a normalized icd-10-gm code: " + code);
    }
    String category = code.substring(0, 3);
    for (Chapter chapter : chapters) {
      if (chapter.first().compareTo(category) <= 0 && category.compareTo(chapter.last()) <= 0) {
        return chapter.name();
      }
    }
    throw new IllegalArgumentException("no chapter for icd-10-gm code: " + code);
  }
}
