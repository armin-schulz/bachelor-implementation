package com.id.custom;

import java.util.List;

public record RefMappingLine(String ident, boolean nonSeg, String code, List<List<Long>> proposals, List<Long> reference) {

  @Override
  public boolean equals(Object o) {
    if (o instanceof RefMappingLine m) {
      return ident.equals(m.ident());
    }
    return false;
  }

  @Override
  public int hashCode() {
    return ident.hashCode();
  }
}
