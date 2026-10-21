package com.id.hierarchy;

import com.id.sct.Term;

public record SctConcept (long id, Term term) implements Concept {

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o instanceof SctConcept c) {
      return id == c.id;
    }
    return false;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(id);
  }

  @Override
  public String toString() {
    return id + ": " + term;
  }
}
