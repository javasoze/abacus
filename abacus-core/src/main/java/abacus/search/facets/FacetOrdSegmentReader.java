package abacus.search.facets;

import org.apache.lucene.util.BytesRef;

public abstract class FacetOrdSegmentReader {

  public abstract int getValueCount();
  public abstract void lookupLabel(int ord, BytesRef label);
  public abstract void setDocument(int docid);
  public abstract long nextOrd();
  public abstract int lookupOrd(BytesRef label);

}
