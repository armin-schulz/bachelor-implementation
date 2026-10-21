package com.id.custom;

import java.util.List;

public record MappingLine(String ident, boolean nonSeg, String code, List<List<Long>> proposals) {

  @Override
  public boolean equals(Object o) {
    if (o instanceof MappingLine m) {
      return ident.equals(m.ident());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ident.hashCode();
  }
}
