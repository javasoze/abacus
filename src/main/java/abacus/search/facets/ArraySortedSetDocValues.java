package abacus.search.facets;

import it.unimi.dsi.fastutil.ints.IntArrayList;

import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.IntsRef;

public class ArraySortedSetDocValues extends SortedSetDocValues {
  private final IntsRef[] ord;
  private IntsRef currentOrdRef = null;
  private int currentOrd = -1;

  private final byte[] buffer;
  private final BytesRef[] byteRefs;
  
  public ArraySortedSetDocValues(SortedSetDocValues inner, int maxDoc) {
    ord = new IntsRef[maxDoc];
    
    IntArrayList intArr = new IntArrayList();
    
    int count = 0;
    for (int i =0;i< maxDoc; ++i) {
      inner.setDocument(i);
      ord[i] = new IntsRef();
      ord[i].offset = count;
      
      long currentOrd;
      while((currentOrd = inner.nextOrd()) != NO_MORE_ORDS) {
        intArr.add((int) currentOrd);
        count++;
      }
      ord[i].length = count - ord[i].offset;
    }
    
    int[] intBuff = intArr.elements();
    for (IntsRef ordinal : ord) {
      ordinal.ints = intBuff;
    }
    
    byteRefs = new BytesRef[(int) inner.getValueCount()];
    
    int numBytes = 0;
    for (int i=0;i<byteRefs.length;++i) {
      BytesRef tempRef = new BytesRef();
      inner.lookupOrd(i, tempRef);
      numBytes+=tempRef.length;
      byteRefs[i] = tempRef;
    }
    buffer = new byte[numBytes];
    for (int i=0;i<byteRefs.length;++i) {
      System.arraycopy(byteRefs[i].bytes, byteRefs[i].offset, buffer, byteRefs[i].offset, byteRefs[i].length);
      byteRefs[i].bytes = buffer;
    }
  }
  
  @Override
  public long nextOrd() {
    currentOrd++;
    if (currentOrd < currentOrdRef.length) {
      return currentOrdRef.ints[currentOrd];
    } else {
      return NO_MORE_ORDS;
    }
  }

  @Override
  public void setDocument(int docID) {
    currentOrdRef = ord[docID];
  }

  @Override
  public void lookupOrd(long ord, BytesRef result) {
    result.bytes = buffer;
    result.offset = byteRefs[(int) ord].offset;
    result.length = byteRefs[(int) ord].length;
  }

  @Override
  public long getValueCount() {
    return byteRefs.length;
  }

}
