package abacus.config;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

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
  
  private static final String PREFIX = "__$abacus.";
  private static final String PARAM_MEM_TYPE = "mem_type";
  private static final String PARAM_FACET_TYPE = "facet_type";
  private static final String PARAM_NUMERIC_TYPE = "numeric_type";
  
  public static Map<String, String> flatten(Map<String, FacetsConfig> configMap) {
    Map<String, String> flattenMap = new HashMap<String, String>();
    for (Entry<String, FacetsConfig> entry : configMap.entrySet()) {
      String name = entry.getKey();
      FacetsConfig config = entry.getValue();
      flattenMap.put(PREFIX + name + "." + PARAM_MEM_TYPE, config.getMemType().toString());
      flattenMap.put(PREFIX + name + "." + PARAM_FACET_TYPE, config.getFacetType().toString());
      flattenMap.put(PREFIX + name + "." + PARAM_NUMERIC_TYPE, config.getNumericType().toString());
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
        String[] pair = configString.split(".");
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
        if (PARAM_MEM_TYPE.equals(configType)) {
          builder.withMemType(MemType.valueOf(val));
        } else if (PARAM_FACET_TYPE.equals(configType)) {
          builder.withFacetType(FacetType.valueOf(val));
        } else if (PARAM_NUMERIC_TYPE.endsWith(configType)) {
          builder.withNumericType(NumericType.valueOf(val));
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