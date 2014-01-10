package abacus.search.facets;

import java.nio.ByteBuffer;

import org.apache.lucene.index.NumericDocValues;

public class DirectBufferNumericDocValues extends NumericDocValues {

  private ByteBuffer buffer;
  
  public DirectBufferNumericDocValues(NumericDocValues docvals, int maxdoc) {    
    buffer = ByteBuffer.allocateDirect(maxdoc * 8);
    for (int i =0;i< maxdoc;++i) {      
      buffer.putLong(docvals.get(i));
    }
  }
  
  @Override
  public long get(int docID) {
    return buffer.getLong(docID * 8);
  }

}
