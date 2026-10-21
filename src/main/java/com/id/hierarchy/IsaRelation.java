package com.id.hierarchy;

public record IsaRelation(Concept childConcept, Concept parentConcept) {

  @Override
  public String toString() {
    return childConcept.id() + " " + childConcept.term().term() + " isA " +
       parentConcept.id() + " " + parentConcept.term().term();
  }
}
