package abacus.search.facets.docvalues;

import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.ints.IntListIterator;

import java.io.Closeable;
import java.io.IOException;

import org.apache.lucene.index.SortedSetDocValues;
import org.apache.lucene.util.BytesRef;

import abacus.search.facets.unsafe.HS;

public class NativeSortedSetDocValues extends SortedSetDocValues implements Closeable {

  private final long ordsPtr;
  private final long bufferPtr;
  private final long bytesRefPtr;
  private final long ordsPoolPtr;
  private final int numTerms;
  
  private int currentVal = SortedSetDocValuesUtil.UNASSIGNED;
  private int offset = 0;
  private boolean hasMoreOrds = false;
  
  public NativeSortedSetDocValues(SortedSetDocValues inner, int maxDoc) {    
    long valCount = inner.getValueCount();
    if (valCount > Integer.MAX_VALUE) {
      throw new IllegalStateException("too many values: " + valCount);      
    }
    
    numTerms = (int) valCount;
    
    // temp buffer to hold the bytesrefs
    BytesRef[] byteRefs = new BytesRef[numTerms];
   
    int numBytes = 0;
    for (int i=0; i < numTerms; ++i) {
      BytesRef tempRef = inner.lookupOrd(i);
      numBytes+=tempRef.length;
      byteRefs[i] = tempRef;
    }
    
    bytesRefPtr= HS.allocArray(numTerms, 8, false);
    bufferPtr = HS.allocArray(numBytes, 1, false);
    
    int byteCount = 0;
    for (int i=0;i < numTerms; ++i) {
      int offset = byteRefs[i].offset;
      int length = byteRefs[i].length;
      for (int k = 0; k < length; ++k) {
        HS.unsafe.putByte(bufferPtr + byteCount + k, byteRefs[i].bytes[offset + k]);        
      }      
      byteRefs[i].bytes = null;
      HS.setInt(bytesRefPtr, i*2, byteCount);
      HS.setInt(bytesRefPtr, i*2 + 1, length);
      byteCount += length;
    }
    
    ordsPtr = HS.allocArray(maxDoc, 4, false);
    for (int i = 0; i < maxDoc; ++i) {
      HS.unsafe.putInt(ordsPtr + (i * 4), SortedSetDocValuesUtil.UNASSIGNED);
    }
    
    IntArrayList ordIndexList = new IntArrayList();
    
    for (int i = 0; i < maxDoc; ++ i) {
      inner.setDocument(i);
      int tmpOrd;
      while ((tmpOrd = (int) inner.nextOrd()) != SortedSetDocValues.NO_MORE_ORDS) {
        int existingOrd = HS.unsafe.getInt(ordsPtr + i * 4);
        if (existingOrd == SortedSetDocValuesUtil.UNASSIGNED) {                              
          HS.unsafe.putInt(ordsPtr + (i * 4), tmpOrd);
        } else {
          if (!SortedSetDocValuesUtil.isSetHighestBit(existingOrd)) {
            int pointer = ordIndexList.size();            
            HS.unsafe.putInt(ordsPtr + (i * 4), SortedSetDocValuesUtil.setHighestBit(pointer));
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
    ordsPoolPtr =  HS.allocArray(numOrds, 4, false);
    
    IntListIterator intIter = ordIndexList.iterator();
    int idx = 0;
    while (intIter.hasNext()) {
      HS.unsafe.putInt(ordsPoolPtr + (idx * 4), intIter.nextInt());
      idx++;
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
      int val = HS.unsafe.getInt(ordsPoolPtr + offset * 4);
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
    currentVal = HS.unsafe.getInt(ordsPtr + docID * 4);
    offset = 0;
    hasMoreOrds = true;
  }

  @Override
  public BytesRef lookupOrd(long ord) {
    BytesRef result = new BytesRef();
    int offset = HS.getInt(bytesRefPtr, 2 * (int) ord);
    int length = HS.getInt(bytesRefPtr, 2 * (int) ord + 1);
    
    result.bytes = new byte[length];
    result.offset = 0;
    result.length = length;
    for (int i = 0; i < result.length; ++i) {
      result.bytes[i] = HS.unsafe.getByte(bufferPtr + offset + i);  
    }
    return result;
  }

  @Override
  public long getValueCount() {
    return numTerms;
  }

  @Override
  public void close() throws IOException {
    HS.freeArray(bufferPtr);
    HS.freeArray(ordsPtr);
    HS.freeArray(bytesRefPtr);
    HS.freeArray(ordsPoolPtr);
  }
}
