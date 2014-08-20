package abacus.config;

import abacus.api.AbacusFieldType;

public class FieldConfigBuilder {
  private AbacusFieldType fieldType = null;
  private FacetIndexedType facetType = null;
  private String[] rangeStrings = null;

  public FieldConfigBuilder withFiledType(AbacusFieldType fieldType) {
    this.fieldType = fieldType;
    return this;
  }

  public FieldConfigBuilder withFacetIndexedType(FacetIndexedType facetType) {
    this.facetType = facetType;
    return this;
  }

  public FieldConfigBuilder withFacetIndexedRangeStrings(String... rangeStrings) {
    this.rangeStrings = rangeStrings;
    return this;
  }

  public FieldConfig build() {
    return new FieldConfig(fieldType, facetType, rangeStrings);
  }
}
