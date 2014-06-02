package abacus.config;

import org.apache.lucene.document.FieldType.NumericType;

public class FacetsConfigBuilder {  
  private FacetType facetType = null;
  private NumericType numericType = null;
  
  public FacetsConfigBuilder withNumericType(NumericType numericType) {
	this.numericType = numericType;	  
	return this;
  }
  
  public FacetsConfigBuilder withFacetType(FacetType facetType) {
    this.facetType = facetType;
    return this;
  }

  public FacetsConfig build() {
    return new FacetsConfig(facetType, numericType);
  }
}
