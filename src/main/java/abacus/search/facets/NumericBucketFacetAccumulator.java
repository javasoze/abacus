package abacus.search.facets;

import java.io.IOException;

import org.apache.lucene.index.AtomicReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.util.BytesRef;

public class NumericBucketFacetAccumulator extends FacetAccumulator {

  private NumericDocValues docValues = null;
  private FacetBucket[] buckets;
  
  public NumericBucketFacetAccumulator(String field, FacetBucket[] buckets) {
    super(field);
    this.buckets = buckets;
  }

  @Override
  public FacetEntryIterator getFacetEntryIterator(final int minHit) {
    return new FacetEntryIterator() {
      
      int ord = -1;
      @Override
      public boolean next(FacetValue val) {
        while (true) {
          ord++;
          if (ord >= buckets.length) break;
          if (buckets[ord].getCount() >= minHit) {
            BytesRef label = val.getLabel();
            if (label == null) {
              label = new BytesRef();
            }
            val.setValues(buckets[ord].getLabel(), buckets[ord].getCount());
            return true;
          }
        }
        return false;
      }
    };
  }

  @Override
  public void collect(int doc) throws IOException {
    long val = docValues.get(doc);
    for (FacetBucket bucket : buckets) {
      bucket.accumulate(val);
    }
  }

  @Override
  public void setNextReader(AtomicReaderContext context) throws IOException {
    docValues = context.reader().getNumericDocValues(getField());
    if (docValues == null) {
      throw new IOException("docvalue: " + getField() + " not found.");
    }
  }

}
