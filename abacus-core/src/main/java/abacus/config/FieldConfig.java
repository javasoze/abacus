package abacus.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import abacus.api.AbacusFieldType;
import org.apache.lucene.document.FieldType.NumericType;

public class FieldConfig {
  private final AbacusFieldType fieldType;
  private final FacetIndexedType facetType;
  private final String[] rangeStrings;

  FieldConfig(AbacusFieldType fieldType, FacetIndexedType facetType, String[] rangeStrings) {
    this.fieldType = fieldType;
    this.facetType = facetType;
    this.rangeStrings = rangeStrings;
  }

  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("fieldType:" + fieldType);
    buf.append("\tfacetType:" + facetType);
    buf.append("\tranges :").append(Arrays.toString(rangeStrings));
    return buf.toString();
  }

  public AbacusFieldType getFieldType() {
    return fieldType;
  }

  public FacetIndexedType getFacetType() {
    return facetType;
  }

  public String[] getRangeStrings() {
    return rangeStrings;
  }

  private static final String PREFIX = "__$abacus.";
  private static final String PARAM_FIELD_TYPE = "field_type";
  private static final String PARAM_FACET_TYPE = "facet_type";
  private static final String PARAM_RANGE_STRING = "range_string";

  public static Map<String, String> flatten(Map<String, FieldConfig> configMap) {
    Map<String, String> flattenMap = new HashMap<>();
    for (Entry<String, FieldConfig> entry : configMap.entrySet()) {
      String name = entry.getKey();
      FieldConfig config = entry.getValue();
      flattenMap.put(PREFIX + name + "." + PARAM_FIELD_TYPE, String.valueOf(config.getFieldType()));
      flattenMap.put(PREFIX + name + "." + PARAM_FACET_TYPE, String.valueOf(config.getFacetType()));
      if (config.getRangeStrings() != null && config.getRangeStrings().length > 0) {
        StringBuilder concatRanges = new StringBuilder();
        for (int i = 0; i < config.getRangeStrings().length; ++i) {
          if (i > 0) {
            concatRanges.append(",");
          }
          concatRanges.append(config.getRangeStrings()[i]);
        }
        flattenMap.put(PREFIX + name + "." + PARAM_RANGE_STRING, concatRanges.toString());
      }
    }
    return flattenMap;
  }

  public static Map<String, FieldConfig> deFlatten(Map<String, String> flattenMap) {
    Map<String, FieldConfig> configMap = new HashMap<>();
    Map<String, FieldConfigBuilder> builderMap = new HashMap<>();
    for (Entry<String, String> entry : flattenMap.entrySet()) {
      String key = entry.getKey();
      if (key.startsWith(PREFIX)) {
        String configString = key.substring(PREFIX.length());
        String[] pair = configString.split("\\.");
        if (pair.length != 2) {
          throw new IllegalStateException("invalid key: " + key);
        }
        String name = pair[0];
        String configType = pair[1];
        FieldConfigBuilder builder;
        if (builderMap.containsKey(name)) {
          builder = builderMap.get(name);
        } else {
          builder = new FieldConfigBuilder();
          builderMap.put(name, builder);
        }
        String val = entry.getValue();
        if (PARAM_FIELD_TYPE.endsWith(configType)) {
          builder.withFiledType(AbacusFieldType.valueOf(val));
        } else if (PARAM_FACET_TYPE.equals(configType)) {
          builder.withFacetIndexedType(FacetIndexedType.valueOf(val));
        } else if (PARAM_RANGE_STRING.endsWith(configType)) {
          String[] parts = val.split(",");
          builder.withFacetIndexedRangeStrings(parts);
        } else {
          throw new IllegalStateException("invalid config type: " + configType);
        }
      }
    }
    for (Entry<String, FieldConfigBuilder> entry : builderMap.entrySet()) {
      configMap.put(entry.getKey(), entry.getValue().build());
    }
    return configMap;
  }
}
