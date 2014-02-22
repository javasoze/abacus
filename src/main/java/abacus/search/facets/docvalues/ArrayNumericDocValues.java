package abacus.search.facets.docvalues;

import org.apache.lucene.index.NumericDocValues;

public class ArrayNumericDocValues extends NumericDocValues {

  private long[] vals;
  
  public ArrayNumericDocValues(NumericDocValues docvals, int maxdoc) {
    vals = new long[maxdoc];
    for (int i =0;i<maxdoc;++i) {
      vals[i] = docvals.get(i);
    }
  }
  
  @Override
  public long get(int docID) {
    return vals[docID];
  }

}
