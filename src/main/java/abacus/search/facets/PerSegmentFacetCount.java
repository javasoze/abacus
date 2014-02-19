package abacus.search.facets;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;

public abstract class PerSegmentFacetCount {

  private final int counts[];
  private final OpenBitSet bits;
  
  
  public PerSegmentFacetCount(int[] counts) {    
    this.counts = counts;
    this.bits = new OpenBitSet(counts.length);
  }
  
  public abstract void lookupLabel(int ord, BytesRef result);
  
  public abstract int getOrd(BytesRef label);
  
  public void accumulate(int ord) {
    counts[ord] ++;
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
        while ((doc = bits.nextSetBit(doc + 1)) != -1) {          
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
      public void lookupLabel(long val, BytesRef label) {
        int ord = (int) val;
        lookupLabel(ord, label);        
      }
    };
  }
}
