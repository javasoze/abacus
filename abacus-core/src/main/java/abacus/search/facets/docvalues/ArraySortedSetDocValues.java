package abacus.search.facets.docvalues;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntList;

import java.util.Arrays;

import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;

public class ArraySortedSetDocValues extends SortedSetDocValues {
  
  private final int[] ords;
  private final BytesRef[] byteRefs;
  private final byte[] buffer;
  private final IntList ordIndexList = new IntArrayList();  
  private final int numValues;
  
  private int currentVal = SortedSetDocValuesUtil.UNASSIGNED;
  private int offset = 0;
  private boolean hasMoreOrds = false;  
  
  public ArraySortedSetDocValues(SortedSetDocValues inner, int maxDoc) {
    
    long valCount = inner.getValueCount();
    if (valCount > Integer.MAX_VALUE) {
      throw new IllegalStateException("too many values: " + valCount);      
    }
    
    numValues = (int) valCount;
 // bytesRef array
    byteRefs = new BytesRef[(int)inner.getValueCount()];
    
    int numBytes = 0;
    for (int i=0;i<byteRefs.length;++i) {
      BytesRef tempRef = BytesRef.deepCopyOf(inner.lookupOrd(i));
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
    
    ords = new int[maxDoc];
    Arrays.fill(ords, SortedSetDocValuesUtil.UNASSIGNED);
    
    for (int i = 0; i < maxDoc; ++ i) {
      inner.setDocument(i);
      int tmpOrd;
      while ((tmpOrd = (int) inner.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
        int existingOrd = ords[i];
        if (existingOrd == SortedSetDocValuesUtil.UNASSIGNED) {
          ords[i] = tmpOrd;
        } else {
          if (!SortedSetDocValuesUtil.isSetHighestBit(existingOrd)) {
            int pointer = ordIndexList.size();
            ords[i] = SortedSetDocValuesUtil.setHighestBit(pointer);
            ordIndexList.add(SortedSetDocValuesUtil.setHighestBit(existingOrd));
          } else {
            int lastIdx = ordIndexList.size() - 1;
            ordIndexList.set(lastIdx, SortedSetDocValuesUtil.setHighestBit(ordIndexList.getInt(lastIdx)));
          }
          ordIndexList.add(tmpOrd);
        }
      }
    }
  }
  
  @Override
  public long nextOrd() {
    if (currentVal == SortedSetDocValuesUtil.UNASSIGNED || !hasMoreOrds) {
      return SortedSetDocValues.NO_MORE_ORDS;
    }
    if (SortedSetDocValuesUtil.isSetHighestBit(currentVal)) {
      if (offset == 0) {
        offset = SortedSetDocValuesUtil.decodePointer(currentVal);
      }
      int val = ordIndexList.getInt(offset++);
      if (SortedSetDocValuesUtil.isSetHighestBit(val)) {
        val = SortedSetDocValuesUtil.decodePointer(val);
      } else {
        hasMoreOrds = false;
      }
      return val;
    } else {
      hasMoreOrds = false;
      return currentVal;
    }
  }

  @Override
  public void setDocument(int docID) {
    currentVal = ords[docID];
    offset = 0;
    hasMoreOrds = true;
  }

  @Override
  public BytesRef lookupOrd(long ord) {
    BytesRef result = new BytesRef();
    result.bytes = buffer;
    result.offset = byteRefs[(int)ord].offset;
    result.length = byteRefs[(int)ord].length;
    return result;
  }

  @Override
  public long getValueCount() {
    return numValues;
  }

}
