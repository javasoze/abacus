package org.apache.lucene.util.packed;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;

import org.apache.lucene.search.DocIdSetIterator;

public class
    PackedIntsDocIdSetIterator extends DocIdSetIterator {
  private int cs = -1;
  private int readCursor;
  private long lastVal;

  private final int[] currentSeg;
  private final int currentCount;
  private final PackedDocSegment[] segList;
  private final int size;
  
  private int doc = -1;
  
  PackedIntsDocIdSetIterator(int[] currentSeg, int currentCount,
      LinkedList<PackedDocSegment> segList, int size) {
    this.currentCount = currentCount;
    this.currentSeg = currentSeg;
    this.segList = new ArrayList<PackedDocSegment>(segList).toArray(new PackedDocSegment[segList.size()]);
    this.size = size;
    reset();
  }
  
  void reset() {
    readCursor = 0;
    if (segList.length > 0) {
      cs = 0;
      lastVal = segList[cs].minVal;
    } else {
      cs = -1;
      lastVal = 0;
    }
  }
  
  @Override
  public int docID() {
    return doc;
  }

  @Override
  public int nextDoc() throws IOException {
    if (cs == -1 || cs >= segList.length) {
      if (readCursor < currentCount) {
        doc = currentSeg[readCursor];
        readCursor++;
        return doc;
      } else {
        return NO_MORE_DOCS;
      }
    }
    if (readCursor < segList[cs].valSet.size()) {
      doc = (int) segList[cs].valSet.get(readCursor);
      doc += lastVal;
      lastVal = doc;
      if (readCursor == segList[cs].valSet.size() - 1) {
        readCursor = 0;
        if (cs < segList.length - 1) {
          cs++;
          lastVal = segList[cs].minVal;
        } else {
          cs = -1;
          lastVal = 0;
        }
      } else {
        readCursor++;
      }
      return doc;
    }
    return NO_MORE_DOCS;
  }

  @Override
  public int advance(int target) throws IOException {
    int i;
    boolean sameAsMinVal = false;
    if (cs != -1) {
      int newSegIdx = cs;
      for (i = newSegIdx; i < segList.length; ++i) {
        if (segList[i].minVal > target) {
          break;
        } else if (segList[i].minVal == target){
          sameAsMinVal = true;
          break;
        }
      }
      // first block
      if (i == 0 || sameAsMinVal) {
        newSegIdx = i;           
      } else {  // seek to last block
        newSegIdx = i - 1;
      }
      if (cs != newSegIdx) {
        lastVal = segList[cs].minVal;
        // reset the read cursor
        readCursor = 0;
      }
    }
    
    while (doc < target) {
      doc = nextDoc();
    }
    return doc;
  }

  @Override
  public long cost() {
    return size;
  }

}
