package abacus.search.facets;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;

public abstract class FacetOrdReader {

  public abstract FacetOrdSegmentReader getSegmentOrdReader(String indexedField, AtomicReaderContext ctx) throws IOException;  
  public abstract String getIndexedFieldName();

}
