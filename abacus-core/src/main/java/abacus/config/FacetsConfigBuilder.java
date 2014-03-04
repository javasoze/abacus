package abacus.config;

import org.apache.lucene.document.FieldType.NumericType;

import abacus.search.facets.FastDocValuesAtomicReader.MemType;

public class FacetsConfigBuilder {
  
  private MemType memType = null;
  private FacetType facetType = null;
  private NumericType numericType = null;
  
  public FacetsConfigBuilder withNumericType(NumericType numericType) {
	this.numericType = numericType;	  
	return this;
  }
  
  public FacetsConfigBuilder withMemType(MemType memType) {
    this.memType = memType;
    return this;
  }
  
  public FacetsConfigBuilder withFacetType(FacetType facetType) {
    this.facetType = facetType;
    return this;
  }

  public FacetsConfig build() {
    return new FacetsConfig(memType, facetType, numericType);
  }
}
