package abacus.search.facets;

import it.unimi.dsi.fastutil.longs.Long2IntMap;
import it.unimi.dsi.fastutil.longs.Long2IntMap.Entry;
import it.unimi.dsi.fastutil.longs.Long2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.util.BytesRef;

public class NumericFacetAccumulator extends FacetAccumulator {

  private NumericDocValues docValues = null;
  private Long2IntMap countMap = new Long2IntOpenHashMap();
  public NumericFacetAccumulator(String field) {
    super(field);
    countMap.defaultReturnValue(0);
  }
  
  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {    
    docValues = context.reader().getNumericDocValues(getField());
    if (docValues == null) {
      throw new IOException("field is not defined: " + getField());
    }
  }
  
  @Override
  public void collect(int doc) throws IOException {    
    long valOrd = docValues.get(doc);    
    int count = countMap.get(valOrd) + 1;
    countMap.put(valOrd, count);    
  }
  
  public BytesRef getFacetLabel(long labelOrd) {
    return new BytesRef(String.valueOf(labelOrd));
  }

  @Override
  public FacetEntryIterator getFacetEntryIterator(final int minHit) {
    final ObjectIterator<Entry> entryIter = countMap.long2IntEntrySet().iterator();
    
    return new FacetEntryIterator() {

      @Override
      public boolean next(FacetValue val) {
        while (entryIter.hasNext()) {
          Entry entry = entryIter.next();
          int count = entry.getIntValue();
          if (count < minHit) continue;
          
          val.setValues(new BytesRef(String.valueOf(entry.getLongKey())), 
              entry.getIntValue());
          
          return true;
        } 
        return false;
      }
      
    };
  }
}
