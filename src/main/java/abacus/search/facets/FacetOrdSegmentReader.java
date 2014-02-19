package abacus.search.facets;

import it.unimi.dsi.fastutil.ints.IntIterator;

import org.apache.lucene.util.BytesRef;

public abstract class FacetOrdSegmentReader {

  public abstract int getValueCount();
  public abstract void lookupLabel(int ord, BytesRef label);
  public abstract IntIterator getOrds(int docid);
  public abstract int lookupOrd(BytesRef label);

}
