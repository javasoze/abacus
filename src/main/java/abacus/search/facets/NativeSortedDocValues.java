package abacus.search.facets;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;

import abacus.search.facets.unsafe.HS;

public class NativeSortedDocValues extends SortedDocValues 
    implements Closeable {

  private final long ordsPtr;
  private final long bufferPtr;  
  private final BytesRef[] byteRefs;
  public NativeSortedDocValues(SortedDocValues inner, int maxDoc) {    
    this.ordsPtr = HS.allocArray(maxDoc, 4, false);
    for (int i =0;i< maxDoc; ++i) {     
      HS.setInt(this.ordsPtr, i, inner.getOrd(i));
    }
    
    byteRefs = new BytesRef[inner.getValueCount()];
    
    int numBytes = 0;
    for (int i=0;i<byteRefs.length;++i) {
      BytesRef tempRef = new BytesRef();
      inner.lookupOrd(i, tempRef);
      numBytes+=tempRef.length;
      byteRefs[i] = tempRef;
    }
    
    bufferPtr = HS.allocArray(numBytes, 1, false);
    for (int i=0;i<byteRefs.length;++i) {
      for (int k = 0; k < byteRefs[i].length; ++k) {
        HS.unsafe.putByte(bufferPtr + k, byteRefs[i].bytes[byteRefs[i].offset + k]);
      }      
      byteRefs[i].bytes = null;
    }
  }
  
  @Override
  public int getOrd(int docID) {
    return HS.getInt(this.ordsPtr, docID);
  }

  @Override
  public void lookupOrd(int ord, BytesRef result) {
    result.bytes = new byte[byteRefs[ord].length];
    result.offset = 0;
    result.length = byteRefs[ord].length;
    for (int i = 0; i < result.length; ++i) {
      result.bytes[i] = HS.unsafe.getByte(bufferPtr + byteRefs[ord].offset + i);  
    }    
  }

  @Override
  public int getValueCount() {
    return byteRefs.length;
  }

  @Override
  public void close() throws IOException {
    HS.freeArray(bufferPtr);
    HS.freeArray(ordsPtr);
  }
  
  

}
