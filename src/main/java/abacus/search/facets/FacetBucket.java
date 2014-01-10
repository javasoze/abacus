package abacus.search.facets;

import org.apache.lucene.util.BytesRef;

public abstract class FacetBucket extends FacetValue {
  
  public FacetBucket() {
    super();
  }

  public FacetBucket(BytesRef label, int count) {
    super(label, count);
  }

  public abstract void accumulate(long val);
}
