package abacus.search.facets;

import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.OpenBitSet;

public abstract class PerSegmentFacetCount {

  private final int counts[];
  private final OpenBitSet bits;
  
  
  public PerSegmentFacetCount(int numVals) {    
    this.counts = new int[numVals];
    this.bits = new OpenBitSet(numVals);
  }
  
  public abstract void lookupLabel(int ord, BytesRef result);
  
  public void accumulate(int ord) {
    counts[ord] ++;
    bits.set(ord);
  }
  
  public FacetEntryIterator getFacetEntryIterator(final int minHit) {
    
    return new FacetEntryIterator() {
      int doc = -1;
      @Override
      public boolean next(FacetValue val) {        
        while ((doc = bits.nextSetBit(doc + 1)) != -1) {          
          int count = counts[doc];
          if (count >= minHit) {
            BytesRef label = val.getLabel();
            if (label == null) {
              label = new BytesRef();
            }
            lookupLabel(doc, label);
            val.setValues(label, count);
            return true;
          }
        }
        return false;
      }
    };
  }
}
