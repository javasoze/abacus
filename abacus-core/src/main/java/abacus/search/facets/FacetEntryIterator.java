package abacus.search.facets;

import org.apache.lucene.util.BytesRef;

public interface FacetEntryIterator {
  boolean next(ValCountPair val);

  public static FacetEntryIterator EMPTY = new FacetEntryIterator() {
    @Override
    public boolean next(ValCountPair val) {
      return false;
    }

    @Override
    public BytesRef lookupLabel(long val) {
      return null;
    }

  };

  BytesRef lookupLabel(long val);
}
