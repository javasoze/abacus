package abacus.search.facets.docvalues;

import java.nio.ByteBuffer;

import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;

public class DirectBufferSortedDocValues extends SortedDocValues {

  private final ByteBuffer ords;
  private final ByteBuffer buffer;  
  private final ByteBuffer byteRefs;
  private final int numTerms;
  
  public DirectBufferSortedDocValues(SortedDocValues inner, int maxDoc) {    
    this.ords = ByteBuffer.allocateDirect(maxDoc * 4);
    for (int i =0;i< maxDoc; ++i) {      
      this.ords.putInt(inner.getOrd(i));
    }
    
    numTerms = inner.getValueCount();
    
    BytesRef[] byteRefArr = new BytesRef[numTerms];
    
    int numBytes = 0;
    for (int i = 0; i < numTerms ; ++i) {
      BytesRef tempRef = BytesRef.deepCopyOf(inner.lookupOrd(i));
      numBytes += tempRef.length;
      byteRefArr[i] = tempRef;      
    }
    this.byteRefs = ByteBuffer.allocateDirect(numTerms * 8);
    buffer = ByteBuffer.allocateDirect(numBytes);    
    int byteCount = 0;
    for (int i = 0;i < numTerms ;++i) {      
      buffer.put(byteRefArr[i].bytes, byteRefArr[i].offset, byteRefArr[i].length);      
      this.byteRefs.putInt(byteCount);
      this.byteRefs.putInt(byteRefArr[i].length);
      byteCount += byteRefArr[i].length;
    }
  }
  
  @Override
  public int getOrd(int docID) {
    return this.ords.getInt(docID * 4);
  }

  @Override
  public BytesRef lookupOrd(int ord) {
    BytesRef result = new BytesRef();
    int offset = byteRefs.getInt(8 * ord );
    int length = byteRefs.getInt(8 * ord + 4);
    
    result.bytes = new byte[length];
    result.offset = 0;
    result.length = length;
    for (int i = 0; i < result.length; ++i) {
      result.bytes[i] = buffer.get(offset + i);
    }
    return result;
  }

  @Override
  public int getValueCount() {
    return numTerms;
  }

}
