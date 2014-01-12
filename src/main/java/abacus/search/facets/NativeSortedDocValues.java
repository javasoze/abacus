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
  //private final BytesRef[] byteRefs;
  private final long bytesRefPtr;
  private final int numTerms;
  public NativeSortedDocValues(SortedDocValues inner, int maxDoc) {    
    this.ordsPtr = HS.allocArray(maxDoc, 4, false);
    for (int i =0;i< maxDoc; ++i) {     
      HS.setInt(this.ordsPtr, i, inner.getOrd(i));
    }
    
    
    numTerms = inner.getValueCount();
    BytesRef[] byteRefs = new BytesRef[numTerms];
    bytesRefPtr= HS.allocArray(numTerms, 8, false);
    
    int numBytes = 0;
    for (int i=0; i < numTerms; ++i) {
      BytesRef tempRef = new BytesRef();
      inner.lookupOrd(i, tempRef);
      numBytes+=tempRef.length;
      byteRefs[i] = tempRef;
      HS.setInt(bytesRefPtr, i*2, tempRef.offset);
      HS.setInt(bytesRefPtr, i*2 + 1, tempRef.length);
    }
    
    bufferPtr = HS.allocArray(numBytes, 1, false);
    for (int i=0;i < numTerms; ++i) {
      int offset = byteRefs[i].offset;
      int length = byteRefs[i].length;
      for (int k = 0; k < length; ++k) {
        HS.unsafe.putByte(bufferPtr + k, byteRefs[i].bytes[offset + k]);
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
    int offset = HS.getInt(bytesRefPtr, 2 * ord);
    int length = HS.getInt(bytesRefPtr, 2 * ord + 1);
    
    result.bytes = new byte[length];
    result.offset = 0;
    result.length = length;
    for (int i = 0; i < result.length; ++i) {
      result.bytes[i] = HS.unsafe.getByte(bufferPtr + offset + i);  
    }    
  }

  @Override
  public int getValueCount() {
    return numTerms;
  }

  @Override
  public void close() throws IOException {
    HS.freeArray(bufferPtr);
    HS.freeArray(ordsPtr);
    HS.freeArray(bytesRefPtr);
  }  
}
