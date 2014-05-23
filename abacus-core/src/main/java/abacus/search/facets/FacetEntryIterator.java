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
    public void lookupLabel(long val, BytesRef label) {
    }

  };

  void lookupLabel(long val, BytesRef label);
}
