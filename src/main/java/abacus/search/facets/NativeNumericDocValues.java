package abacus.search.facets;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.index.NumericDocValues;

import abacus.search.facets.unsafe.HS;

public class NativeNumericDocValues extends NumericDocValues 
    implements Closeable {

  private long bufferPtr;
  
  public NativeNumericDocValues(NumericDocValues docvals, int maxdoc) {
    bufferPtr = HS.allocArray(maxdoc, 8, false);
    for (int i =0;i< maxdoc;++i) {
      HS.setLong(bufferPtr, i, docvals.get(i));
    }
  }
  
  @Override
  public long get(int docID) {
    return HS.getLong(bufferPtr, docID);
  }

  @Override
  public void close() throws IOException {
    HS.freeArray(bufferPtr);
  }

}
