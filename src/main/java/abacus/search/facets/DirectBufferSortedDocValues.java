package abacus.search.facets;

import java.nio.ByteBuffer;

import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;

public class DirectBufferSortedDocValues extends SortedDocValues {

  private final ByteBuffer ords;
  private final ByteBuffer buffer;  
  private final BytesRef[] byteRefs;
  public DirectBufferSortedDocValues(SortedDocValues inner, int maxDoc) {    
    this.ords = ByteBuffer.allocateDirect(maxDoc * 4);
    for (int i =0;i< maxDoc; ++i) {      
      this.ords.putInt(inner.getOrd(i));
    }
    byteRefs = new BytesRef[inner.getValueCount()];
    
    int numBytes = 0;
    for (int i=0;i<byteRefs.length;++i) {
      BytesRef tempRef = new BytesRef();
      inner.lookupOrd(i, tempRef);
      numBytes+=tempRef.length;
      byteRefs[i] = tempRef;
    }
    buffer = ByteBuffer.allocateDirect(numBytes);
    for (int i=0;i<byteRefs.length;++i) {      
      buffer.put(byteRefs[i].bytes, byteRefs[i].offset, byteRefs[i].length);
      byteRefs[i].bytes = null;
    }
  }
  
  @Override
  public int getOrd(int docID) {
    return this.ords.getInt(docID * 4);
  }

  @Override
  public void lookupOrd(int ord, BytesRef result) {
    result.bytes = new byte[byteRefs[ord].length];
    result.offset = 0;
    result.length = byteRefs[ord].length;
    for (int i = 0; i < result.length; ++i) {
      result.bytes[i] = buffer.get(byteRefs[ord].offset + i);
    }
  }

  @Override
  public int getValueCount() {
    return byteRefs.length;
  }

}
