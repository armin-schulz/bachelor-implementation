package com.id.sct;

import java.util.Objects;

public record Term(long id, String term, LanguageRefset language) {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof Term t) {
      return id == t.id;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(id);
  }

  @Override
  public String toString() {
    return term + "[" + language.abrv() + "]";
  }
}
