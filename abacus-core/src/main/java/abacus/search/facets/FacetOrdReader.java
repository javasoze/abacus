package abacus.search.facets;

import java.io.IOException;

import org.apache.lucene.index.LeafReaderContext;

public abstract class FacetOrdReader {

  public abstract FacetOrdSegmentReader getSegmentOrdReader(LeafReaderContext ctx)
      throws IOException;  
  public abstract String getIndexedFieldName();

}
