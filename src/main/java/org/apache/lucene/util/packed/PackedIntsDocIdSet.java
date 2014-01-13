package org.apache.lucene.util.packed;

import java.io.IOException;
import java.util.LinkedList;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;
import org.apache.lucene.store.DataInput;
import org.apache.lucene.store.DataOutput;
import org.apache.lucene.util.packed.PackedInts.Format;

public class PackedIntsDocIdSet extends DocIdSet {
  private int maxDelta;
  private final int[] currentSeg;
  private int currentCount;
  private int size = 0;
  private final LinkedList<PackedDocSegment> segList = new LinkedList<PackedDocSegment>();
  
  public static void serialize(PackedIntsDocIdSet idset,DataOutput out) throws IOException {    
    out.writeVInt(idset.currentSeg.length);
    out.writeVInt(idset.currentCount);
    for (int i=0;i<idset.currentCount;++i){
      out.writeVInt(idset.currentSeg[i]);
    }
    
    out.writeVInt(idset.maxDelta);
    
    out.writeVInt(idset.size);
    out.writeVInt(idset.segList.size());    
    for (PackedDocSegment seg : idset.segList){
      PackedDocSegment.serialize(seg, out);
    }
  }
  
  public static PackedIntsDocIdSet deserialize(DataInput in) throws IOException {
    int blockSize = in.readVInt();
    
    PackedIntsDocIdSet idSet = new PackedIntsDocIdSet(blockSize);
    idSet.currentCount = in.readVInt();
    int[] currentSeg = idSet.currentSeg;
    for (int i=0;i < idSet.currentCount; ++i){
      currentSeg[i] = in.readVInt();
    }
    idSet.maxDelta = in.readVInt();
    idSet.size = in.readVInt();
    
    int segLen = in.readVInt();

    for (int i=0;i<segLen;++i){
      PackedDocSegment seg = PackedDocSegment.deserialize(in);
      idSet.segList.add(seg);
    }
    
    return idSet;
  }

  public PackedIntsDocIdSet(int blockSize) {
    currentSeg = new int[blockSize];
    init();
  }

  public long sizeInBytes() {
    int size = currentCount * 4 + 20;
    for (PackedDocSegment seg : segList) {
      size += seg.ramBytesUsed();
    }
    return size;
  }

  void init() {
    currentCount = 0;
    maxDelta = -1;
  }

  public void addID(int val) {
    if (currentCount == 0) {
      currentSeg[currentCount++] = val;
    } else {
      int delta = val - currentSeg[currentCount];
      if (maxDelta < delta) {
        maxDelta = delta;
      }
      currentSeg[currentCount++] = val;
    }
    if (currentCount == currentSeg.length) {
      compressBlock();
    }
    size++;
  }

  private void compressBlock() {
    int nBits = PackedIntsUtil.getNumBits(maxDelta);
    PackedDocSegment seg = new PackedDocSegment();
    seg.minVal = currentSeg[0];
    seg.valSet = PackedInts.getMutable(currentSeg.length, nBits, Format.PACKED);
    for (int i = 0; i < currentSeg.length; ++i) {
      if (i > 0) {
        long val = currentSeg[i] - currentSeg[i - 1];
        seg.valSet.set(i, val);
      } else {
        seg.valSet.set(i, 0);
      }
    }
    segList.add(seg);
    init();
  }

  @Override
  public DocIdSetIterator iterator() {
    return new PackedIntsDocIdSetIterator(currentSeg, currentCount, segList, size);
  }
}
