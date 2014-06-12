package abacus.config;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.lucene.document.FieldType.NumericType;

public class FacetsConfig {
  private final FacetIndexedType facetType;
  private final NumericType numericType;
  private final String[] rangeStrings;
  
  FacetsConfig(FacetIndexedType facetType, NumericType numericType, String[] rangeStrings) {
    this.facetType = facetType;
    this.rangeStrings = rangeStrings;
    if (facetType == FacetIndexedType.NUMERIC) {
      if (numericType == null) {
    	throw new IllegalArgumentException("numeric type not specified");
      }
      this.numericType = numericType;
    } else {
      this.numericType = null;
    }
  }
  
  @Override
  public String toString() {
    StringBuilder buf = new StringBuilder();
    buf.append("facettype: " + facetType);
    if (numericType != null) {
      buf.append("\tnumericType: " + numericType);
    }
    buf.append("\tranges :").append(Arrays.toString(rangeStrings));
    return buf.toString();
  }
  
  public String[] getRangeStrings() {
    return rangeStrings;
  }
  
  public NumericType getNumericType() {
	  return numericType;
  }

  public FacetIndexedType getFacetType() {
    return facetType;
  }
  
  private static final String PREFIX = "__$abacus.";
  private static final String PARAM_FACET_TYPE = "facet_type";
  private static final String PARAM_NUMERIC_TYPE = "numeric_type";
  private static final String PARAM_RANGE_STRING = "range_string";
  
  public static Map<String, String> flatten(Map<String, FacetsConfig> configMap) {
    Map<String, String> flattenMap = new HashMap<String, String>();
    for (Entry<String, FacetsConfig> entry : configMap.entrySet()) {
      String name = entry.getKey();
      FacetsConfig config = entry.getValue();
      flattenMap.put(PREFIX + name + "." + PARAM_FACET_TYPE, String.valueOf(config.getFacetType()));
      if (config.getNumericType() != null) {
        flattenMap.put(PREFIX + name + "." + PARAM_NUMERIC_TYPE, String.valueOf(config.getNumericType()));
      }
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
  
  public static Map<String, FacetsConfig> deFlatten(Map<String, String> flattenMap) {
    Map<String, FacetsConfig> configMap = new HashMap<String, FacetsConfig>();
    Map<String, FacetsConfigBuilder> builderMap = new HashMap<String, FacetsConfigBuilder>();
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
        FacetsConfigBuilder builder;
        if (builderMap.containsKey(name)) {
          builder = builderMap.get(name);
        } else {
          builder = new FacetsConfigBuilder();
          builderMap.put(name, builder);
        }
        String val = entry.getValue();
        if (PARAM_FACET_TYPE.equals(configType)) {
          builder.withFacetIndexedType(FacetIndexedType.valueOf(val));
        } else if (PARAM_NUMERIC_TYPE.endsWith(configType)) {
          builder.withNumericType(NumericType.valueOf(val));
        } else if (PARAM_RANGE_STRING.endsWith(configType)) {
          String[] parts = val.split(",");
          builder.withFacetIndexedRangeStrings(parts);
        } else {
          throw new IllegalStateException("invalid config type: " + configType);
        }
      }
    }
    for (Entry<String, FacetsConfigBuilder> entry : builderMap.entrySet()) {
      configMap.put(entry.getKey(), entry.getValue().build());
    }
    return configMap;
  }  
}
