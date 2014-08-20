package abacus.config;

import java.io.IOException;
import java.util.Map;

public interface FieldConfigReader {
  Map<String, FieldConfig> readerFacetsConfig() throws IOException;
}
