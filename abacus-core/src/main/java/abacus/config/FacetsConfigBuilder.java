package abacus.config;

import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.document.FieldType.NumericType;

public class FacetsConfigBuilder {  
  private FacetIndexedType facetType = null;
  private NumericType numericType = null;
  private String[] rangeStrings = null;
  
  public FacetsConfigBuilder withNumericType(NumericType numericType) {
	  this.numericType = numericType;	  
	  return this;
  }
  
  public FacetsConfigBuilder withFacetIndexedType(FacetIndexedType facetType) {
    this.facetType = facetType;
    return this;
  }
  
  public FacetsConfigBuilder withFacetIndexedRangeStrings(String... rangeStrings) {
    this.rangeStrings = rangeStrings;
    return this;
  }

  public FacetsConfig build() {
    return new FacetsConfig(facetType, numericType, rangeStrings);
  }
}
