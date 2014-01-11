package org.apache.lucene.util.packed;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;

import org.apache.lucene.search.DocIdSet;
import org.apache.lucene.search.DocIdSetIterator;

public class PackedIntsDocIdSet extends DocIdSet {
  private int maxDelta;
  private final int[] currentSeg;
  private int currentCount;
  private int size = 0;
  private float acceptableOverheadRatio;
  private final LinkedList<PackedDocSegment> segList = new LinkedList<PackedDocSegment>();
  
  public static void serialize(PackedIntsDocIdSet idset,OutputStream output) throws IOException {
    DataOutputStream out = new DataOutputStream(output);
    int blockSize = idset.currentSeg.length;
    out.writeInt(blockSize);
    for (int i=0;i<blockSize;++i){
      out.writeInt(idset.currentSeg[i]);
    }
    out.writeInt(idset.maxDelta);
    out.writeFloat(idset.acceptableOverheadRatio);
    out.writeInt(idset.currentCount);
    out.writeInt(idset.size);
    out.writeInt(idset.segList.size());    
    for (PackedDocSegment seg : idset.segList){
      PackedDocSegment.serialize(seg, out);
    }
  }
  
  public static PackedIntsDocIdSet deserialize(InputStream input) throws IOException {
    DataInputStream in = new DataInputStream(input);
    int blockSize = in.readInt();
    
    PackedIntsDocIdSet idSet = new PackedIntsDocIdSet(blockSize);
    int[] currentSeg = idSet.currentSeg;
    for (int i=0;i<blockSize;++i){
      currentSeg[i] = in.readInt();
    }
    idSet.maxDelta = in.readInt();
    idSet.acceptableOverheadRatio = in.readFloat();
    idSet.currentCount = in.readInt();
    idSet.size = in.readInt();
    int segLen = in.readInt();

    for (int i=0;i<segLen;++i){
      PackedDocSegment seg = PackedDocSegment.deserialize(in, idSet.acceptableOverheadRatio);
      idSet.segList.add(seg);
    }
    
    return idSet;
  }

  public PackedIntsDocIdSet(int blockSize) {
    this(blockSize, PackedInts.DEFAULT);
  }
  
  public PackedIntsDocIdSet(int blockSize, float acceptableOverheadRatio) {
    this.acceptableOverheadRatio = acceptableOverheadRatio;
    currentSeg = new int[blockSize];
    init();
  }

  public long sizeInBytes() {
    long size = currentCount * 8;
    for (PackedDocSegment seg : segList) {
      size += seg.sizeInBytes();
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
    seg.valSet = PackedInts.getMutable(currentSeg.length, nBits, acceptableOverheadRatio);
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
