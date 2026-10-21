package com.id.sct;

public enum LanguageRefset {
  GERMAN(31000274107L, "de"),
  ENGLISH_GB(900000000000508004L, "uk"),
  ENGLISH_US(900000000000509007L, "us");

  private final long id;
  private final String abbreviation;

  LanguageRefset(long id, String abbreviation) {
    this.id = id;
    this.abbreviation = abbreviation;
  }

  public String abrv() {
    return abbreviation;
  }

  public static LanguageRefset fromId(String idString) {
    long id = Long.parseLong(idString);
    for (LanguageRefset c : LanguageRefset.values()) {
      if (c.id == id) {
        return c;
      }
    }
    return null;
  }
}
