package abacus.search.facets.docvalues;

import org.apache.lucene.index.SortedDocValues;
import org.apache.lucene.util.BytesRef;

public class ArraySortedDocValues extends SortedDocValues {
  private final int[] ords;
  private final byte[] buffer;
  private final BytesRef[] byteRefs;
  
  public ArraySortedDocValues(SortedDocValues inner, int maxDoc) {
    // ordinal array
    this.ords = new int[maxDoc];    
    for (int i =0;i< maxDoc; ++i) {
      this.ords[i] = inner.getOrd(i);
    }
    
    // bytesRef array
    byteRefs = new BytesRef[inner.getValueCount()];
    
    int numBytes = 0;
    for (int i=0;i<byteRefs.length;++i) {
      BytesRef tempRef = inner.lookupOrd(i);
      numBytes+=tempRef.length;
      byteRefs[i] = tempRef;
    }
    
    // loading in the bytes
    buffer = new byte[numBytes];
    int byteCount = 0;
    for (int i=0;i<byteRefs.length;++i) {
      System.arraycopy(byteRefs[i].bytes, byteRefs[i].offset, buffer, byteCount, byteRefs[i].length);
      byteRefs[i].bytes = buffer;
      byteRefs[i].offset=byteCount;
      byteCount += byteRefs[i].length;
    }
  }
  
  @Override
  public int getOrd(int docID) {
    return this.ords[docID];
  }

  @Override
  public BytesRef lookupOrd(int ord) {
    BytesRef result = new BytesRef();
    result.bytes = buffer;
    result.offset = byteRefs[ord].offset;
    result.length = byteRefs[ord].length;
    return result;
  }

  @Override
  public int getValueCount() {
    return byteRefs.length;
  }

}
