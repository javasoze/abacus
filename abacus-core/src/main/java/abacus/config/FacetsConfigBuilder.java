package abacus.config;

import abacus.search.facets.FastDocValuesAtomicReader.MemType;

public class FacetsConfigBuilder {
  
  private MemType memType;
  private FacetType facetType;
  
  public FacetsConfigBuilder withMemType(MemType memType) {
    this.memType = memType;
    return this;
  }
  
  public FacetsConfigBuilder withFacetType(FacetType facetType) {
    this.facetType = facetType;
    return this;
  }

  public FacetsConfig build() {
    return new FacetsConfig(memType, facetType);
  }
}
