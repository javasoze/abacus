package abacus.config;

import abacus.search.facets.FastDocValuesAtomicReader.MemType;

public class FacetsConfig {
  private final MemType memType;
  private final FacetType facetType;
  
  FacetsConfig(MemType memType, FacetType facetType) {
    this.memType = memType;
    this.facetType = facetType;
  }

  public MemType getMemType() {
    return memType;
  }

  public FacetType getFacetType() {
    return facetType;
  }  
  
}
