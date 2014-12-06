package abacus.search.facets.docvalues;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntListIterator;

import java.nio.ByteBuffer;

import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;

public class DirectBufferSortedSetDocValues extends SortedSetDocValues {

  private final ByteBuffer ords;
  private final ByteBuffer buffer;  
  private final ByteBuffer byteRefs;
  private final ByteBuffer ordsPool;
  private final int numValues;
  
  private int currentVal = SortedSetDocValuesUtil.UNASSIGNED;
  private int offset = 0;
  private boolean hasMoreOrds = false;
  
  public DirectBufferSortedSetDocValues(SortedSetDocValues inner, int maxDoc) {
    long valCount = inner.getValueCount();
    if (valCount > Integer.MAX_VALUE) {
      throw new IllegalStateException("too many values: " + valCount);      
    }
    
    numValues = (int) valCount;
    
    BytesRef[] byteRefArr = new BytesRef[numValues];
    
    int numBytes = 0;
    for (int i = 0; i < numValues ; ++i) {
      BytesRef tempRef = inner.lookupOrd(i);
      numBytes += tempRef.length;
      byteRefArr[i] = tempRef;      
    }
    this.byteRefs = ByteBuffer.allocateDirect(numValues * 8);
    buffer = ByteBuffer.allocateDirect(numBytes);    
    int byteCount = 0;
    for (int i = 0;i < numValues ;++i) {      
      buffer.put(byteRefArr[i].bytes, byteRefArr[i].offset, byteRefArr[i].length);      
      this.byteRefs.putInt(byteCount);
      this.byteRefs.putInt(byteRefArr[i].length);
      byteCount += byteRefArr[i].length;
    }
    
    ords = ByteBuffer.allocateDirect(maxDoc * 4);
    for (int i = 0; i < maxDoc; ++i) {
      ords.putInt(SortedSetDocValuesUtil.UNASSIGNED);
    }
    
    IntArrayList ordIndexList = new IntArrayList();
    
    for (int i = 0; i < maxDoc; ++ i) {
      inner.setDocument(i);
      int tmpOrd;
      while ((tmpOrd = (int) inner.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
        int existingOrd = ords.getInt(i * 4);
        if (existingOrd == SortedSetDocValuesUtil.UNASSIGNED) {                    
          ords.putInt(i * 4, tmpOrd);
        } else {
          if (!SortedSetDocValuesUtil.isSetHighestBit(existingOrd)) {
            int pointer = ordIndexList.size();
            ords.putInt(i * 4, SortedSetDocValuesUtil.setHighestBit(pointer));
            ordIndexList.add(SortedSetDocValuesUtil.setHighestBit(existingOrd));
          } else {
            int lastIdx = ordIndexList.size() - 1;
            ordIndexList.set(lastIdx, SortedSetDocValuesUtil.setHighestBit(ordIndexList.getInt(lastIdx)));
          }
          ordIndexList.add(tmpOrd);
        }
      }
    }
    
    int numOrds = ordIndexList.size();
    ordsPool = ByteBuffer.allocateDirect(numOrds * 4);
    
    IntListIterator intIter = ordIndexList.iterator();
    while (intIter.hasNext()) {
      ordsPool.putInt(intIter.nextInt());
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
      int val = ordsPool.getInt(offset * 4);
      offset++;
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
    currentVal = ords.getInt(docID * 4);
    offset = 0;
    hasMoreOrds = true;
  }

  @Override
  public BytesRef lookupOrd(long ord) {
    BytesRef result = new BytesRef();
    int offset = byteRefs.getInt(8 * (int) ord );
    int length = byteRefs.getInt(8 * (int) ord + 4);
    
    result.bytes = new byte[length];
    result.offset = 0;
    result.length = length;
    for (int i = 0; i < result.length; ++i) {
      result.bytes[i] = buffer.get(offset + i);
    }
    return result;
  }

  @Override
  public long getValueCount() {
    return numValues;
  }

}
