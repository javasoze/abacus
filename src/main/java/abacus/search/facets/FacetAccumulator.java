package abacus.search.facets;

import java.io.IOException;
import java.util.Comparator;

import org.apache.lucene.search.Collector;
import org.apache.lucene.search.Scorer;
import org.apache.lucene.util.PriorityQueue;

public abstract class FacetAccumulator extends Collector {
  
  private final String field;
  
  public FacetAccumulator(String field) {
    this.field = field;
  }
  
  public final String getField() {
    return field;
  }
  
  public final FacetValue[] getTopFacets(int max) {
    return getTopFacets(max, 1, FacetValue.COUNT_COMPARATOR);
  }
  
  public final FacetValue[] getTopFacets(int max, int minHit) {
    return getTopFacets(max, minHit, FacetValue.COUNT_COMPARATOR);
  }
  
  public final FacetValue[] getTopFacets(int max, 
      int minHit,
      final Comparator<FacetValue> comparator) {
    
    PriorityQueue<FacetValue> pq = new PriorityQueue<FacetValue>(max, false) {

      @Override
      protected boolean lessThan(FacetValue a, FacetValue b) {
        return comparator.compare(a, b) > 0;
      }
      
    };
    
    final FacetEntryIterator iter = getFacetEntryIterator(minHit);
    
    FacetValue value = new FacetValue();
    
    while (iter.next(value)) {      
      value = pq.insertWithOverflow(value);
      if (value == null) {
        value = new FacetValue();
      }
    }
    
    int numVals = pq.size();
    FacetValue[] retArr = new FacetValue[numVals];
    for (int i = 0; i < numVals; ++i) {      
      retArr[numVals - i - 1] = pq.pop();
    }
    
    return retArr;
  }
  
  public abstract FacetEntryIterator getFacetEntryIterator(int minHit);
 
  @Override
  public void setScorer(Scorer scorer) throws IOException {
    // ignore
  }
  
  @Override
  public boolean acceptsDocsOutOfOrder() {
    return true;
  }

}