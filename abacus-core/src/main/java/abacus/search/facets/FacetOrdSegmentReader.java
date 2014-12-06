package abacus.search.facets;

import org.apache.lucene.util.BytesRef;

public abstract class FacetOrdSegmentReader {

  public abstract int getValueCount();
  public abstract BytesRef lookupLabel(int ord);
  public abstract void setDocument(int docId);
  public abstract long nextOrd();
  public abstract int lookupOrd(BytesRef label);

}
