package abacus.config;

import org.apache.lucene.document.FieldType.NumericType;

import abacus.search.facets.FastDocValuesAtomicReader.MemType;

public class FacetsConfig {
  private final MemType memType;
  private final FacetType facetType;
  private final NumericType numericType;
  
  FacetsConfig(MemType memType, FacetType facetType, NumericType numericType) {
    this.memType = memType;
    this.facetType = facetType;
    if (facetType == FacetType.NUMERIC) {
      if (numericType == null) {
    	throw new IllegalArgumentException("numeric type not specified");
      }
      this.numericType = numericType;
    } else {
      this.numericType = null;
    }
  }
  
  public NumericType getNumericType() {
	return numericType;
  }

  public MemType getMemType() {
    return memType;
  }

  public FacetType getFacetType() {
    return facetType;
  }  
  
}
