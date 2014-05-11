package abacus.config;

import java.io.IOException;
import java.util.Map;

public interface FacetsConfigReader {
  Map<String, FacetsConfig> readerFacetsConfig() throws IOException;
}
