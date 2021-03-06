package abacus.search.facets;

import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.FixedBitSet;

public abstract class PerSegmentFacetCount {

  private final int counts[];
  private final FixedBitSet bits;

  public PerSegmentFacetCount(int[] counts) {
    this.counts = counts;
    this.bits = new FixedBitSet(counts.length);
  }

  public abstract BytesRef lookupLabel(int ord);

  public abstract int getOrd(BytesRef label);

  public void accumulate(int ord) {
    counts[ord]++;
    bits.set(ord);
  }

  public int getCountForLabel(String label) {
    int ord = getOrd(new BytesRef(label));
    if (ord < 0) {
      return 0;
    } else {
      return counts[ord];
    }
  }

  public FacetEntryIterator getFacetEntryIterator() {

    final int minHit = 1;

    return new FacetEntryIterator() {
      int doc = -1;

      @Override
      public boolean next(ValCountPair val) {
        doc++;
        if (doc == bits.length()) return false;
        while ((doc = bits.nextSetBit(doc)) != DocIdSetIterator.NO_MORE_DOCS) {
          int count = counts[doc];
          if (count >= minHit) {
            val.count = count;
            val.val = doc;
            return true;
          }
        }
        return false;
      }

      @Override
      public BytesRef lookupLabel(long val) {
        int ord = (int) val;
        return PerSegmentFacetCount.this.lookupLabel(ord);
      }
    };
  }
}
